package jagoclient.igs;

import jagoclient.Global;
import jagoclient.dialogs.GetParameter;
import jagoclient.dialogs.Help;
import jagoclient.dialogs.Message;
import jagoclient.gui.ButtonAction;
import jagoclient.gui.CheckboxMenuItemAction;
import jagoclient.gui.GrayTextField;
import jagoclient.gui.HistoryTextField;
import jagoclient.gui.MenuItemAction;
import jagoclient.gui.MyLabel;
import jagoclient.gui.MyMenu;
import jagoclient.gui.MyPanel;
import jagoclient.gui.Panel3D;
import jagoclient.igs.connection.ConnectionInfo;
import jagoclient.igs.connection.ConnectionState;
import jagoclient.igs.games.GamesPanel;
import jagoclient.igs.users.UsersPanel;
import jagoclient.igs.who.WhoFrame;

import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import rene.dialogs.Question;
import rene.gui.CloseFrame;
import rene.gui.DoItemListener;

class CloseConnectionQuestion extends Question
{
	public CloseConnectionQuestion (ConnectionFrame g)
	{
		super(g, Global.resourceString("This_will_close_your_connection_"),
			Global.resourceString("Close"), true);
		setVisible(true);
	}
}


class GetWaitfor extends GetParameter
{
	ConnectionFrame CF;

	public GetWaitfor (ConnectionFrame cf)
	{
		super(cf, Global.resourceString("Wait_for_"), Global
			.resourceString("Wait_for_Player"), cf, true);
		CF = cf;
		set(CF.Waitfor);
		setVisible(true);
	}

	@Override
	public boolean tell (Frame f, String s)
	{
		CF.Waitfor = s;
		return true;
	}
}


class GetReply extends GetParameter
{
	ConnectionFrame CF;

	public GetReply (ConnectionFrame cf)
	{
		super(cf, Global.resourceString("Automatic_Reply_"), Global
			.resourceString("Auto_Reply"), cf, true);
		CF = cf;
		set(CF.Reply);
		setVisible(true);
	}

	@Override
	public boolean tell (Frame f, String s)
	{
		CF.Reply = s;
		Global.setParameter("autoreply", s);
		return true;
	}
}


class RefreshWriter extends PrintWriter
{
	private static final Logger LOG = Logger.getLogger(RefreshWriter.class.getName());
	Thread T;
	boolean NeedsRefresh;
	boolean Stop = false;

	public RefreshWriter (OutputStreamWriter out, boolean flag)
	{
		super(out, flag);
		if (Global.getParameter("refresh", true))
		{
			LOG.info("Refresh Thread started.");
			T = new Thread()
			{
				@Override
				public void run ()
				{
					runAYT();
				}
			};
			T.start();
		}
	}

	@Override
	public void print (String s)
	{
		super.print(s);
		LOG.log(Level.INFO, "Out ---> {0}", s);
		NeedsRefresh = false;
	}

	public void printLn (String s)
	{
		super.println(s);
		LOG.log(Level.INFO, "Out ---> {0}", s);
		NeedsRefresh = false;
	}

	@Override
	public void close ()
	{
		super.close();
		NeedsRefresh = false;
		Stop = true;
	}

	public void runAYT ()
	{
		while ( !Stop)
		{
			NeedsRefresh = true;
			try
			{
				Thread.sleep(300000);
			}
			catch (Exception e)
			{}
			if (Stop) break;
			if (NeedsRefresh)
			{
				println("ayt");
				// write(254);
				LOG.info("ayt sent!");
			}
		}
	}
}


/**
 * This frame contains a menu, a text area for the server output, a text area to
 * send commands to the server and buttons to call who, games etc.
 */

public class ConnectionFrame extends CloseFrame implements DoItemListener, KeyListener
{
	private static final Logger LOG = Logger.getLogger(ConnectionFrame.class.getName());
	GridBagLayout girdbag;
	JPanel usersPanel;
	GamesPanel gamesPanel;
	HistoryTextField Input;
	GridBagLayout gridbag;
	public WhoFrame Who;

	Socket Server;
	PrintWriter Out;
	public DataOutputStream Outstream;
	String Encoding;
	IgsStream In;
	String Dir;
	JTextField Game;
	CheckboxMenuItem CheckInfo, CheckMessages, CheckErrors, ReducedOutput,
		AutoReply;
	JTabbedPane tabbedPane;
	public int MoveStyle = ConnectionInfo.MOVE;
	JTextField WhoRange; // Kyu/Dan range for the who command.
	String Waitfor; // Pop a a message, when this player connects.
	List<OutputListener> OL; // List of usersPanel-Listeners
	String Reply;
	ConnectionState connectionState;

