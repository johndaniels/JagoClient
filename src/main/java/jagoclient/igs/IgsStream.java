package jagoclient.igs;

import jagoclient.Global;
import jagoclient.sound.JagoSound;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

import rene.util.parser.StringParser;

import javax.swing.*;

/**
 * This class handles the raw by raw input from the server, parsing it for
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
	public interface LineListener {
		void handleLine(LineResult line);
	}
	private static final Logger LOG = Logger.getLogger(IgsStream.class.getName());
	static final int linesize = 4096;
	final List<LineListener> listeners = new ArrayList<>();
	InputStream In;
	OutputStream out;
	boolean isLoggedIn = false;
	Socket socket;
	boolean stopThread;
	Thread readingThread;

	List<Runnable> loginListeners = new ArrayList<>();
	CompletableFuture<Boolean> loginFuture;

	public IgsStream ()
	{
		LOG.info("--> IgsStream opened");
	}

	public void addLineListener(LineListener listener) {
		synchronized (listeners) {
			this.listeners.add(listener);
		}
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
			InputStream telnetStream = new TelnetStream(input, out);
			In = new BufferedInputStream(telnetStream);
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

	public int read () throws IOException
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
			return c;
		}
	}

	public void close () throws IOException
	{
		In.close();
	}

	public InputStream getInputStream ()
	{
		return In;
	}

	public static class LineResult {
		String raw;
		int number;
		String command = "";

		public String getRaw() {
			return raw;
		}

		public void setRaw(String raw) {
			this.raw = raw;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}
	}
	/**
	 * This reads a complete raw from the server.
	 */
	LineResult readlineprim () throws IOException
	{
		StringBuilder builder = new StringBuilder();
		LineResult result = new LineResult();
		StringParser sp;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int b = read();
		while (true)
		{
			if (builder.length() >= linesize || b == '\n')
			{
				if (builder.length() >= linesize) LOG.severe("IGS : Buffer overflow");
				break;
			}
			buffer.write(b);
			builder.append((char)b);
			b = read();
		}
		byte[] line = buffer.toByteArray();
		String weird = new String(line, "utf-8");
		int spacesEnd;
		for (spacesEnd=0; spacesEnd < builder.length(); spacesEnd++) {
			if (builder.charAt(spacesEnd) != ' ') break;
		}
		int i;
		for (i = spacesEnd; i < builder.length(); i++)
		{
			if (builder.charAt(i) < '0' || builder.charAt(i) > '9') break;
		}
		if (i > spacesEnd)
		{
			result.setNumber(Integer.parseInt(builder.substring(spacesEnd, i), 10));
			result.setCommand(builder.substring(i + 1));
		}
		else
		{
			result.setCommand(builder.toString());
		}
		result.setRaw(builder.toString());
		return result;
	}



	private <T> void completeOnSwingThread(CompletableFuture<T> future, T result) {
		SwingUtilities.invokeLater(() -> future.complete(result));
	}

	/**
	 * The most important method of this class.
	 * <P>
	 * This method reads input from the server raw by raw, filtering out and
	 * answering Telnet protocol characters. If it receives a full raw it will
	 * interpret it and return true. Otherwise, it will return false. The raw
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
		LineResult lineResult = readlineprim();
		//LOG.log(Level.INFO, "IGS sent: {0}", lineResult.getRaw());
		if (!isLoggedIn) {
			if (lineResult.getRaw().contains("This is a guest account")) {
				completeOnSwingThread(loginFuture, false);
				SwingUtilities.invokeLater(this::disconnect);
			} else if (lineResult.getRaw().contains("Invalid Password")) {
				completeOnSwingThread(loginFuture, false);
				SwingUtilities.invokeLater(this::disconnect);
			} else if (lineResult.getNumber() == 1 && lineResult.getCommand().equals("1")) {
				completeOnSwingThread(loginFuture, true);
				isLoggedIn = true;
			}
		} else {
			if (lineResult.getNumber() == 9 && lineResult.getCommand().equals("File")) {
				LineResult fileLine = readlineprim();
				while (fileLine.getNumber() != 9 || !fileLine.getCommand().equals("File")) {
					// Skip through files
					fileLine = readlineprim();
				}
				return false;
			}

			SwingUtilities.invokeLater(() -> {
				synchronized (listeners) {
					for (LineListener l : listeners) {
						l.handleLine(lineResult);
					}
				}
			});
		}
		return true;
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
}
