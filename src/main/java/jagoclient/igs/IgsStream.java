package jagoclient.igs;

import jagoclient.Global;
import jagoclient.dialogs.Message;
import jagoclient.sound.JagoSound;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

import rene.util.parser.StringParser;

import javax.swing.*;

/**
 * This class handles the line by line input from the server, parsing it for
 * command numbers and sub-numbers.
 * <P>
 * Furthermore, it will filter the input and distribute it to the distributors.
 * The class initializes a list of distributors. All distributors must chain
 * themselves to this list. They can unchain themselves, if they do no longer
 * wish to receive input.
 * <P>
 * The input is done via a BufferedReader, which does the encoding stuff.
 */

public class IgsStream
{
	private static final Logger LOG = Logger.getLogger(IgsStream.class.getName());
	String Line;
	char C[];
	final int linesize = 4096;
	int L;
	int Number;
	String Command;
	final List<Distributor> DistributorList;
	BufferedReader In;
	OutputStream out;
	boolean isLoggedIn = false;
	Socket socket;
	boolean stopThread;
	Thread readingThread;

	List<Runnable> loginListeners = new ArrayList<>();
	CompletableFuture<Boolean> loginFuture;

	public IgsStream ()
	{
		Line = "";
		L = 0;
		Number = 0;
		LOG.info("--> IgsStream opened");
		DistributorList = new ArrayList<Distributor>();
		C = new char[linesize];
	}

