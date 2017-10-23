package jagoclient.board;

import jagoclient.BMPFile;
import jagoclient.Global;
import jagoclient.dialogs.ColorEdit;
import jagoclient.dialogs.GetFontSize;
import jagoclient.dialogs.GetParameter;
import jagoclient.dialogs.Help;
import jagoclient.dialogs.Message;
import jagoclient.gui.ButtonAction;
import jagoclient.gui.CheckboxAction;
import jagoclient.gui.CheckboxMenuItemAction;
import jagoclient.gui.FormTextField;
import jagoclient.gui.GrayTextField;
import jagoclient.gui.MenuItemAction;
import jagoclient.gui.MyLabel;
import jagoclient.gui.MyMenu;
import jagoclient.gui.MyPanel;
import jagoclient.gui.OutputLabel;
import jagoclient.gui.Panel3D;
import jagoclient.gui.SimplePanel;
import jagoclient.gui.TextFieldAction;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import javax.swing.JPanel;
import javax.swing.JTextField;

import rene.dialogs.Question;
import rene.gui.CloseDialog;
import rene.gui.CloseFrame;
import rene.gui.DoItemListener;
import rene.gui.IconBar;
import rene.gui.IconBarListener;
import rene.util.FileName;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlReaderException;

/**
 * Display a dialog to edit game information.
 */

class EditInformation extends CloseDialog
{
	Node N;
	JTextField Black, White, BlackRank, WhiteRank, Date, Time, Komi, Result,
		Handicap, GameName;
	GoFrame F;

	public EditInformation (GoFrame f, Node n)
	{
		super(f, Global.resourceString("Game_Information"), false);
		N = n;
		F = f;
		JPanel p = new MyPanel(new GridLayout(0, 2));
		p.add(new MyLabel(Global.resourceString("Game_Name")));
		p.add(GameName = new FormTextField(n.getaction(Action.Type.GAME_NAME)));
		p.add(new MyLabel(Global.resourceString("Date")));
		p.add(Date = new FormTextField(n.getaction(Action.Type.DATE)));
		p.add(new MyLabel(Global.resourceString("Black")));
		p.add(Black = new FormTextField(n.getaction(Action.Type.BLACK_PLAYER_NAME)));
		p.add(new MyLabel(Global.resourceString("Black_Rank")));
		p.add(BlackRank = new FormTextField(n.getaction(Action.Type.BLACK_PLAYER_RANK)));
		p.add(new MyLabel(Global.resourceString("White")));
		p.add(White = new FormTextField(n.getaction(Action.Type.WHITE_PLAYER_NAME)));
		p.add(new MyLabel(Global.resourceString("White_Rank")));
		p.add(WhiteRank = new FormTextField(n.getaction(Action.Type.WHITE_PLAYER_RANK)));
		p.add(new MyLabel(Global.resourceString("Result")));
		p.add(Result = new FormTextField(n.getaction(Action.Type.RESULT)));
		p.add(new MyLabel(Global.resourceString("Time")));
		p.add(Time = new FormTextField(n.getaction(Action.Type.TIME)));
		p.add(new MyLabel(Global.resourceString("Komi")));
		p.add(Komi = new FormTextField(n.getaction(Action.Type.KOMI)));
		p.add(new MyLabel(Global.resourceString("Handicap")));
		p.add(Handicap = new FormTextField(n.getaction(Action.Type.HANDICAP)));
		add("Center", p);
		JPanel pb = new MyPanel();
		pb.add(new ButtonAction(this, Global.resourceString("OK")));
		pb.add(new ButtonAction(this, Global.resourceString("Cancel")));
		add("South", pb);
		Global.setpacked(this, "editinformation", 350, 450);
	}

	@Override
	public void doAction (String o)
	{
		Global.notewindow(this, "editinformation");
		if (Global.resourceString("OK").equals(o))
		{
			N.setaction(Action.Type.GAME_NAME, GameName.getText());
			N.setaction(Action.Type.BLACK_PLAYER_NAME, Black.getText());
			N.setaction(Action.Type.WHITE_PLAYER_NAME, White.getText());
			N.setaction(Action.Type.BLACK_PLAYER_RANK, BlackRank.getText());
			N.setaction(Action.Type.WHITE_PLAYER_RANK, WhiteRank.getText());
			N.setaction(Action.Type.DATE, Date.getText());
			N.setaction(Action.Type.TIME, Time.getText());
			N.setaction(Action.Type.KOMI, Komi.getText());
			N.setaction(Action.Type.RESULT, Result.getText());
			N.setaction(Action.Type.HANDICAP, Handicap.getText());
			if ( !GameName.getText().equals(""))
				F.setTitle(GameName.getText());
		}
		setVisible(false);
		dispose();
	}
}


/**
 * A dialog to get the present encoding.
 */

class GetEncoding extends GetParameter
{
	GoFrame gcf;

	public GetEncoding (GoFrame gcf)
	{
		super(gcf, Global.resourceString("Encoding__empty__default_"), Global
			.resourceString("Encoding"), gcf, true, "encoding");
		if ( !Global.isApplet())
			set(Global.getParameter("encoding", System
				.getProperty("file.encoding")));
		this.gcf = gcf;
	}

	@Override
	public boolean tell (Frame f, String s)
	{
		if (s.equals(""))
			Global.removeParameter("encoding");
		else Global.setParameter("encoding", s);
		return true;
	}
}


class GetSearchString extends CloseDialog
{
	GoFrame gf;
	TextFieldAction tfa;
	static boolean Active = false;

	public GetSearchString (GoFrame gf)
	{
		super(gf, Global.resourceString("Search"), false);
		if (Active) return;
		add("North", new MyLabel(Global.resourceString("Search_String")));
		add("Center", tfa = new TextFieldAction(this, "Input", 25));
		JPanel p = new MyPanel();
		p.add(new ButtonAction(this, Global.resourceString("Search")));
		p.add(new ButtonAction(this, Global.resourceString("Cancel")));
		add("South", p);
		Global.setpacked(this, "getparameter", 300, 150);
		validate();
		tfa.addKeyListener(this);
		tfa.setText(Global.getParameter("searchstring", "++"));
		this.gf = gf;
		Active = true;
	}

	@Override
	public void doAction (String s)
	{
		if (s.equals(Global.resourceString("Search")) || s.equals("Input"))
		{
			Global.setParameter("searchstring", tfa.getText());
			gf.search();
		}
		else if (s.equals(Global.resourceString("Cancel")))
		{
			setVisible(false);
			dispose();
			Active = false;
		}
	}