	public boolean hasClosed = false; // note that the user closed the window

	public ConnectionFrame (String Name, ConnectionState connectionState)
	{
		super(Name);
		this.connectionState = connectionState;
		Waitfor = "";
		OL = new ArrayList<OutputListener>();
		// Menu
		MenuBar M = new MenuBar();
		setMenuBar(M);
		Menu file = new MyMenu(Global.resourceString("File"));
		M.add(file);
		file.add(new MenuItemAction(this, Global.resourceString("Save")));
		file.addSeparator();
		file.add(new MenuItemAction(this, Global.resourceString("Clear")));
		file.addSeparator();
		file.add(new MenuItemAction(this, Global.resourceString("Close")));
		Menu options = new MyMenu(Global.resourceString("Options"));
		M.add(options);
		options.add(AutoReply = new CheckboxMenuItemAction(this, Global
			.resourceString("Auto_Reply")));
		AutoReply.setState(false);
		options
			.add(new MenuItemAction(this, Global.resourceString("Set_Reply")));
		Reply = Global.getParameter("autoreply",
			"I am busy! Please, try later.");
		options.addSeparator();
		options.add(CheckInfo = new CheckboxMenuItemAction(this, Global
			.resourceString("Show_Information")));
		CheckInfo.setState(Global.getParameter("showinformation", false));
		options.add(CheckMessages = new CheckboxMenuItemAction(this, Global
			.resourceString("Show_Messages")));
		CheckMessages.setState(Global.getParameter("showmessages", true));
		options.add(CheckErrors = new CheckboxMenuItemAction(this, Global
			.resourceString("Show_Errors")));
		CheckErrors.setState(Global.getParameter("showerrors", false));
		options.add(ReducedOutput = new CheckboxMenuItemAction(this, Global
			.resourceString("Reduced_Output")));
		ReducedOutput.setState(Global.getParameter("reducedoutput", true));
		options.add(new MenuItemAction(this, Global
			.resourceString("Wait_for_Player")));
		Menu help = new MyMenu(Global.resourceString("Help"));
		help.add(new MenuItemAction(this, Global
			.resourceString("Terminal_Window")));
		help
			.add(new MenuItemAction(this, Global.resourceString("Server_Help")));
		help.add(new MenuItemAction(this, Global.resourceString("Channels")));
		help.add(new MenuItemAction(this, Global
			.resourceString("Observing_Playing")));
		help.add(new MenuItemAction(this, Global.resourceString("Teaching")));
		M.setHelpMenu(help);
		JPanel roomsPanel = new JPanel();
		roomsPanel.setLayout(new BorderLayout());


		JPanel center = new JPanel();
		center.setLayout(new BorderLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		// Text
		usersPanel = new UsersPanel(connectionState);
		usersPanel.setFont(Global.Monospaced);
		splitPane.setTopComponent(usersPanel);
		gamesPanel = new GamesPanel(connectionState);
		splitPane.setBottomComponent(gamesPanel);
		center.add(splitPane, "Center");

		// Input
		Input = new HistoryTextField(this, "Input");
		Input.loadHistory("input.history");
		tabbedPane = new JTabbedPane();
		roomsPanel.add("Center", center);
		// Buttons:
		MyPanel p = new MyPanel();
		p.add(new ButtonAction(this, Global.resourceString("Who")));
		p.add(WhoRange = new HistoryTextField(this, "WhoRange", 5));
		WhoRange.setText(Global.getParameter("whorange", "20k-8d"));
		JButton observeButton = new JButton("Observe");
		observeButton.addActionListener(actionEvent -> this.observe(gamesPanel.getSelectedGame().getGameNumber()));
		p.add(new MyLabel(" "));
		p.add(new ButtonAction(this, Global.resourceString("Peek")));
		p.add(new ButtonAction(this, Global.resourceString("Status")));
		p.add(observeButton);
		Game = new GrayTextField(4);
		p.add(Game);
		roomsPanel.add("South", new Panel3D(p));
		tabbedPane.addTab("Rooms", roomsPanel);
		add(tabbedPane);
		//
		Dir = new String("");
		seticon("iconn.gif");
		addKeyListener(this);
		Input.addKeyListener(this);
	}

	void movestyle (int style)
	{
		MoveStyle = style;
	}


	@Override
	public void doAction (String o)
	{
		try
		{
			if (Global.resourceString("Close").equals(o))
			{
				if (close()) doclose();
			}
			else if (Global.resourceString("Clear").equals(o))
			{
				//usersPanel.setText("");
			}
			else if (Global.resourceString("Save").equals(o))
			{
				FileDialog fd = new FileDialog(this, Global
					.resourceString("Save_Game"), FileDialog.SAVE);
				if ( !Dir.equals("")) fd.setDirectory(Dir);
				fd.setFile("*.txt");
				fd.setVisible(true);
				String fn = fd.getFile();
				if (fn == null) return;
				Dir = fd.getDirectory();
				try
				{
					PrintWriter fo;
					if (Encoding.equals(""))
						fo = new PrintWriter(new OutputStreamWriter(
							new FileOutputStream(fd.getDirectory() + fn)), true);
					else fo = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(fd.getDirectory() + fn), Encoding),
						true);
					//usersPanel.save(fo);
					fo.close();
				}
				catch (IOException ex)
				{
					System.err.println(Global.resourceString("Error_on__") + fn);
				}
			}
			else if (Global.resourceString("Who").equals(o))
			{
				if (Global.getParameter("whowindow", true))
				{
					if (Who != null)
					{
						Who.refresh();
						Who.requestFocus();
						return;
					}
					Who = new WhoFrame(this, Out, In, WhoRange.getText());
					Global.setwindow(Who, "who", 300, 400);
					Who.setVisible(true);
					Who.refresh();
				}
				else
				{
					if (WhoRange.getText().equals(""))
						command("who");
					else command("who " + WhoRange.getText());
				}
			}
			else if (Global.resourceString("Games").equals(o))
			{
				if (Global.getParameter("gameswindow", true))
				{

				}
				else
				{
					command("games");
				}
			}
			else if (Global.resourceString("Peek").equals(o))
			{
				int n;
				try
				{
					n = Integer.parseInt(Game.getText());
					peek(n);
				}
				catch (NumberFormatException ex)
				{
					return;
				}
			}
			else if (Global.resourceString("Status").equals(o))
			{
				int n;
				try
				{
					n = Integer.parseInt(Game.getText());
					status(n);
				}
				catch (NumberFormatException ex)
				{
					return;
				}
			}
			else if (Global.resourceString("Observe").equals(o))
			{
				int n;
				try
				{
					n = Integer.parseInt(Game.getText());
					observe(n);
				}
				catch (NumberFormatException ex)
				{
					return;
				}
			}
			else if (Global.resourceString("Terminal_Window").equals(o))
			{
				new Help("terminal").display();
			}
			else if (Global.resourceString("Server_Help").equals(o))
			{
				new Help("server").display();
			}
			else if (Global.resourceString("Channels").equals(o))
			{
				new Help("channels").display();
			}
			else if (Global.resourceString("Observing_Playing").equals(o))
			{
				new Help("obsplay").display();
			}
			else if (Global.resourceString("Teaching").equals(o))
			{
				new Help("teaching").display();
			}
			else if ("Input".equals(o))
			{
				String os = Input.getText();
				command(os);
			}
			else if (Global.resourceString("Wait_for_Player").equals(o))
			{
				new GetWaitfor(this);
			}
			else if (Global.resourceString("Set_Reply").equals(o))
			{
				new GetReply(this);
			}
			else super.doAction(o);
		}
		catch (IOException ex)
		{
			new Message(Global.frame(), ex.getMessage()).setVisible(true);
		}
	}

	@Override
	public void itemAction (String o, boolean flag)
	{
		if (Global.resourceString("Show_Information").equals(o))
		{
			Global.setParameter("showinformation", flag);
		}
		else if (Global.resourceString("Show_Messages").equals(o))
		{
			Global.setParameter("showmessages", flag);
		}
		else if (Global.resourceString("Show_Errors").equals(o))
		{
			Global.setParameter("showerrors", flag);
		}
		else if (Global.resourceString("Reduced_Output").equals(o))
		{
			Global.setParameter("reducedoutput", flag);
		}
	}

	public void command (String os)
	{
		if (os.startsWith(" "))
		{
			os = os.trim();
		}
		else append(os, Color.green.darker());
		if (Global.getParameter("gameswindow", true)
			&& os.toLowerCase().startsWith("games"))
		{
			Input.setText("");
			doAction(Global.resourceString("Games"));
		}
		else if (Global.getParameter("whowindow", true)
			&& os.toLowerCase().startsWith("who"))
		{
			Input.setText("");
			if (os.length() > 4)
			{
				os = os.substring(4).trim();
				if ( !os.equals("")) WhoRange.setText(os);
			}
			doAction(Global.resourceString("Who"));
		}
		else if (os.toLowerCase().startsWith("observe"))
		{
			Input.setText("");
			if (os.length() > 7)
			{
				os = os.substring(7).trim();
				if ( !os.equals("")) Game.setText(os);
				doAction(Global.resourceString("Observe"));
			}
			else append("Observe needs a game number", Color.red);
		}
		else if (os.toLowerCase().startsWith("peek"))
		{
			Input.setText("");
			if (os.length() > 5)
			{
				os = os.substring(5).trim();
				if ( !os.equals("")) Game.setText(os);
				doAction(Global.resourceString("Peek"));
			}
			else append("Peek needs a game number", Color.red);
		}
		else if (os.toLowerCase().startsWith("status"))
		{
			Input.setText("");
			if (os.length() > 6)
			{
				os = os.substring(6).trim();
				if ( !os.equals("")) Game.setText(os);
				doAction(Global.resourceString("Status"));
			}
			else append("Status needs a game number", Color.red);
		}
		else if (os.toLowerCase().startsWith("moves"))
		{
			new Message(this, Global
				.resourceString("Do_not_enter_this_command_here_")).setVisible(true);
		}
		else
		{
			if ( !Input.getText().startsWith(" ")) Input.remember(os);
			Out.println(os);
			Input.setText("");
		}
	}

	public void peek (int n)
	{
		/*if (In.gamewaiting(n))
		{
			new Message(this, Global
				.resourceString("There_is_already_a_board_for_this_game_")).setVisible(true);
			return;
		}*/
		IgsGoFrame gf = new IgsGoFrame(this, Global.resourceString("Peek_game"));
		gf.setVisible(true);
		gf.repaint();
		new Peeker(gf, In, Out, n);
	}

	public void status (int n)
	{
		IgsGoFrame gf = new IgsGoFrame(this, Global.resourceString("Peek_game"));
		gf.setVisible(true);
		gf.repaint();
		new Status(gf, In, Out, n);
	}

	private void observeCurrent() {

	}

	public void observe (int n)
	{
		/*if (In.gamewaiting(n))
		{
			new Message(this, Global
				.resourceString("There_is_already_a_board_for_this_game_")).setVisible(true);
			return;
		}*/
		connectionState.observeGame(n);
		tabbedPane.addTab("Game " + Integer.toString(n), new IgsGamePanel(n, connectionState));

		IgsGoFrame gf = new IgsGoFrame(this, Global
			.resourceString("Observe_game"));
		gf.setVisible(true);
		gf.repaint();
		new GoObserver(gf, In, Out, n);
	}

	@Override
	public void doclose ()
	{
		Global.notewindow(this, "connection");
		hasClosed = true;
		Input.saveHistory("input.history");
		//if (In != null) In.removeall();
		Out.println("quit");
		Out.close();
		LOG.info("doclose() called in connection");
		new Thread(() -> {
			try
			{	if (Server!=null) Server.close();
				if (In.getInputStream()!=null) In.getInputStream().close();
			}
			catch (IOException ex)
			{	LOG.log(Level.WARNING, null, ex);
			}
		}).start();
		inform();
		super.doclose();
	}

	public void append (String s)
	{
		append(s, Color.blue.darker());
	}

	public void append (String s, Color c)
	{
		//usersPanel.append(s + "\n", c);
		for (OutputListener ol : OL)
		{
			ol.append(s, c);
		}
	}

	public void addOutputListener (OutputListener l)
	{
		OL.add(l);
	}

	public void removeOutputListener (OutputListener l)
	{
		OL.remove(l);
	}

	public boolean wantsinformation ()
	{
		return CheckInfo.getState() && Global.Silent <= 0;
	}

	public boolean wantsmessages ()
	{
		return CheckMessages.getState() && Global.Silent <= 0;
	}

	public boolean wantserrors ()
	{
		return CheckErrors.getState() && Global.Silent <= 0;
	}

	public void out (String s)
	{
		if (s.startsWith("observe") || s.startsWith("status")
			|| s.startsWith("moves")) return;
		LOG.info("---> " + s);
		Out.println(s);
	}

	String reply ()
	{
		if (AutoReply.getState())
			return Reply;
		else return "";
	}

	public void keyPressed (KeyEvent e)
	{}

	public void keyTyped (KeyEvent e)
	{}

	public void keyReleased (KeyEvent e)
	{
		String s = Global.getFunctionKey(e.getKeyCode());
		if (s.equals("")) return;
		Input.setText(s);
	}

	@Override
	public void windowOpened (WindowEvent e)
	{
		Input.requestFocus();
	}

}