	private void readForever() {
		while (!stopThread) {
			try {
				readline();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void startReading() {
		readingThread = new Thread(this::readForever);
		readingThread.start();
	}

	public CompletionStage<Boolean> login(String username, String password) {
		connect();
		out(username);
		out(password);
		loginFuture = new CompletableFuture<>();
		return loginFuture;
	}

	/**
	 * This initializes a BufferedReader to do the decoding.
	 * Must be called from awt event thread
	 */
	private void connect ()
	{
		try {
			socket = new Socket("igs.joyjoy.net", 7777);
			out = socket.getOutputStream();
			InputStream input = socket.getInputStream();
			String encoding = "utf-8";
			InputStream telnetStream = new TelnetStream(input, out);
			In = new BufferedReader(new InputStreamReader(telnetStream, encoding));
			stopThread = false;
			startReading();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Must be called from awt event thread.
	 */
	private void disconnect() {
		try {
			stopThread = true;
			try {
				readingThread.join();
			} catch(InterruptedException e) {
				throw new RuntimeException(e);
			}
			socket.close();
			socket = null;
			In = null;
			out = null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	int lastcr = 0, lastcommand = 0;

	public char read () throws IOException
	{
		while (true)
		{
			int c = In.read();
			if (c == -1 || c == 255 && lastcommand == 255)
			{
				LOG.log(Level.FINE, "Received character {0}", c);
				throw new IOException();
			}
			lastcommand = c;
			if (c == 10)
			{
				if (lastcr == 13)
				{
					lastcr = 0;
					continue;
				}
				lastcr = 10;
				return '\n';
			}
			else if (c == 13)
			{
				if (lastcr == 10)
				{
					lastcr = 0;
					continue;
				}
				lastcr = 13;
				return '\n';
			}
			else lastcr = 0;
			return (char)c;
		}
	}

	/** test, if there is another character waiting */
	public boolean available () throws IOException
	{
		return In.ready();
	}

	public void close () throws IOException
	{
		In.close();
	}

	public BufferedReader getInputStream ()
	{
		return In;
	}

	/**
	 * This reads a complete line from the server.
	 */
	void readlineprim () throws IOException
	{
		StringParser sp;
		char b = read();
		L = 0;
		while (true)
		{
			if (L >= linesize || b == '\n')
			{
				if (L >= linesize) LOG.severe("IGS : Buffer overflow");
				break;
			}
			C[L++] = b;
			b = read();
		}
		Number = 0;
		Command = "";
		int i;
		for (i = 0; i < L; i++)
		{
			if (C[i] < '0' || C[i] > '9') break;
		}
		if (i > 0)
		{
			Number = Integer.parseInt(new String(C, 0, i), 10);
			Command = new String(C, i + 1, L - (i + 1));
		}
		else
		{
			Number = 0;
			Command = new String(C, 0, L);
		}
		L = 0;
	}

	/**
	 * Processes the input, until there is a line, which is not suited for the
	 * specified distributor.
	 */
	void sendall (Distributor dis) throws IOException
	{
		while (true)
		{
			readlineprim();
			LOG.log(Level.INFO, "IGS:({0} for {1}) {2}", new Object[]{Number, dis.number(), Command});
			if (Number == dis.number())
				dis.send(Command);
			else if (Number == 9) // information got in other content
			{
				Distributor dist = findDistributor(9);
				if (dist != null)
				{
					LOG.info("Sending information");
					dist.send(Command);
					// sendall(dist); // logical error
				}
			}
			else
			{
				if (dis.sizetask() != null || dis.task() != null)
				{
					unchain(dis);
					if (dis.sizetask() != null)
						dis.sizetask().sizefinished();
					if (dis.task() != null)
						dis.task().finished();
					LOG.log(Level.INFO, "Distributor {0} finished", dis.number());
				}
				break;
			}
		}
		LOG.log(Level.INFO, "sendall() for {0} finished", dis.number());
		dis.allsended();
	}

	/**
	 * Same as above, but the distrubutor get the input with the String
	 * prepended.
	 */
	void sendall (String s, Distributor dis) throws IOException
	{
		while (true)
		{
			readlineprim();
			LOG.log(Level.INFO, "IGS: {0}", Command);
			if (Number == 11)
				dis.send(s + Command);
			else
			{
				if (dis.sizetask() != null || dis.task() != null)
				{
					unchain(dis);
					if (dis.sizetask() != null)
						dis.sizetask().sizefinished();
					if (dis.task() != null)
						dis.task().finished();
				}
				break;
			}
		}
		dis.allsended();
	}

	private <T> void completeOnSwingThread(CompletableFuture<T> future, T result) {
		SwingUtilities.invokeLater(() -> future.complete(result));
	}

	/**
	 * The most important method of this class.
	 * <P>
	 * This method reads input from the server line by line, filtering out and
	 * answering Telnet protocol characters. If it receives a full line it will
	 * interpret it and return true. Otherwise, it will return false. The line
	 * can be read from the Line variable. Incomplete lines happen only at the
	 * start of the connection during login.
	 * <P>
	 * Interpreting a lines means determining its command number and an eventual
	 * sub-command number. Both are used to determine the right distributor for
	 * this command, if there is one. Otherwise, the function returns true and
	 * InputThread handles the command.
	 * <P>
	 * The protocol is not very logic, nor is the structure of this method. It
	 * has cases for several distributors. Probably, this code should be a
	 * static method of the distributor.
	 * 
	 * @see jagoclient.igs.InputThread
	 */
	public boolean readline () throws IOException
	{
		boolean full;
		StringParser sp;
		outerloop: while (true)
		{
			full = false;
			char b = 0;
			b = read();
			while (true)
			{
				if (L >= linesize)
				{
					LOG.severe("IGS : Buffer overflow");
					throw new IOException("Buffer Overflow");
				}
				if (b == '\n')
				{
					full = true;
					break;
				}
				C[L++] = b;
				if ( !available()) break;
				b = read();
			}
			Line = new String(C, 0, L);
			LOG.log(Level.INFO, "IGS sent: {0}", Line);
			if (!isLoggedIn) {
				if (Line.contains("This is a guest account")) {
					completeOnSwingThread(loginFuture, false);
					SwingUtilities.invokeLater(this::disconnect);
				} else if (Line.contains("Invalid Password")) {
					completeOnSwingThread(loginFuture, false);
					SwingUtilities.invokeLater(this::disconnect);
				} else if (Line.contains("You have entered IGS")) {
					completeOnSwingThread(loginFuture, true);
					isLoggedIn = true;
				}
				L = 0;
				break;
			}
			Number = 0;
			Command = "";
			if (full)
			{
				int i;
				for (i = 0; i < L; i++)
				{
					if (C[i] < '0' || C[i] > '9') break;
				}
				if (i > 0 && C[i] == ' ')
				{
					try
					{
						Number = Integer.parseInt(new String(C, 0, i), 10);
					}
					catch (Exception e)
					{
						break;
					}
					Command = new String(C, i + 1, L - (i + 1));
				}
				else
				{
					Number = 100;
					Command = new String(C, 0, L);
				}
				L = 0;
				loop1: while (true)
				{
					LOG.log(Level.INFO, "loop1 with {0} {1}", new Object[]{Number, Command});
					if (Number == 21
						&& (Command.startsWith("{Game") || Command
							.startsWith("{ Game")))
					{
						sp = new StringParser(Command);
						sp.skip("{");
						sp.skipblanks();
						sp.skip("Game");
						sp.skipblanks();
						if ( !sp.isint()) continue outerloop;
						int G = sp.parseint(':');
						Distributor dis = findDistributor(15, G);
						if (dis != null)
						{
							LOG.log(Level.INFO, "Sending comment to game {0}", G);
							dis.send(Command);
							L = 0;
							continue outerloop;
						}
						if (Global.getParameter("reducedoutput", true)
							&& !Global.posfilter(Command))
						{
							continue outerloop;
						}
					}
					else if (Number == 21 && Command.startsWith("{")
						&& Global.getParameter("reducedoutput", true)
						&& !Global.posfilter(Command))
					{
						continue outerloop;
					}
					else if (Number == 21 && Global.posfilter(Command))
					{
						JagoSound.play("message", "wip", true);
					}
					else if (Number == 11 && Command.startsWith("Kibitz"))
					{
						sp = new StringParser(Command);
						sp.upto('[');
						if (sp.error()) continue outerloop;
						sp.upto(']');
						if (sp.error()) continue outerloop;
						sp.upto('[');
						if (sp.error()) continue outerloop;
						sp.skip("[");
						sp.skipblanks();
						if ( !sp.isint()) continue outerloop;
						int G = sp.parseint(']');
						Distributor dis = findDistributor(15, G);
						if (dis != null)
						{
							LOG.log(Level.INFO, "Sending kibitz to game {0}", G);
							dis.send(Command);
							sendall("Kibitz-> ", dis);
							continue loop1;
						}
					}
					else if (Number == 9 && Command.startsWith("Removing @"))
					{
						sp = new StringParser(Command);
						sp.skip("Removing @");
						Distributor dis = findDistributor(15);
						if (dis != null)
						{
							LOG.log(Level.INFO, "Got {0}", Command);
							dis.send(Command);
							sendall(dis);
							continue loop1;
						}
						continue outerloop;
					}
					else if (Number == 9 && !Command.startsWith("File"))
					{
						Distributor dis = findDistributor(9);
						if (dis != null)
						{
							LOG.info("Sending information");
							dis.send(Command);
							sendall(dis);
							LOG.info("End of information");
							continue loop1;
						}
					}
					else if (Number == 15 && Command.startsWith("Game"))
					{
						sp = new StringParser(Command);
						sp.skip("Game");
						sp.skipblanks();
						if ( !sp.isint()) continue outerloop;
						int G = sp.parseint();
						Distributor dis = findDistributor(15, G);
						if (dis != null)
						{
							LOG.log(Level.INFO, "Sending to game {0}", G);
							dis.send(Command);
							sendall(dis);
							continue loop1;
						}
						dis = findDistributor(15, -1);
						if (dis != null)
						{
							LOG.log(Level.INFO, "Game {0} started", G);
							dis.game(G);
							dis.send(Command);
							sendall(dis);
							continue loop1;
						}
						continue outerloop;
					}
					else if (Number == 32)
					{
						sp = new StringParser(Command);
						int G = sp.parseint(':');
						sp.skip(":");
						sp.skipblanks();
						if (G == 0)
						{
							Number = 9;
							continue loop1;
						}
						Distributor dis = findDistributor(32, G);
						if (dis != null)
						{
							LOG.log(Level.INFO, "Sending to channel {0}", G);
							dis.send(sp.upto((char)0));
							continue outerloop;
						}
						//ChannelDistributor cd = new ChannelDistributor(CF,
						//	this, Out, G);
						//cd.send(sp.upto((char)0));
						continue outerloop;
					}
					else if (Number == 22)
					{
						/*Distributor dis = findDistributor(22);
						if (dis != null)
						{
							dis.send(Command);
							sendall(dis);
							continue loop1;
						}
						IgsGoFrame gf = new IgsGoFrame(CF, "Peek game");
						gf.setVisible(true);
						gf.repaint();
						Status s = new Status(gf, this, Out);
						s.PD.send(Command);
						sendall(s.PD);*/
					}
					else if (Number == 9 && Command.startsWith("-- "))
					{
						Distributor dis = findDistributor(32);
						if (dis != null)
						{
							LOG.log(Level.INFO, "Sending to channel {0}", dis.game());
							dis.send(Command);
							continue outerloop;
						}
						continue outerloop;
					}
					else if (Number == 1
						&& (Command.startsWith("8") || Command.startsWith("5") || Command
							.startsWith("6")))
					{
						LOG.log(Level.INFO, "1 received {0}", Command);
					}
					else if (Number != 9)
					{
						Distributor dis = findDistributor(Number);
						if (dis != null)
						{
							dis.send(Command);
							sendall(dis);
							continue loop1;
						}
						else LOG.log(Level.WARNING, "Distributor {0} not found", Number);
					}
					break;
				}
				L = 0;
			}
			break;
		}
		return full;
	}

	public Distributor findDistributor (int n, int g)
	{
		synchronized (DistributorList)
		{
			for (Distributor dis : DistributorList)
			{
				if (dis.number() == n)
				{
					if (dis.game() == g) return dis;
				}
			}
			return null;
		}
	}

	public Distributor findDistributor (int n)
	{
		synchronized (DistributorList)
		{
			for (Distributor dis : DistributorList)
			{
				if (dis.number() == n) return dis;
			}
			return null;
		}
	}

	/**
	 * Chains a new distributor to the distributor list.
	 */
	public void append (Distributor dis)
	{
		synchronized (DistributorList)
		{
			DistributorList.add(dis);
		}
	}

	/**
	 * Seeks a game distributor, which waits for that game.
	 */
	public boolean gamewaiting (int g)
	{
		synchronized (DistributorList)
		{
			for (Distributor dis : DistributorList)
			{
				if (dis.number() == 15)
				{
					if (dis.game() == g) return true;
				}
			}
			return false;
		}
	}

	public void unchain (Distributor dis)
	{
		try
		{
			synchronized (DistributorList)
			{
				DistributorList.remove(dis);
			}
		}
		catch (Exception e)
		{}
	}

	/**
	 * Removes all distributors from the distributor list.
	 */
	public void removeall ()
	{
		synchronized (DistributorList)
		{
			DistributorList.clear();
		}
	}

	public void out (String s)
	{
		if (s.startsWith("observe") || s.startsWith("moves")
			|| s.startsWith("status")) return;
		LOG.log(Level.INFO, "Sending: {0}", s);
		try {
			out.write(s.getBytes("UTF-8"));
			out.write(0x0a);
			out.flush();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int number ()
	{
		return Number;
	}

	public String command ()
	{
		return Command;
	}

	public int commandnumber ()
	{
		try
		{
			return Integer.parseInt(Command, 10);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	public String line ()
	{
		return Line;
	}

}