	@Override
	public void dispose ()
	{
		Active = false;
		super.dispose();
	}
}


/**
 * Ask the user for permission to close the board frame.
 */

class CloseQuestion extends Question
{
	GoFrame gf;
	boolean Result = false;

	public CloseQuestion (GoFrame g)
	{
		super(g, Global.resourceString("Really_trash_this_board_"), Global
			.resourceString("Close_Board"), true);
		gf = g;
	}

	@Override
	public void tell (Question q, boolean f)
	{
		q.setVisible(false);
		q.dispose();
		Result = f;
	}
}


class SizeQuestion extends GetParameter<GoFrame>
// Ask the board size.
{
	public SizeQuestion (GoFrame g)
	{
		super(g, Global.resourceString("Size_between_5_and_29"), Global
			.resourceString("Board_size"), g, true);
	}

	@Override
	public boolean tell (GoFrame f, String s)
	{
		int n;
		try
		{
			n = Integer.parseInt(s);
			if (n < 5 || n > 59) return false;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		f.doboardsize(n);
		return true;
	}
}


/**
 * Ask the user for permission to close the board frame.
 */

class TextMarkQuestion extends CloseDialog implements DoItemListener
{
	GoFrame G;
	JTextField T;
	Checkbox C;

	public TextMarkQuestion (GoFrame g, String t)
	{
		super(g, Global.resourceString("Text_Mark"), false);
		G = g;
		setLayout(new BorderLayout());
		add("Center", new SimplePanel(new MyLabel(Global.resourceString("String")), 1, T = new TextFieldAction(this, t), 2));
		T.setText(t);
		JPanel ps = new MyPanel();
		ps.add(C = new CheckboxAction(this, Global.resourceString("Auto_Advance")));
		C.setState(Global.getParameter("autoadvance", true));
		ps.add(new ButtonAction(this, Global.resourceString("Set")));
		ps.add(new ButtonAction(this, Global.resourceString("Close")));
		add("South", ps);
		Global.setpacked(this, "gettextmarkquestion", 300, 150);
	}

	@Override
	public void doAction (String o)
	{
		Global.notewindow(this, "gettextmarkquestion");
		Global.setParameter("autoadvance", C.getState());
		if (o.equals(Global.resourceString("Set")))
		{
			G.setTextmark(T.getText());
			Global.setParameter("textmark", T.getText());
		}
		else if (o.equals(Global.resourceString("Close")))
		{
			close();
			setVisible(false);
			dispose();
		}
	}

	@Override
	public void itemAction (String o, boolean b)
	{
		// ??? (Auto_Advance)
	}

	@Override
	public boolean close ()
	{
		G.tmq = null;
		return true;
	}

	public void advance ()
	{
		if ( !C.getState()) return;
		String s = T.getText();
		if (s.length() == 1)
		{
			char c = s.charAt(0);
			c++;
			T.setText("" + c);
			G.setTextmark(T.getText());
		}
		else
		{
			try
			{
				int n = Integer.parseInt(s);
				n = n + 1;
				T.setText("" + n);
				G.setTextmark(T.getText());
			}
			catch (Exception e)
			{}
		}
	}
}


/**
 * // Get/Set the name of the current node
 */

class NodeNameEdit extends GetParameter<GoFrame>
{
	public NodeNameEdit (GoFrame g, String s)
	{
		super(g, Global.resourceString("Name"), Global
			.resourceString("Node_Name"), g, true);
		set(s);
	}

	@Override
	public boolean tell (GoFrame f, String s)
	{
		f.setname(s);
		return true;
	}
}

/**
 * Let the user edit the board fong Redraw the board, when done with OK.
 */

class BoardGetFontSize extends GetFontSize
{
	GoFrame GF;

	public BoardGetFontSize (GoFrame F, String fontname, String deffontname,
		String fontsize, int deffontsize, boolean flag)
	{
		super(fontname, deffontname, fontsize, deffontsize, flag);
		GF = F;
	}

	@Override
	public void doAction (String o)
	{
		super.doAction(o);
		if (Global.resourceString("OK").equals(o))
		{
			Global.createfonts();
			GF.updateall();
		}
	}
}


/**
 * Display a dialog to edit game copyright and user.
 */

class EditCopyright extends CloseDialog
{
	TextArea Copyright;
	JTextField User;
	Node N;

	public EditCopyright (GoFrame f, Node n)
	{
		super(f, Global.resourceString("Copyright_of_Game"), false);
		JPanel p1 = new MyPanel();
		N = n;
		p1.setLayout(new GridLayout(0, 2));
		p1.add(new MyLabel(Global.resourceString("User")));
		p1.add(User = new GrayTextField(n.getaction(Action.Type.USER)));
		add("North", p1);
		JPanel p2 = new MyPanel();
		p2.setLayout(new BorderLayout());
		p2.add("North", new MyLabel(Global.resourceString("Copyright")));
		p2.add("Center", Copyright = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY));
		add("Center", p2);
		JPanel pb = new MyPanel();
		pb.add(new ButtonAction(this, Global.resourceString("OK")));
		pb.add(new ButtonAction(this, Global.resourceString("Cancel")));
		add("South", pb);
		Global.setwindow(this, "editcopyright", 350, 400);
		Copyright.setText(n.getaction(Action.Type.COPYRIGHT));
	}

	@Override
	public void doAction (String o)
	{
		Global.notewindow(this, "editcopyright");
		if (Global.resourceString("OK").equals(o))
		{
			N.setaction(Action.Type.USER, User.getText());
			N.setaction(Action.Type.COPYRIGHT, Copyright.getText());
		}
		setVisible(false);
		dispose();
	}
}


/**
 * Ask, if a complete subtree is to be deleted.
 */

class AskUndoQuestion extends Question
{
	public boolean Result = false;

	public AskUndoQuestion (Frame f)
	{
		super(f, Global.resourceString("Delete_all_subsequent_moves_"), Global
			.resourceString("Delete_Tree"), true);
	}

	@Override
	public void tell (Question q, boolean f)
	{
		q.setVisible(false);
		q.dispose();
		Result = f;
	}
}


/**
 * Ask, if a node is to be inserted and the tree thus changed.
 */

class AskInsertQuestion extends Question
{
	public boolean Result = false;

	public AskInsertQuestion (Frame f)
	{
		super(f, Global.resourceString("Change_Game_Tree_"), Global
			.resourceString("Change_Game_Tree"), true);
	}

