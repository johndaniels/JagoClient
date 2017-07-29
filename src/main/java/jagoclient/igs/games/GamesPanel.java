package jagoclient.igs.games;

import jagoclient.Global;
import jagoclient.dialogs.Help;
import jagoclient.dialogs.Message;
import jagoclient.gui.ButtonAction;
import jagoclient.gui.MenuItemAction;
import jagoclient.gui.MyLabel;
import jagoclient.gui.MyMenu;
import jagoclient.gui.MyPanel;
import jagoclient.gui.Panel3D;
import jagoclient.igs.ConnectionFrame;
import jagoclient.igs.Distributor;
import jagoclient.igs.IgsStream;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jagoclient.igs.connection.ConnectionState;
import rene.gui.CloseFrame;
import rene.gui.CloseListener;
import rene.util.parser.StringParser;
import rene.viewer.Lister;
import rene.viewer.SystemLister;

import javax.swing.*;

/**
 * This frame displays the games on the server. It is opened by a
 * GamesDistributor. To sort the games it uses the GameInfo class, which is a
 * SortObject implementation and can be sorted via the Sorter quicksort
 * algorithm.
 * 
 */

public class GamesPanel extends JPanel implements CloseListener, Distributor.Task
{
	JTable gameLister;

	public GamesPanel (ConnectionState connectionState)
	{
		super();
		setLayout(new BorderLayout());
		connectionState.addGamesChangedHandler(this::gamesChanged);
		gameLister = new JTable();
		gameLister.setFont(Global.Monospaced);
		JScrollPane scrollPane = new JScrollPane(gameLister);
		add("Center", scrollPane);
		PopupMenu pop = new PopupMenu();
	}

	public void gamesChanged(List<GameInfo> gameInfos) {
		gameLister.setModel(new GamesTableModel(gameInfos));
	}

	public GameInfo getSelectedGame() {
		int row = gameLister.getSelectedRow();
		return ((GamesTableModel)gameLister.getModel()).getGame(row);
	}

	public void doAction (String o)
	{
		if (Global.resourceString("Refresh").equals(o))
		{
		}
		else if (Global.resourceString("Peek").equals(o))
		{
		}
		else if (Global.resourceString("Status").equals(o))
		{
		}
		else if (Global.resourceString("Observe").equals(o))
		{
		}
		else if (Global.resourceString("About_this_Window").equals(o))
		{
			try
			{
				new Help("games").display();
			}
			catch (IOException ex)
			{
				new Message(Global.frame(), ex.getMessage()).setVisible(true);
			}
		}
	}

	public synchronized boolean close ()
	{

		Global.notewindow(this, "games");
		return true;
	}


	@Override
	public void finished () {}

	@Override
	public void closed ()
	{
	}
}