	@Override
	public void tell (Question q, boolean f)
	{
		q.setVisible(false);
		q.dispose();
		Result = f;
	}
}


/**
 * The GoFrame class is a frame, which contains the board, the comment window
 * and the navigation buttons (at least).
 * <boardPosition>
 * This class implements BoardInterface. This is done to make clear what
 * routines are called from the board and to give the board a beans appearance.
 * <boardPosition>
 * The layout is a panel of class BoardCommentPanel, containing two panels for
 * the board (BoardPanel) and for the comments (plus the ExtraSendField in
 * ConnectedGoFrame). Below is a 3D panel for the buttons. The BoardCommentPanel
 * takes care of the layout for its components.
 * <boardPosition>
 * This class handles all actions in it, besides the mouse actions on the board,
 * which are handled by Board.
 * <boardPosition>
 * Note that the Board class modifies the appearance of buttons and takes care
 * of the comment window, the next move label and the board position label.
 * <boardPosition>
 * Several private classes in GoFrame.java contain dialogs to enter game
 * information, copyright, text marks, etc.
 * 
 * @see jagoclient.board.Board
 */

// The parent class for a frame containing the board, navigation buttons
// and menus.
// This board has a constructor, which initiates menus to be used as a local
// board. For Partner of IGS games there is the ConnectedGoFrame child, which
// uses another menu structure.
// Furthermore, it has methods to handle lots of user actions.
public class GoFrame extends CloseFrame implements DoItemListener, KeyListener,
	BoardInterface, ClipboardOwner, IconBarListener, UIState.StateChangedHandler
{
	public OutputLabel L, Lm; // For board informations
	TextArea Comment; // For comments
	String Dir; // FileDialog directory
	public Board B; // The board itself
	protected BoardState boardState;
	// menu check items:
	CheckboxMenuItem SetBlack, SetWhite, Black, White, Mark, Letter, Hide,
		Square, Cross, Circle, Triangle, TextMark;
	CheckboxMenuItem Coordinates, UpperLeftCoordinates, LowerRightCoordinates;
	CheckboxMenuItem CommentSGF, DoSound, BeepOnly,
		MenuLastNumber, MenuTarget;
	public boolean BWColor = false, LastNumber = false, ShowTarget = false;
	CheckboxMenuItem MenuBWColor, ShowButtons;
	CheckboxMenuItem VHide, VCurrent, VChild, VNumbers;
	String Text = Global.getParameter("textmark", "A");
	boolean Show;
	TextMarkQuestion tmq;
	IconBar IB;
	JPanel ButtonP;
	String DefaultTitle = "";
	NavigationPanel Navigation;
	UIState uiState;

	public GoFrame (String s)
	// For children, who set up their own menus
	{
		super(s);
		DefaultTitle = s;
		seticon("iboard.gif");
	}

	public GoFrame (Frame f, String s)
	// Constructur for local board menus.
	{
		super(s);
		DefaultTitle = s;
		// Colors
		seticon("iboard.gif");
		setLayout(new BorderLayout());
		// Menu
		MenuBar M = new MenuBar();
		setMenuBar(M);
		uiState = new UIState(19);
		uiState.addStateChangedHandler(this);
		B = new Board(uiState);
		boardState =  uiState.getBoardState();

		Menu file = new GameFileMenu(B, boardState, this, true);
		M.add(file);
		Menu set = new MyMenu(Global.resourceString("Set"));
		M.add(set);
		set.add(Mark = new CheckboxMenuItemAction(this, Global.resourceString("Mark")));
		set.add(Letter = new CheckboxMenuItemAction(this, Global.resourceString("Letter")));
		set.add(Hide = new CheckboxMenuItemAction(this, Global.resourceString("Delete")));
		Menu mark = new MyMenu(Global.resourceString("Special_Mark"));
		mark.add(Square = new CheckboxMenuItemAction(this, Global.resourceString("Square")));
		mark.add(Circle = new CheckboxMenuItemAction(this, Global.resourceString("Circle")));
		mark.add(Triangle = new CheckboxMenuItemAction(this, Global.resourceString("Triangle")));
		mark.add(Cross = new CheckboxMenuItemAction(this, Global.resourceString("Cross")));
		mark.addSeparator();
		mark.add(TextMark = new CheckboxMenuItemAction(this, Global.resourceString("Text")));
		set.add(mark);
		set.addSeparator();
		set.add(new MenuItemAction(this, Global.resourceString("Resume_playing")));
		set.addSeparator();
		set.add(new MenuItemAction(this, Global.resourceString("Pass")));
		set.addSeparator();
		set.add(SetBlack = new CheckboxMenuItemAction(this, Global.resourceString("Set_Black")));
		set.add(SetWhite = new CheckboxMenuItemAction(this, Global.resourceString("Set_White")));
		set.addSeparator();
		set.add(Black = new CheckboxMenuItemAction(this, Global.resourceString("Black_to_play")));
		set.add(White = new CheckboxMenuItemAction(this, Global.resourceString("White_to_play")));
		set.addSeparator();
		set.add(new MenuItemAction(this, Global.resourceString("Undo_Adding_Removing")));
		set.add(new MenuItemAction(this, Global.resourceString("Clear_all_marks")));
		Menu var = new MyMenu(Global.resourceString("Nodes"));
		var.add(new MenuItemAction(this, Global.resourceString("Insert_Node")));
		var.add(new MenuItemAction(this, Global.resourceString("Insert_Variation")));
		var.addSeparator();
		var.add(new MenuItemAction(this, Global.resourceString("Next_Game")));
		var.add(new MenuItemAction(this, Global.resourceString("Previous_Game")));
		var.addSeparator();
		var.add(new MenuItemAction(this, Global.resourceString("Search")));
		var.add(new MenuItemAction(this, Global.resourceString("Search_Again")));
		var.addSeparator();
		var.add(new MenuItemAction(this, Global.resourceString("Node_Name")));
		var.add(new MenuItemAction(this, Global.resourceString("Goto_Next_Name")));
		var.add(new MenuItemAction(this, Global.resourceString("Goto_Previous_Name")));
		M.add(var);
		Menu score = new MyMenu(Global.resourceString("Finish_Game"));
		M.add(score);
		score.add(new MenuItemAction(this, Global.resourceString("Remove_groups")));
		score.add(new MenuItemAction(this, Global.resourceString("Score")));
		score.addSeparator();
		score.add(new MenuItemAction(this, Global.resourceString("Game_Information")));
		score.add(new MenuItemAction(this, Global.resourceString("Game_Copyright")));
		score.addSeparator();
		score.add(new MenuItemAction(this, Global.resourceString("Prisoner_Count")));
		Menu options = new MyMenu(Global.resourceString("Options"));
		Menu mc = new MyMenu(Global.resourceString("Coordinates"));
		mc.add(Coordinates = new CheckboxMenuItemAction(this, Global.resourceString("On")));
		Coordinates.setState(Global.getParameter("coordinates", true));
		mc.add(UpperLeftCoordinates = new CheckboxMenuItemAction(this, Global.resourceString("Upper_Left")));
		UpperLeftCoordinates.setState(Global.getParameter("upperleftcoordinates", true));
		mc.add(LowerRightCoordinates = new CheckboxMenuItemAction(this, Global.resourceString("Lower_Right")));
		LowerRightCoordinates.setState(Global.getParameter("lowerrightcoordinates", true));
		options.add(mc);
		options.addSeparator();
		options.add(MenuBWColor = new CheckboxMenuItemAction(this, Global.resourceString("Use_B_W_marks")));
		MenuBWColor.setState(Global.getParameter("bwcolor", false));
		BWColor = MenuBWColor.getState();
		options.add(CommentSGF = new CheckboxMenuItemAction(this, Global.resourceString("Use_SGF_Comments")));
		CommentSGF.setState(Global.getParameter("sgfcomments", false));
		options.addSeparator();
		Menu fonts = new MyMenu(Global.resourceString("Fonts"));
		fonts.add(new MenuItemAction(this, Global.resourceString("Board_Font")));
		fonts.add(new MenuItemAction(this, Global.resourceString("Fixed_Font")));
		fonts.add(new MenuItemAction(this, Global.resourceString("Normal_Font")));
		options.add(fonts);
		Menu variations = new MyMenu(Global.resourceString("Variation_Display"));
		variations.add(VCurrent = new CheckboxMenuItemAction(this, Global.resourceString("To_Current")));
		VCurrent.setState(Global.getParameter("vcurrent", true));
		variations.add(VChild = new CheckboxMenuItemAction(this, Global.resourceString("To_Child")));
		VChild.setState( !Global.getParameter("vcurrent", true));
		variations.add(VHide = new CheckboxMenuItemAction(this, Global.resourceString("Hide")));
		VHide.setState(Global.getParameter("vhide", false));
		variations.addSeparator();
		variations.add(VNumbers = new CheckboxMenuItemAction(this, Global.resourceString("Continue_Numbers")));
		VNumbers.setState(Global.getParameter("variationnumbers", false));
		options.add(variations);
		options.addSeparator();
		options.add(MenuTarget = new CheckboxMenuItemAction(this, Global.resourceString("Show_Target")));
		MenuTarget.setState(Global.getParameter("showtarget", true));
		ShowTarget = MenuTarget.getState();
		options.add(MenuLastNumber = new CheckboxMenuItemAction(this, Global.resourceString("Last_Number")));
		MenuLastNumber.setState(Global.getParameter("lastnumber", false));
		LastNumber = MenuLastNumber.getState();
		options.add(new MenuItemAction(this, Global.resourceString("Last_50")));
		options.add(new MenuItemAction(this, Global.resourceString("Last_100")));
		options.addSeparator();
		options.addSeparator();
		options.add(new MenuItemAction(this, Global.resourceString("Set_Encoding")));
		options.add(ShowButtons = new CheckboxMenuItemAction(this, Global.resourceString("Show_Buttons")));
		ShowButtons.setState(Global.getParameter("showbuttons", true));
		Menu help = new MyMenu(Global.resourceString("Help"));
		help.add(new MenuItemAction(this, Global.resourceString("Board_Window")));
		help.add(new MenuItemAction(this, Global.resourceString("Making_Moves")));
		help.add(new MenuItemAction(this, Global.resourceString("Keyboard_Shortcuts")));
		help.add(new MenuItemAction(this, Global.resourceString("About_Variations")));
		help.add(new MenuItemAction(this, Global.resourceString("Playing_Games")));
		help.add(new MenuItemAction(this, Global.resourceString("Mailing_Games")));
		M.add(options);
		M.setHelpMenu(help);
		// Board
		L = new OutputLabel(Global.resourceString("New_Game"));
		Lm = new OutputLabel("--");

		MyPanel BP = new MyPanel(new BorderLayout());
		BP.add("Center", B);
		// Add the label
		SimplePanel sp = new SimplePanel(L, 80, Lm, 20);
		BP.add("South", sp);
		// Text Area
		Comment = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
		Comment.setFont(Global.SansSerif);
		JPanel bcp;
		if (Global.getParameter("shownavigationtree", true))
		{
			Navigation = new NavigationPanel(boardState);
			bcp = new BoardCommentPanel(new Panel3D(BP),
				new CommentNavigationPanel(new Panel3D(Comment), new Panel3D(
					Navigation)), B);
		}
		else bcp = new BoardCommentPanel(new Panel3D(BP), new Panel3D(Comment),
			B);
		add("Center", bcp);
		// Navigation panel
		IB = createIconBar();
		ButtonP = new Panel3D(IB);
		if (Global.getParameter("showbuttons", true)) add("South", ButtonP);
		// Directory for FileDialog
		Dir = new String("");
		Global.setwindow(this, "board", 500, 450, false);
		validate();
		Show = true;
		B.addKeyListener(this);
		if (Navigation != null) Navigation.addKeyListener(B);
		addmenuitems();
		repaint();
	}

	public void addmenuitems ()
	// for children to add menu items (because of bug in Linux Java 1.5)
	{}

	public IconBar createIconBar ()
	{
		Global.setParameter("iconsize", 32);
		IconBar I = new IconBar(this);
		I.Resource = "/icons/";
		I.addLeft("undo");
		I.addSeparatorLeft();
		I.addLeft("allback");
		I.addLeft("fastback");
		I.addLeft("back");
		I.addLeft("forward");
		I.addLeft("fastforward");
		I.addLeft("allforward");
		I.addSeparatorLeft();
		I.addLeft("variationback");
		I.addLeft("variationforward");
		I.addLeft("variationstart");
		I.addLeft("main");
		I.addLeft("mainend");
		I.addSeparatorLeft();
		String icons[] =
		{
			"mark", "square", "triangle", "circle", "letter", "text", "",
			"black", "white", "", "setblack", "setwhite", "delete"
		};
		I.addToggleGroupLeft(icons);
		I.addSeparatorLeft();
		I.addLeft("deletemarks");
		I.addLeft("play");
		I.setIconBarListener(this);
		return I;
	}

	public void iconPressed (String s)
	{
		if (s.equals("undo"))
			doAction(Global.resourceString("Undo"));
		else if (s.equals("allback"))
			doAction("I<<");
		else if (s.equals("fastback"))
			doAction("<<");
		else if (s.equals("back"))
			doAction("<");
		else if (s.equals("forward"))
			doAction(">");
		else if (s.equals("fastforward"))
			doAction(">>");
		else if (s.equals("allforward"))
			doAction(">>I");
		else if (s.equals("variationback"))
			doAction("<rankValue");
		else if (s.equals("variationstart"))
			doAction("rankValue");
		else if (s.equals("variationforward"))
			doAction("rankValue>");
		else if (s.equals("main"))
			doAction("*");
		else if (s.equals("mainend"))
			doAction("**");
		else if (s.equals("mark"))
			B.mark();
		else if (s.equals("mark"))
			B.mark();
		else if (s.equals("square"))
			B.specialmark(Field.Marker.SQUARE);
		else if (s.equals("triangle"))
			B.specialmark(Field.Marker.TRIANGLE);
		else if (s.equals("circle"))
			B.specialmark(Field.Marker.CIRCLE);
		else if (s.equals("letter"))
			B.letter();
		else if (s.equals("text"))
		{
			B.textmark(Text);
			if (tmq == null)
			{
				tmq = new TextMarkQuestion(this, Text);
				tmq.setVisible(true);
			}
		}
		else if (s.equals("black"))
			B.black();
		else if (s.equals("white"))
			B.white();
		else if (s.equals("setblack"))
			B.setblack();
		else if (s.equals("setwhite"))
			B.setwhite();
		else if (s.equals("delete"))
			B.deletestones();
		else if (s.equals("deletemarks"))
			boardState.clearmarks();
		else if (s.equals("play")) B.resume();
	}

	@Override
	public void doAction (String o)
	{
		try
		{
			if (Global.resourceString("Undo").equals(o))
			{
				B.undo();
			}
			else if (Global.resourceString("Close").equals(o))
			{
				close();
			}
			else if (Global.resourceString("Board_size").equals(o))
			{
				boardsize();
			}
			else if ("<".equals(o))
			{
				boardState.back();
				uiState.stateChanged();
			}
			else if (">".equals(o))
			{
				boardState.forward();
				uiState.stateChanged();
			}
			else if (">>".equals(o))
			{
				boardState.fastforward();
				uiState.stateChanged();
			}
			else if ("<<".equals(o))
			{
				boardState.fastback();
				uiState.stateChanged();
			}
			else if ("I<<".equals(o))
			{
				boardState.allback();
				uiState.stateChanged();
			}
			else if (">>I".equals(o))
			{
				boardState.allforward();
				uiState.stateChanged();
			}
			else if ("<rankValue".equals(o))
			{
				boardState.varleft();
				uiState.stateChanged();
			}
			else if ("rankValue>".equals(o))
			{
				boardState.varright();
				uiState.stateChanged();
			}
			else if ("rankValue".equals(o))
			{
				boardState.varup();
				uiState.stateChanged();
			}
			else if ("**".equals(o))
			{
				boardState.varmaindown();
				uiState.stateChanged();
			}
			else if ("*".equals(o))
			{
				boardState.varmain();
				uiState.stateChanged();
			}
			else if (Global.resourceString("Pass").equals(o))
			{
				boardState.pass();
				notepass();
				uiState.stateChanged();
			}
			else if (Global.resourceString("Resume_playing").equals(o))
			{
				B.resume();
			}
			else if (Global.resourceString("Clear_all_marks").equals(o))
			{
				boardState.clearmarks();
				uiState.stateChanged();
			}
			else if (Global.resourceString("Undo_Adding_Removing").equals(o))
			{
				boardState.clearremovals();
				uiState.stateChanged();
			}
			else if (Global.resourceString("Remove_groups").equals(o))
			{
				B.score();
			}
			else if (Global.resourceString("Score").equals(o))
			{
				String s = boardState.done();
				if (s != null) new Message(this, s).setVisible(true);
			}
			else if (Global.resourceString("Local_Count").equals(o))
			{
				new Message(this, boardState.docount()).setVisible(true);
			}
			else if (Global.resourceString("Print").equals(o)) // print the game
			{
				B.print(Global.frame());
			}
			else if (Global.resourceString("Board_Window").equals(o))
			{
				new Help("board").display();
			}
			else if (Global.resourceString("Making_Moves").equals(o))
			{
				new Help("moves").display();
			}
			else if (Global.resourceString("Keyboard_Shortcuts").equals(o))
			{
				new Help("keyboard").display();
			}
			else if (Global.resourceString("Playing_Games").equals(o))
			{
				new Help("playing").display();
			}
			else if (Global.resourceString("About_Variations").equals(o))
			{
				new Help("variations").display();
			}
			else if (Global.resourceString("Insert_Node").equals(o))
			{
				boardState.insertnode();
			}
			else if (Global.resourceString("Insert_Variation").equals(o))
			{
				boardState.insertvariation();
			}
			else if (Global.resourceString("Game_Information").equals(o))
			{
				new EditInformation(this, boardState.firstnode()).setVisible(true);
			}
			else if (Global.resourceString("Game_Copyright").equals(o))
			{
				new EditCopyright(this, boardState.firstnode()).setVisible(true);
			}
			else if (Global.resourceString("Prisoner_Count").equals(o))
			{
				String s = Global.resourceString("Black__") + boardState.getBoardPosition().Pw
					+ Global.resourceString("__White__") + boardState.getBoardPosition().Pb + "\n"
					+ Global.resourceString("Komi") + " " + boardState.getKomi();
				new Message(this, s).setVisible(true);
			}
			else if (Global.resourceString("Board_Font").equals(o))
			{
				new BoardGetFontSize(this, "boardfontname", Global.getParameter(
					"boardfontname", "SansSerif"), "boardfontsize", Global
					.getParameter("boardfontsize", 10), true).setVisible(true);
				updateall();
			}
			else if (Global.resourceString("Normal_Font").equals(o))
			{
				new BoardGetFontSize(this, "sansserif", Global.getParameter(
					"sansserif", "SansSerif"), "ssfontsize", Global.getParameter(
					"ssfontsize", 11), true).setVisible(true);
				updateall();
			}
			else if (Global.resourceString("Fixed_Font").equals(o))
			{
				new BoardGetFontSize(this, "monospaced", Global.getParameter(
					"monospaced", "Monospaced"), "msfontsize", Global.getParameter(
					"msfontsize", 11), true).setVisible(true);
				updateall();
			}
			else if (Global.resourceString("Last_50").equals(o))
			{
				B.lastrange(50);
			}
			else if (Global.resourceString("Last_100").equals(o))
			{
				B.lastrange(100);
			}
			else if (Global.resourceString("Node_Name").equals(o))
			{
				callInsert();
			}
			else if (Global.resourceString("Goto_Next_Name").equals(o))
			{
				boardState.gotonext();
			}
			else if (Global.resourceString("Goto_Previous_Name").equals(o))
			{
				boardState.gotoprevious();
			}
			else if (Global.resourceString("Set_Encoding").equals(o))
			{
				new GetEncoding(this).setVisible(true);
			}
			else if (Global.resourceString("Search_Again").equals(o))
			{
				search();
			}
			else if (Global.resourceString("Search").equals(o))
			{
				new GetSearchString(this).setVisible(true);
			}
			else super.doAction(o);
		}
		catch (IOException ex)
		{
			new Message(Global.frame(), ex.getMessage()).setVisible(true);
		}
	}

	public void center (FileDialog d)
	{
		Point lo = getLocation();
		Dimension di = getSize();
		d.setLocation(lo.x + di.width / 2 - 100, lo.y + di.height / 2 - 100);
	}

	public void search ()
	{
		boardState.search(Global.getParameter("searchstring", "++"));
	}

	@Override
	public void itemAction (String o, boolean flag)
	{
		if (Global.resourceString("Set_Black").equals(o))
		{
			B.setblack();
		}
		else if (Global.resourceString("Set_White").equals(o))
		{
			B.setwhite();
		}
		else if (Global.resourceString("Black_to_play").equals(o))
		{
			B.black();
		}
		else if (Global.resourceString("White_to_play").equals(o))
		{
			B.white();
		}
		else if (Global.resourceString("Mark").equals(o))
		{
			B.mark();
		}
		else if (Global.resourceString("Text").equals(o))
		{
			B.textmark(Text);
			if (tmq == null)
			{
				tmq = new TextMarkQuestion(this, Text);
				tmq.setVisible(true);
			}
		}
		else if (Global.resourceString("Square").equals(o))
		{
			B.specialmark(Field.Marker.SQUARE);
		}
		else if (Global.resourceString("Triangle").equals(o))
		{
			B.specialmark(Field.Marker.TRIANGLE);
		}
		else if (Global.resourceString("Cross").equals(o))
		{
			B.specialmark(Field.Marker.CROSS);
		}
		else if (Global.resourceString("Circle").equals(o))
		{
			B.specialmark(Field.Marker.CIRCLE);
		}
		else if (Global.resourceString("Letter").equals(o))
		{
			B.letter();
		}
		else if (Global.resourceString("Delete").equals(o))
		{
			B.deletestones();
		}
		else if (Global.resourceString("True_Color_Board").equals(o))
		{
			Global.setParameter("beauty", flag);
			updateall();
		}
		else if (Global.resourceString("True_Color_Stones").equals(o))
		{
			Global.setParameter("beautystones", flag);
			updateall();
		}
		else if (Global.resourceString("Anti_alias_Stones").equals(o))
		{
			Global.setParameter("alias", flag);
			updateall();
		}
		else if (Global.resourceString("Shadows").equals(o))
		{
			Global.setParameter("shadows", flag);
			updateall();
		}
		else if (Global.resourceString("Black_Only").equals(o))
		{
			Global.setParameter("blackonly", flag);
			updateall();
		}
		else if (Global.resourceString("Use_B_W_marks").equals(o))
		{
			BWColor = flag;
			Global.setParameter("bwcolor", BWColor);
			updateall();
		}
		else if (Global.resourceString("Last_Number").equals(o))
		{
			LastNumber = flag;
			Global.setParameter("lastnumber", LastNumber);
			B.updateall();
		}
		else if (Global.resourceString("Show_Target").equals(o))
		{
			ShowTarget = flag;
			Global.setParameter("showtarget", ShowTarget);
		}
		else if (Global.resourceString("Hide").equals(o))
		{
			B.setVariationStyle(flag, VCurrent.getState());
			Global.setParameter("vhide", flag);
		}
		else if (Global.resourceString("To_Current").equals(o))
		{
			VCurrent.setState(true);
			VChild.setState(false);
			B.setVariationStyle(VHide.getState(), true);
		}
		else if (Global.resourceString("To_Child").equals(o))
		{
			VCurrent.setState(false);
			VChild.setState(true);
			B.setVariationStyle(VHide.getState(), false);
		}
		else if (Global.resourceString("Continue_Numbers").equals(o))
		{
			Global.setParameter("variationnumbers", flag);
		}
	}

	// This can be used to set a board position
	// The board is updated directly, if it is at the
	// last move.
	/** set a black move at i,j */
	public void black (int i, int j)
	{
		boardState.black(i, j);
	}

	/** set a white move at i,j */
	public void white (int i, int j)
	{
		boardState.white(i, j);
	}

	/** set a black stone at i,j */
	public void setblack (int i, int j)
	{
		boardState.setblack(i, j);
	}

	/** set a black stone at i,j */
	public void setwhite (int i, int j)
	{
		boardState.setwhite(i, j);
	}

	/** mark the field at i,j as territory */
	public void territory (int i, int j)
	{
		territory(i, j);
	}

	/** Next to move */
	public void color (int c)
	{
		if (c == -1)
			B.white();
		else B.black();
	}

	/**
	 * This called from the board to set the menu checks according to the
	 * current state.
	 * 
	 *            the number of the state the Board is in.
	 */
	private void updateMenuChecks ()
	{
		Black.setState(false);
		White.setState(false);
		SetBlack.setState(false);
		SetWhite.setState(false);
		Mark.setState(false);
		Letter.setState(false);
		Hide.setState(false);
		Circle.setState(false);
		Cross.setState(false);
		Triangle.setState(false);
		Square.setState(false);
		TextMark.setState(false);
		switch (uiState.getUiMode())
		{
			case PLAY_BLACK:
				Black.setState(true);
				IB.setState("black", true);
				break;
			case PLAY_WHITE:
				White.setState(true);
				IB.setState("white", true);
				break;
			case ADD_BLACK:
				SetBlack.setState(true);
				IB.setState("setblack", true);
				break;
			case ADD_WHITE:
				SetWhite.setState(true);
				IB.setState("setwhite", true);
				break;
			case MARK:
				Mark.setState(true);
				IB.setState("mark", true);
				break;
			case LETTER:
				Letter.setState(true);
				IB.setState("letter", true);
				break;
			case HIDE:
				Hide.setState(true);
				break;
			case DELETE_STONE:
				IB.setState("delete", true);
				break;
			case TEXT_MARK:
				TextMark.setState(true);
				IB.setState("text", true);
				break;
		}
	}

	/**
	 * Called from board to check the proper menu for markers.
	 * 
	 * @param i
	 *            the marker type.
	 */
	@Override
	public void setMarkState (Field.Marker i)
	{
		setState(0);
		switch (i)
		{
			case SQUARE:
				Square.setState(true);
				break;
			case TRIANGLE:
				Triangle.setState(true);
				break;
			case CROSS:
				Cross.setState(true);
				break;
			case CIRCLE:
				Circle.setState(true);
				break;
		}
		switch (i)
		{
			case SQUARE:
				IB.setState("square", true);
				break;
			case TRIANGLE:
				IB.setState("triangle", true);
				break;
			case CROSS:
				IB.setState("mark", true);
				break;
			case CIRCLE:
				IB.setState("circle", true);
				break;
		}
	}

	/**
	 * Called from board to enable and disable navigation buttons.
	 */
	@Override
	public void stateChanged ()
	{
		Node n = uiState.getBoardState().current().content();

		IB.setEnabled("variationstart", !n.main());
		IB.setEnabled("main", !n.main());
		IB.setEnabled("mainend", !n.main() || boardState.hasChildren());
		IB.setEnabled("sendforward", n.main() && !boardState.hasChildren());
		IB.setEnabled("fastback", boardState.current().parent() != null);
		IB.setEnabled("back", boardState.current().parent() != null);
		IB.setEnabled("allback", boardState.current().parent() != null);
		IB.setEnabled("fastforward", boardState.hasChildren());
		IB.setEnabled("forward", boardState.hasChildren());
		IB.setEnabled("allforward", boardState.hasChildren());

		IB.setEnabled("variationback", boardState.current().parent() != null
				&& boardState.current().parent().firstchild() != boardState.current());
		IB.setEnabled("variationforward", boardState.current().parent() != null
				&& boardState.current().parent().lastchild() != boardState.current());

		int number = n.number();
		String NodeName = n.getaction(Action.Type.NAME);
		String ms = "";
		String LText;
		if (n.main())
		{
			if ( !uiState.getBoardState().hasChildren())
				ms = "** ";
			else ms = "* ";
		}
		LText = ms + uiState.getUiMode().getDescription();
		switch (uiState.getUiMode())
		{
			case TEXT_MARK:
				LText = ms + Global.resourceString("Text__") + uiState.getTextMarker();
				break;
			default:
				if (uiState.getBoardPosition().color() > 0)
				{
					String s = uiState.getBoardState().lookuptime(Action.Type.BLACK_TIME);
					if ( !s.equals(""))
						LText += number + " (" + s + ")";
					else LText += number;
				}
				else
				{
					String s = uiState.getBoardState().lookuptime(Action.Type.WHITE_TIME);
					if ( !s.equals(""))
						LText += number + " (" + s + ")";
					else LText += number;
				}
		}
		if (uiState.getBoardState().hasParent())
		{
			LText += " (" + uiState.getBoardState().siblings() + " " + Global.resourceString("Siblings") + ", ";
		}
		LText += uiState.getBoardState().children() + " " + Global.resourceString("Children") + ")";
		setLabel(LText);
		updateMenuChecks();

		setComment(uiState.getBoardPosition().getCurrentComment());
		setLabelM(uiState.getLabelM());
	}

	/**
	 * Called from the edit marker label dialog, when its text has been entered
	 * by the user.
	 * 
	 * @param s
	 *            the marker to be used by the board
	 */
	void setTextmark (String s)
	{
		B.textmark(s);
	}

	/** A blocked board cannot react to the user. */
	public boolean blocked ()
	{
		return false;
	}

	// The following are used from external board
	// drivers to set stones, handicap etc. (like
	// distributors for IGS commands)
	/** see, if the board is already acrive */
	public void active (boolean f)
	{
		B.active(f);
	}

	/** pass the Board */
	public void pass ()
	{
		boardState.pass();
	}

	public void setpass ()
	{
		boardState.setpass();
	}

	/** Notify about pass */
	public void notepass ()
	{}

	/**
	 * Set a handicap to the Board.
	 * 
	 * @param n
	 *            number of stones
	 */
	public void handicap (int n)
	{
		B.handicap(n);
	}

	/** set a move at i,j (called from Board) */
	public boolean moveset (int i, int j)
	{
		return true;
	}

	/** pass (only proceeded from ConnectedGoFrame) */
	public void movepass ()
	{}

	/**
	 * Undo moves on the board (called from a distributor e.g.)
	 * 
	 * @param n
	 *            numbers of moves to undo.
	 */
	public void undo (int n)
	{
		boardState.undo(n);
	}

	/** undo (only processed from ConnectedGoFrame) */
	public void undo ()
	{}

	@Override
	public boolean close () // try to close
	{
		if (Global.getParameter("confirmations", true))
		{
			CloseQuestion CQ = new CloseQuestion(this);
			CQ.setVisible(true);
			if (CQ.Result)
			{
				Global.notewindow(this, "board");
				doclose();
			}
			return false;
		}
		else
		{
			Global.notewindow(this, "board");
			doclose();
			return false;
		}
	}

	/**
	 * Called from the BoardsizeQuestion dialog.
	 * 
	 * @param s
	 *            the size of the board.
	 */
	public void doboardsize (int s)
	{
		//boardState.setsize(s);
	}

	/** called by menu action, opens a SizeQuestion dialog */
	public void boardsize ()
	{
		new SizeQuestion(this).setVisible(true);
	}

	/**
	 * Determine the board size (for external purpose)
	 * 
	 * @return the board size
	 */
	public int getboardsize ()
	{
		return B.getboardsize();
	}

	/** add a comment to the board (called from external sources) */
	public void addComment (String s)
	{
		boardState.addcomment(s);
	}

	public void result (int b, int w)
	{}

	public void yourMove (boolean notinpos)
	{}

	InputStreamReader LaterLoad = null;
	boolean LaterLoadXml;
	int LaterMove = 0;
	String LaterFilename = "";
	boolean Activated = false;

	/**
	 * Note that the board must only load a file, when it is ready. This is to
	 * interpret a command line argument SGF filename.
	 */
	public synchronized void load (String file, int move)
	{
		LaterFilename = FileName.purefilename(file);
		LaterMove = move;
		try
		{
			if (file.endsWith(".xml"))
			{
				LaterLoad = new InputStreamReader(new FileInputStream(file),
					"UTF8");
				LaterLoadXml = true;
			}
			else
			{
				LaterLoad = new InputStreamReader(new FileInputStream(file));
				LaterLoadXml = false;
			}
		}
		catch (Exception e)
		{
			LaterLoad = null;
		}
		if (LaterLoad != null && Activated) activate();
	}

	public void load (String file)
	{
		load(file, 0);
	}

	/**
	 * Note that the board must load a file, when it is ready. This is to
	 * interpret a command line argument SGF filename.
	 */
	public void load (URL file)
	{
		LaterFilename = file.toString();
		try
		{
			if (file.toExternalForm().endsWith(".xml"))
			{
				LaterLoad = new InputStreamReader(file.openStream(), "UTF8");
				LaterLoadXml = true;
			}
			else
			{
				LaterLoad = new InputStreamReader(file.openStream());
				LaterLoadXml = false;
			}
		}
		catch (Exception e)
		{
			LaterLoad = null;
		}
	}

	/** Actually do the loading, when the board is ready. */
	public void doload (Reader file)
	{
		validate();
		try
		{
			boardState.load(new BufferedReader(file));
			file.close();
			setGameTitle(LaterFilename);
			boardState.gotoMove(LaterMove);
		}
		catch (Exception ex)
		{
			rene.dialogs.Warning w = new rene.dialogs.Warning(this, ex
				.toString(), "Warning", true);
			w.center(this);
			w.setVisible(true);
		}
	}

	public void setGameTitle (String filename)
	{
		String s = boardState.firstnode().getaction(Action.Type.GAME_NAME);
		if (s != null && !s.equals(""))
		{
			setTitle(s);
		}
		else
		{
			boardState.firstnode().addaction(new Action(Action.Type.GAME_NAME, filename));
			setTitle(filename);
		}
	}

	/** Actually do the loading, when the board is ready. */
	private void doloadXml (Reader file)
	{
		validate();
		try
		{
			XmlReader xml = new XmlReader(new BufferedReader(file));
			boardState.loadXml(xml);
			file.close();
			setGameTitle(LaterFilename);
		}
		catch (Exception ex)
		{
			rene.dialogs.Warning w = new rene.dialogs.Warning(this, ex
				.toString(), "Warning", true);
			w.center(this);
			w.setVisible(true);
		}
	}

	public void lostOwnership (Clipboard b, Transferable s)
	{}

	public synchronized void activate ()
	{
		Activated = true;
		if (LaterLoad != null)
		{
			if (LaterLoadXml)
				doloadXml(LaterLoad);
			else doload(LaterLoad);
		}
		LaterLoad = null;
	}

	/** Repaint the board, when color or font changes. */
	public void updateall ()
	{
		B.updateboard();
	}

	/**
	 * Opens a dialog to ask for deleting of game trees. This is called from the
	 * Board, if the node has grandchildren.
	 */
	public boolean askUndo ()
	{
		Question question = new AskUndoQuestion(this);
		question.setVisible(true);
		return question.Result;
	}

	/**
	 * Called from the board, when a node is to be inserted. Opens a dialog
	 * asking for permission.
	 */
	public boolean askInsert ()
	{
		Question question = new AskInsertQuestion(this);
		question.setVisible(true);
		return question.Result;
	}

	/**
	 * Sets the name of the Board (called from a Distributor)
	 * 
	 * @see jagoclient.igs.Distributor
	 */
	public void setname (String s)
	{
		boardState.setname(s);
	}

	/**
	 * Sets the name of the current board name in a dialog (called from the
	 * menu)
	 */
	public void callInsert ()
	{
		new NodeNameEdit(this, boardState.getname()).setVisible(true);
	}

	/**
	 * Remove a group at i,j in the board.
	 */
	public void remove (int i, int j)
	{
		//boardState.remove(i, j);
	}

	/**
	 * Called from the board to advance the text mark.
	 */
	public void advanceTextmark ()
	{
		if (tmq != null) tmq.advance();
	}

	/**
	 * Called from the board to set the comment of a board in the Comment text
	 * area.
	 */
	public void setComment (String s)
	{
		Comment.setText(s);
		Comment.append("");
	}

	/**
	 * Called from the Board to read the comment text area.
	 */
	public String getComment ()
	{
		return Comment.getText();
	}

	/**
	 * Called from outside to append something to the comment text area (e.g.
	 * from a Distributor).
	 */
	public void appendComment (String s)
	{
		Comment.append(s);
	}

	/**
	 * Called from the board to set the Label below the board.
	 */
	private void setLabel (String s)
	{
		L.setText(s);
		if (Navigation != null) Navigation.repaint();
	}

	/**
	 * Called from the board to set the label for the cursor position.
	 */
	public void setLabelM (String s)
	{
		Lm.setText(s);
	}

	public boolean boardShowing ()
	{
		return Show;
	}

	public boolean lastNumber ()
	{
		return LastNumber;
	}

	public boolean showTarget ()
	{
		return ShowTarget;
	}

	/**
	 * Process the insert key, which set the node name by opening the
	 * correspinding dialog.
	 */
	public void keyPressed (KeyEvent e)
	{
		if (e.isActionKey())
		{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_INSERT:
					callInsert();
					break;
			}
		}
		else
		{
			switch (e.getKeyChar())
			{
				case 'f':
				case 'F':
					boardState.search(Global.getParameter("searchstring", "++"));
					break;
			}
		}
	}

	public void keyReleased (KeyEvent e)
	{}

	public void keyTyped (KeyEvent e)
	{}

	// interface routines for the BoardInterface

	public Color getColor (String key, int r, int g, int b, Color c)
	{
		return Global.getColor(key, r, g, b, c);
	}

	public String resourceString (String S)
	{
		return Global.resourceString(S);
	}

	public String version ()
	{
		return "Version " + resourceString("Version");
	}

	public boolean getParameter (String s, boolean f)
	{
		return Global.getParameter(s, f);
	}

	public Font boardFont ()
	{
		return Global.BoardFont;
	}

	public Frame frame ()
	{
		return Global.frame();
	}

	public Color backgroundColor ()
	{
		return Color.gray;
	}
}
