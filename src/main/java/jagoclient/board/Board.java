package jagoclient.board;

import jagoclient.Global;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.BufferedReader;

import java.util.LinkedList;
import rene.util.list.Tree;

import javax.swing.*;


//******************* Board ***********************

/**
 * This is the main file for presenting a Go board.
 * <boardPosition>
 * Handles the complete display and storage of a Go board. The display is kept
 * on an offscreen image. Stores a Go game in a node list (with variants).
 * Handles the display of the current node.
 * <boardPosition>
 * This class handles mouse input to set the next move. It also has methods to
 * move in the node tree from external sources.
 * <boardPosition>
 * A BoardInterface is used to encorporate the board into an environment.
 */

public class Board extends JPanel implements MouseListener,
	MouseMotionListener, KeyListener, Position.PositionUpdatedHandler
{
	private int O, W, D, S, OT, OTU, OP; // pixel coordinates
	// O=offset, W=total width, D=field width, S=board size (9,11,13,19)
	// OT=offset for coordinates to the right and below
	// OTU=offset above the board and below for coordinates
	private int lasti = -1, lastj = 0; // last move (used to highlight the move)
	private boolean showlast; // internal flag, if last move is to be highlighted
	private Image Empty, EmptyShadow, ActiveImage;
	// offscreen images of empty and current board
	private int CurrentTree; // the currently displayed tree
	Position boardPosition; // current board position
	BoardState boardState;
	private int number;
	int State; // states: 1 is black, 2 is white, 3 is set black etc.
	// see GoFrame.setState(int)
	private Font font; // Font for board letters and coordinates
	private FontMetrics fontmetrics; // metrics of this font
	BoardInterface GF; // frame containing the board
	private boolean Active;
	private int MainColor = 1;
	public int MyColor = 0;
	// board position which has been sended to server
	private Dimension Dim; // Note size to check for resizeing at paint
	private Field.Marker SpecialMarker = Field.Marker.SQUARE;
	private String TextMarker = "A";
	BufferedReader LaterLoad = null; // File to be loaded at repaint
	private Image BlackStone, WhiteStone;
	private int Range = -1; // Numbers display from this one
	private boolean KeepRange = false;
	private String NodeName = "", LText = "";
	private boolean DisplayNodeName = false;
	private boolean Removing = false;
	private boolean Activated = false;
	public boolean Teaching = false; // enable teaching mode
	private boolean VCurrent = false; // show variations to current move
	private boolean VHide = false; // hide variation markers

	private Color BoardColor = new Color(170, 120, 70);
	private Color BlackColor = new Color(30, 30, 30);
	private Color BlackSparkleColor = new Color(120, 120, 120);
	private Color WhiteColor = new Color(210, 210, 210);
	private Color WhiteSparkleColor = new Color( 250, 250, 250);
	Color MarkerColor = Color.BLUE;
	Color LabelColor = Color.PINK.darker();

	// ******************** initialize board *******************

	public Board (int size, BoardInterface gf)
	{
		S = size;
		D = 16;
		W = S * D;
		Empty = null;
		EmptyShadow = null;
		showlast = true;
		GF = gf;
		State = 1;
		boardState = new BoardState(S);
		boardPosition = boardState.getBoardPosition();
		boardPosition.addPositionUpdatedHandler(this);
		number = 1;
		CurrentTree = 0;
		Active = true;
		Dim = new Dimension();
		Dim.width = 0;
		Dim.height = 0;
		setfonts();
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		VHide = GF.getParameter("vhide", false);
		VCurrent = GF.getParameter("vcurrent", true);
	}

	public Color labelColor (int color)
	{
		switch (color)
		{
			case 1:
				return LabelColor.brighter().brighter();
			case -1:
				return LabelColor.darker().darker();
			default:
				return LabelColor.brighter();
		}
	}

	public Color markerColor (int color)
	{
		switch (color)
		{
			case 1:
				return MarkerColor.brighter().brighter();
			case -1:
				return MarkerColor.darker().darker();
			default:
				return MarkerColor;
		}
	}

	void setfonts ()
	// get the font from the go frame
	{
		font = Global.BoardFont;
		fontmetrics = getFontMetrics(font);
	}

	@Override
	public Dimension getMinimumSize ()
	// for the layout menager of the containing component
	{
		Dimension d = getSize();
		if (d.width == 0) return d = Dim;
		d.width = d.height + 5;
		return d;
	}

	@Override
	public Dimension getPreferredSize ()
	// for the layout menager of the containing component
	{
		return getMinimumSize();
	}

	// ************** paint ************************

	private synchronized void makeimages ()
	// create images and repaint, if ActiveImage is invalid.
	// uses parameters from the BoardInterface for coordinate layout.
	{
		Dim = getSize();
		boolean c = GF.getParameter("coordinates", true);
		boolean ulc = GF.getParameter("upperleftcoordinates", true);
		boolean lrc = GF.getParameter("lowerrightcoordinates", false);
		D = Dim.height / (S + 1 + (c?(ulc?1:0) + (lrc?1:0):0));
		OP = D / 4;
		O = D / 2 + OP;
		W = S * D + 2 * O;
		if (c)
		{
			if (lrc)
				OT = D;
			else OT = 0;
			if (ulc)
				OTU = D;
			else OTU = 0;
		}
		else OT = OTU = 0;
		W += OTU + OT;
		if ( !GF.boardShowing()) return;
		// create image and paint empty board
		synchronized (this)
		{
			Empty = createImage(W, W);
			EmptyShadow = createImage(W, W);
		}
		emptypaint();
		ActiveImage = createImage(W, W);
		// update the emtpy board
		updateall();
		repaint();
	}

	@Override
	public void paintComponent (Graphics g)
	// repaint the board (generate images at first call)
	{
		super.paintComponent(g);
		Dimension d = getSize();
		// test if ActiveImage is valid.
		if (Dim.width != d.width || Dim.height != d.height)
		{
			Dim = d;
			makeimages();
		}
		if (ActiveImage != null) {
			g.drawImage(ActiveImage, 0, 0, this);
		}
		if ( !Activated && GF.boardShowing())
		{
			Activated = true;
			GF.activate();
		}
		Color backgroundColor = Color.GRAY;
		g.setColor(backgroundColor);
		if (d.width > W) g.fillRect(W, 0, d.width - W, W);
		if (d.height > W) g.fillRect(0, W, d.width, d.height - W);
	}


	/**
	 * Try to paint the wooden board. If the size is correct, use the predraw
	 * board.
	 */
	private boolean trywood (Graphics gr, Graphics grs, int w)
	{
		if (!EmptyPaint.haveImage(w, w, new Color(170, 120, 70),
			OP + OP / 2, OP - OP / 2, D))
		// use predrawn image
		{
			new EmptyPaint(this, w, w, new Color(
					170, 120, 70), true, OP + OP / 2, OP - OP / 2, D).run();

		}
		gr.drawImage(EmptyPaint.StaticImage, O + OTU - OP, O + OTU - OP,
				this);
		if (EmptyPaint.StaticShadowImage != null && grs != null)
			grs.drawImage(EmptyPaint.StaticShadowImage, O + OTU - OP, O
					+ OTU - OP, this);
		return true;
	}

	final double pixel = 0.8, shadow = 0.7;

	private void stonespaint ()
	// Create the (beauty) images of the stones (black and white)
	{
		int col = BoardColor.getRGB();
		int blue = col & 0x0000FF, green = (col & 0x00FF00) >> 8, red = (col & 0xFF0000) >> 16;
		if (BlackStone == null || BlackStone.getWidth(this) != D + 2)
		{
			int d = D + 2;
			int pb[] = new int[d * d];
			int pw[] = new int[d * d];
			int i, j, g, k;
			double di, dj, d2 = d / 2.0 - 5e-1, r = d2 - 2e-1 - 1, f = Math.sqrt(3);
			double x, y, z, xr, xg, hh;
			k = 0;
			for (i = 0; i < d; i++)
				for (j = 0; j < d; j++)
				{
					di = i - d2;
					dj = j - d2;
					hh = r - Math.sqrt(di * di + dj * dj);
					if (hh >= 0)
					{
						z = r * r - di * di - dj * dj;
						if (z > 0)
							z = Math.sqrt(z) * f;
						else z = 0;
						x = di;
						y = dj;
						xr = Math.sqrt(6 * (x * x + y * y + z * z));
						xr = (2 * z - x + y) / xr;
						if (xr > 0.9)
							xg = (xr - 0.9) * 10;
						else xg = 0;
						if (hh > pixel)
						{
							g = (int)(10 + 10 * xr + xg * 140);
							pb[k] = 255 << 24 | g << 16 | g << 8 | g;
							g = (int)(200 + 10 * xr + xg * 45);
							pw[k] = 255 << 24 | g << 16 | g << 8 | g;
						}
						else
						{
							hh = (pixel - hh) / pixel;
							g = (int)(10 + 10 * xr + xg * 140);
							double shade;
							if (di - dj < r / 3)
								shade = 1;
							else shade = shadow;
							pb[k] = 255 << 24
								| (int)((1 - hh) * g + hh * shade * red) << 16
								| (int)((1 - hh) * g + hh * shade * green) << 8
								| (int)((1 - hh) * g + hh * shade * blue);
							g = (int)(200 + 10 * xr + xg * 45);
							pw[k] = 255 << 24
								| (int)((1 - hh) * g + hh * shade * red) << 16
								| (int)((1 - hh) * g + hh * shade * green) << 8
								| (int)((1 - hh) * g + hh * shade * blue);
						}
					}
					else pb[k] = pw[k] = 0;
					k++;
				}
			BlackStone = createImage(new MemoryImageSource(d, d, ColorModel
				.getRGBdefault(), pb, 0, d));
			WhiteStone = createImage(new MemoryImageSource(d, d, ColorModel
				.getRGBdefault(), pw, 0, d));
		}
	}

	private synchronized void emptypaint ()
	// Draw an empty board onto the graphics context g.
	// Including lines, coordinates and markers.
	{
		if (Empty == null || EmptyShadow == null) return;
		Graphics2D g = (Graphics2D)Empty.getGraphics(), gs = (Graphics2D)EmptyShadow
			.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

		g.setColor(Global.ControlBackground);
		g.fillRect(0, 0, S * D + 2 * OP + 100, S * D + 2 * OP + 100);
		trywood(g, gs, S * D + 2 * OP);
		stonespaint();
		g.setColor(Color.black);
		gs.setColor(Color.black);
		int i, j, x, y1, y2;
		// Draw lines
		x = O + OTU + D / 2;
		y1 = O + OTU + D / 2;
		y2 = O + D / 2 + OTU + (S - 1) * D;
		for (i = 0; i < S; i++)
		{
			g.drawLine(x, y1, x, y2);
			g.drawLine(y1, x, y2, x);
			gs.drawLine(x, y1, x, y2);
			gs.drawLine(y1, x, y2, x);
			x += D;
		}
		if (S == 19) // handicap markers
		{
			for (i = 0; i < 3; i++)
				for (j = 0; j < 3; j++)
				{
					hand(g, 3 + i * 6, 3 + j * 6);
					hand(gs, 3 + i * 6, 3 + j * 6);
				}
		}
		else if (S >= 11) // handicap markers
		{
			if (S >= 15 && S % 2 == 1)
			{
				int k = S / 2 - 3;
				for (i = 0; i < 3; i++)
					for (j = 0; j < 3; j++)
					{
						hand(g, 3 + i * k, 3 + j * k);
						hand(gs, 3 + i * k, 3 + j * k);
					}
			}
			else
			{
				hand(g, 3, 3);
				hand(g, S - 4, 3);
				hand(g, 3, S - 4);
				hand(g, S - 4, S - 4);
				hand(gs, 3, 3);
				hand(gs, S - 4, 3);
				hand(gs, 3, S - 4);
				hand(gs, S - 4, S - 4);
			}
		}
		// coordinates below and to the right
		if (OT > 0)
		{
			g.setFont(font);
			int y = O + OTU;
			int h = fontmetrics.getAscent() / 2 - 1;
			for (i = 0; i < S; i++)
			{
				String s = "" + (S - i);
				int w = fontmetrics.stringWidth(s) / 2;
				g.drawString(s, O + OTU + S * D + D / 2 + OP - w, y + D / 2
					+ h);
				y += D;
			}
			x = O + OTU;
			char a[] = new char[1];
			for (i = 0; i < S; i++)
			{
				j = i;
				if (j > 7) j++;
				if (j > 'Z' - 'A')
					a[0] = (char)('a' + j - ('Z' - 'A') - 1);
				else a[0] = (char)('A' + j);
				String s = new String(a);
				int w = fontmetrics.stringWidth(s) / 2;
				g.drawString(s, x + D / 2 - w, O + OTU + S * D + D / 2 + OP
					+ h);
				x += D;
			}
		}
		// coordinates to the left and above
		if (OTU > 0)
		{
			g.setFont(font);
			int y = O + OTU;
			int h = fontmetrics.getAscent() / 2 - 1;
			for (i = 0; i < S; i++)
			{
				String s = "" + (S - i);
				int w = fontmetrics.stringWidth(s) / 2;
				g.drawString(s, O + D / 2 - OP - w, y + D / 2 + h);
				y += D;
			}
			x = O + OTU;
			char a[] = new char[1];
			for (i = 0; i < S; i++)
			{
				j = i;
				if (j > 7) j++;
				if (j > 'Z' - 'A')
					a[0] = (char)('a' + j - ('Z' - 'A') - 1);
				else a[0] = (char)('A' + j);
				String s = new String(a);
				int w = fontmetrics.stringWidth(s) / 2;
				g.drawString(s, x + D / 2 - w, O + D / 2 - OP + h);
				x += D;
			}
		}
	}

	public void hand (Graphics g, int i, int j)
	// help function for emptypaint (Handicap point)
	{
		g.setColor(Color.black);
		int s = D / 10;
		if (s < 2) s = 2;
		g.fillRect(O + OTU + D / 2 + i * D - s, O + OTU + D / 2 + j * D - s,
			2 * s + 1, 2 * s + 1);
	}

	// *************** mouse events **********************

	public void mouseClicked (MouseEvent e)
	{}

	public void mouseDragged (MouseEvent e)
	{}

	boolean MouseDown = false; // mouse release only, if mouse pressed

	public void mousePressed (MouseEvent e)
	{
		MouseDown = true;
		requestFocus();
	}

	public void mouseReleased (MouseEvent e)
	// handle mouse input
	{
		if ( !MouseDown) return;
		int x = e.getX(), y = e.getY();
		MouseDown = false;
		if (ActiveImage == null) return;
		if ( !Active) return;
		x -= O + OTU;
		y -= O + OTU;
		int i = x / D, j = y / D; // determine position
		if (x < 0 || y < 0 || i < 0 || j < 0 || i >= S || j >= S) return;
		switch (State)
		{
			case 3: // set a black stone
				if (GF.blocked() && !boardState.hasChildren() && boardState.ismain()) return;
				if (e.isShiftDown() && e.isControlDown())
					setmousec(i, j, 1);
				else setmouse(i, j, 1);
				break;
			case 4: // set a white stone
				if (GF.blocked() && !boardState.hasChildren() && boardState.ismain()) return;
				if (e.isShiftDown() && e.isControlDown())
					setmousec(i, j, -1);
				else setmouse(i, j, -1);
				break;
			case 5:
				break;
			case 6:
				boardState.letter(i, j);
				break;
			case 7: // delete a stone
				if (e.isShiftDown() && e.isControlDown())
					deletemousec(i, j);
				else deletemouse(i, j);
				break;
			case 8: // remove a group
				//removemouse(i, j);
				break;
			case 9:
				boardState.specialmark(i, j);
				break;
			case 10:
				boardState.textmark(i, j);
				break;
			case 1:
			case 2: // normal move mode
				if (e.isShiftDown()) // create variation
				{
					if (e.isControlDown())
					{
						if (GF.blocked() && !boardState.hasChildren() && boardState.ismain()) return;
						boardState.changemove(i, j);
					}
					else {
						boardState.variation(i, j);
					}
				}
				else if (e.isControlDown())
				// goto variation
				{
					if (boardPosition.tree(i, j) != null)
					{
						boardState.gotovariation(i, j);
					}
				}
				else if (e.isMetaDown()) // right click
				{
					if (boardPosition.tree(i, j) != null)
					{
						boardState.gotovariation(i, j);
					}
					else {
						boardState.variation(i, j);
					}
				}
				else
				// place a W or B stone
				{
					if (GF.blocked() && !boardState.hasChildren() && boardState.ismain()) return;
					movemouse(i, j);
				}
				break;
		}
		showinformation();
		copy(); // show position
	}

	// target rectangle things

	protected int iTarget = -1, jTarget = -1;

	public synchronized void mouseMoved (MouseEvent e)
	// show coordinates in the Lm Label
	{
		if ( !Active) return;
		if (DisplayNodeName)
		{
			GF.setLabelM(LText);
			DisplayNodeName = false;
		}
		int x = e.getX(), y = e.getY();
		x -= O + OTU;
		y -= O + OTU;
		int i = x / D, j = y / D; // determine position
		if (i < 0 || j < 0 || i >= S || j >= S)
		{
			if (GF.showTarget())
			{
				iTarget = jTarget = -1;
				copy();
			}
			return;
		}
		if (GF.showTarget() && (iTarget != i || jTarget != j))
		{
			drawTarget(i, j);
			iTarget = i;
			jTarget = j;
		}
		GF.setLabelM(Field.coordinate(i, j, S));
	}

	public void drawTarget (int i, int j)
	{
		copy();
		Graphics g = getGraphics();
		if (g == null) return;
		i = O + OTU + i * D + D / 2;
		j = O + OTU + j * D + D / 2;
		g.setColor(Color.gray.brighter());
		g.drawRect(i - D / 4, j - D / 4, D / 2, D / 2);
		g.dispose();
	}

	public void mouseEntered (MouseEvent e)
	// start showing coordinates
	{
		if ( !Active) return;
		if (DisplayNodeName)
		{
			GF.setLabel(LText);
			DisplayNodeName = false;
		}
		int x = e.getX(), y = e.getY();
		x -= O + OTU;
		y -= O + OTU;
		int i = x / D, j = y / D; // determine position
		if (i < 0 || j < 0 || i >= S || j >= S) return;
		if (GF.showTarget())
		{
			drawTarget(i, j);
			iTarget = i;
			jTarget = j;
		}
		GF.setLabelM(Field.coordinate(i, j, S));
	}

	public void mouseExited (MouseEvent e)
	// stop showing coordinates
	{
		if ( !Active) return;
		GF.setLabelM("--");
		if ( !NodeName.equals(""))
		{
			GF.setLabel(NodeName);
			DisplayNodeName = true;
		}
		if (GF.showTarget())
		{
			iTarget = jTarget = -1;
			copy();
		}
	}

	// *************** keyboard events ********************

	public synchronized void keyPressed (KeyEvent e)
	{
		if (e.isActionKey())
		{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_DOWN:
					boardState.forward();
					break;
				case KeyEvent.VK_UP:
					boardState.back();
					break;
				case KeyEvent.VK_LEFT:
					boardState.varleft();
					break;
				case KeyEvent.VK_RIGHT:
					boardState.varright();
					break;
				case KeyEvent.VK_PAGE_DOWN:
					boardState.fastforward();
					break;
				case KeyEvent.VK_PAGE_UP:
					boardState.fastback();
					break;
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_DELETE:
					undo();
					break;
				case KeyEvent.VK_HOME:
					boardState.varmain();
					break;
				case KeyEvent.VK_END:
					boardState.varmaindown();
					break;
			}
		}
		else
		{
			switch (e.getKeyChar())
			{
				case '*':
					boardState.varmain();
					break;
				case '/':
					boardState.varmaindown();
					break;
				case 'v':
				case 'V':
					boardState.varup();
					break;
				case 'm':
				case 'M':
					mark();
					break;
				case 'p':
				case 'P':
					resume();
					break;
				case 'c':
				case 'C':
					specialmark(Field.Marker.CIRCLE);
					break;
				case 's':
				case 'S':
					specialmark(Field.Marker.SQUARE);
					break;
				case 't':
				case 'T':
					specialmark(Field.Marker.TRIANGLE);
					break;
				case 'l':
				case 'L':
					letter();
					break;
				case 'r':
				case 'R':
					specialmark(Field.Marker.CROSS);
					break;
				case 'w':
					setwhite();
					break;
				case 'b':
					setblack();
					break;
				case 'W':
					white();
					break;
				case 'B':
					black();
					break;
				case '+':
					boardState.gotonext();
					break;
				case '-':
					boardState.gotoprevious();
					break;
				// Bug (VK_DELETE not reported as ActionEvent)
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_DELETE:
					undo();
					break;
			}
		}
	}

	public void keyReleased (KeyEvent e)
	{}

	public void keyTyped (KeyEvent e)
	{}

	// set things on the board

	int captured = 0, capturei, capturej;

	public void showinformation ()
	// update the label to display the next move and who's turn it is
	// and disable variation buttons
	// update the navigation buttons
	// update the comment
	{
		Node n = boardState.current().content();
		number = n.number();
		NodeName = n.getaction(Action.Type.NAME);
		String ms = "";
		if (n.main())
		{
			if ( !boardState.hasChildren())
				ms = "** ";
			else ms = "* ";
		}
		switch (State)
		{
			case 3:
				LText = ms + Global.resourceString("Set_black_stones");
				break;
			case 4:
				LText = ms + Global.resourceString("Set_white_stones");
				break;
			case 5:
				LText = ms + Global.resourceString("Mark_fields");
				break;
			case 6:
				LText = ms + Global.resourceString("Place_letters");
				break;
			case 7:
				LText = ms + Global.resourceString("Delete_stones");
				break;
			case 8:
				LText = ms + Global.resourceString("Remove_prisoners");
				break;
			case 9:
				LText = ms + Global.resourceString("Set_special_marker");
				break;
			case 10:
				LText = ms + Global.resourceString("Text__") + TextMarker;
				break;
			default:
				if (boardPosition.color() > 0)
				{
					String s = boardState.lookuptime(Action.Type.BLACK_TIME);
					if ( !s.equals(""))
						LText = ms + Global.resourceString("Next_move__Black_")
							+ number + " (" + s + ")";
					else LText = ms + Global.resourceString("Next_move__Black_")
						+ number;
				}
				else
				{
					String s = boardState.lookuptime(Action.Type.WHITE_TIME);
					if ( !s.equals(""))
						LText = ms + Global.resourceString("Next_move__White_")
							+ number + " (" + s + ")";
					else LText = ms + Global.resourceString("Next_move__White_")
						+ number;
				}
		}
		if (boardState.hasParent())
		{
			LText += " (" + boardState.siblings() + " " + Global.resourceString("Siblings") + ", ";
		}
		LText += boardState.children() + " " + Global.resourceString("Children") + ")";
		if (NodeName.equals(""))
		{
			GF.setLabel(LText);
			DisplayNodeName = false;
		}
		else
		{
			GF.setLabel(NodeName);
			DisplayNodeName = true;
		}
		GF.setState(3, !n.main());
		GF.setState(4, !n.main());
		GF.setState(7, !n.main() || boardState.hasChildren());
		if (State == 1 || State == 2)
		{
			if (boardPosition.color() == 1)
				State = 1;
			else State = 2;
		}
		GF.setState(1, boardState.current().parent() != null
			&& boardState.current().parent().firstchild() != boardState.current());
		GF.setState(2, boardState.current().parent() != null
			&& boardState.current().parent().lastchild() != boardState.current());
		GF.setState(5, boardState.hasChildren());
		GF.setState(6, boardState.current().parent() != null);
		if (State != 9)
			GF.setState(State);
		else GF.setMarkState(SpecialMarker);
		int i, j;

		String sc = "";
		int let = 1;

		if ( !GF.getComment().equals(sc))
		{
			GF.setComment(sc);
		}
		if (Range >= 0 && !KeepRange) clearrange();
	}

	public void update (int i, int j)
	// update the field (i,j) in the offscreen image Active
	// in dependance of the board position boardPosition.
	// display the last move mark, if applicable.
	{
		if (ActiveImage == null) return;
		if (i < 0 || j < 0) return;
		Graphics g = ActiveImage.getGraphics();
		int xi = O + OTU + i * D;
		int xj = O + OTU + j * D;

		g.drawImage(Empty, xi, xj, xi + D, xj + D, xi, xj, xi + D, xj + D, this);

		if (boardPosition.color(i, j) != 0)
		{
			g.drawImage(EmptyShadow, xi - OP / 2, xj + OP / 2, xi + D
				- OP / 2, xj + D + OP / 2, xi - OP / 2, xj + OP / 2, xi
				+ D - OP / 2, xj + D + OP / 2, this);
		}
		else
		{
			g.drawImage(Empty, xi - OP / 2, xj + OP / 2, xi + D - OP
				/ 2, xj + D + OP / 2, xi - OP / 2, xj + OP / 2, xi + D
				- OP / 2, xj + D + OP / 2, this);
		}
		g.setClip(xi - OP / 2, xj + OP / 2, D, D);
		update1(g, i - 1, j);
		update1(g, i, j + 1);
		update1(g, i - 1, j + 1);
		g.setClip(xi, xj, D, D);
		if (i < S - 1 && boardPosition.color(i + 1, j) != 0)
		{
			g.drawImage(EmptyShadow, xi + D - OP / 2, xj + OP / 2, xi
				+ D, xj + D, xi + D - OP / 2, xj + OP / 2, xi + D, xj
				+ D, this);
		}
		if (j > 0 && boardPosition.color(i, j - 1) != 0)
		{
			g.drawImage(EmptyShadow, xi, xj, xi + D - OP / 2, xj + OP
				/ 2, xi, xj, xi + D - OP / 2, xj + OP / 2, this);
		}
		g.setClip(xi, xj, D, D);
		update1(g, i, j);
		g.dispose();
		repaint();
	}

	void update1 (Graphics g, int i, int j)
	{
		if (i < 0 || i >= S || j < 0 || j >= S) return;
		char c[] = new char[1];
		int n;
		int xi = O + OTU + i * D;
		int xj = O + OTU + j * D;
		if (boardPosition.color(i, j) > 0)
		{
			if (BlackStone != null)
			{
				g.drawImage(BlackStone, xi - 1, xj - 1, this);
			}
			else
			{
				g.setColor(BlackColor);
				g.fillOval(xi + 1, xj + 1, D - 2, D - 2);
				g.setColor(BlackSparkleColor);
				g.drawArc(xi + D / 2, xj + D / 4, D / 4, D / 4, 40, 50);
			}
		}
		else if (boardPosition.color(i, j) < 0)
		{
			if (WhiteStone != null)
			{
				g.drawImage(WhiteStone, xi - 1, xj - 1, this);
			}
			else
			{
				g.setColor(WhiteColor);
				g.fillOval(xi + 1, xj + 1, D - 2, D - 2);
				g.setColor(WhiteSparkleColor);
				g.drawArc(xi + D / 2, xj + D / 4, D / 4, D / 4, 40, 50);
			}
		}
		if (boardPosition.marker(i, j) != Field.Marker.NONE)
		{
			g.setColor(markerColor(boardPosition.color(i, j)));
			int h = D / 4;
			switch (boardPosition.marker(i, j))
			{
			case CIRCLE:
				g.drawOval(xi + D/2 - h, xj + D/2 - h, 2*h, 2*h);
				break;
			case CROSS:
				g.drawLine(xi + D/2 - h, xj + D/2 - h, xi + D/2 + h, xj + D/2 + h);
				g.drawLine(xi + D/2 + h, xj + D/2 - h, xi + D/2 - h, xj + D/2 + h);
				break;
			case TRIANGLE:
				g.drawLine(xi + D/2, xj + D/2 - h, xi + D/2 - h, xj + D/2 + h);
				g.drawLine(xi + D/2, xj + D/2 - h, xi + D/2 + h, xj + D/2 + h);
				g.drawLine(xi + D/2 - h, xj + D/2 + h, xi + D/2 + h, xj + D/2 + h);
				break;
			default:
				g.drawRect(xi + D/2 - h, xj + D/2 - h, 2*h, 2*h);
			}
		}
		if (boardPosition.letter(i, j) != 0)
		{
			g.setColor(labelColor(boardPosition.color(i, j)));
			c[0] = (char)('a' + boardPosition.letter(i, j) - 1);
			String hs = new String(c);
			int w = fontmetrics.stringWidth(hs) / 2;
			int h = fontmetrics.getAscent() / 2 - 1;
			g.setFont(font);
			g.drawString(hs, xi + D / 2 - w, xj + D / 2 + h);
		}
		else if (boardPosition.haslabel(i, j))
		{
			g.setColor(labelColor(boardPosition.color(i, j)));
			String hs = boardPosition.label(i, j);
			int w = fontmetrics.stringWidth(hs) / 2;
			int h = fontmetrics.getAscent() / 2 - 1;
			g.setFont(font);
			g.drawString(hs, xi + D / 2 - w, xj + D / 2 + h);
		}
		else if (boardPosition.tree(i, j) != null && !VHide)
		{
			g.setColor(Color.green);
			g.drawLine(xi + D / 2 - D / 6, xj + D / 2, xi + D / 2 + D / 6, xj
				+ D / 2);
			g.drawLine(xi + D / 2, xj + D / 2 - D / 6, xi + D / 2, xj + D / 2
				+ D / 6);
		}
		/*if (sendi == i && sendj == j)
		{
			g.setColor(Color.gray);
			g.drawLine(xi + D / 2 - 1, xj + D / 2, xi + D / 2 + 1, xj + D / 2);
			g.drawLine(xi + D / 2, xj + D / 2 - 1, xi + D / 2, xj + D / 2 + 1);
		}*/
		if (lasti == i && lastj == j && showlast)
		{
			if (GF.lastNumber() || Range >= 0 && boardPosition.number(i, j) > Range)
			{
				if (boardPosition.color(i, j) > 0)
					g.setColor(Color.white);
				else g.setColor(Color.black);
				String hs = "" + boardPosition.number(i, j) % 100;
				int w = fontmetrics.stringWidth(hs) / 2;
				int h = fontmetrics.getAscent() / 2 - 1;
				g.setFont(font);
				g.drawString(hs, xi + D / 2 - w, xj + D / 2 + h);
			}
			else
			{
				g.setColor(Color.red);
				g.drawLine(xi + D / 2 - D / 6, xj + D / 2, xi + D / 2 + D / 6,
					xj + D / 2);
				g.drawLine(xi + D / 2, xj + D / 2 - D / 6, xi + D / 2, xj + D
					/ 2 + D / 6);
			}
		}
		else if (boardPosition.color(i, j) != 0 && Range >= 0 && boardPosition.number(i, j) > Range)
		{
			if (boardPosition.color(i, j) > 0)
				g.setColor(Color.white);
			else g.setColor(Color.black);
			String hs = "" + boardPosition.number(i, j) % 100;
			int w = fontmetrics.stringWidth(hs) / 2;
			int h = fontmetrics.getAscent() / 2 - 1;
			g.setFont(font);
			g.drawString(hs, xi + D / 2 - w, xj + D / 2 + h);
		}
	}

	public void copy ()
	// copy the offscreen board to the screen
	{
		if (ActiveImage == null) return;
		try
		{
			getGraphics().drawImage(ActiveImage, 0, 0, this);
		}
		catch (Exception e)
		{}
	}

	public void undo ()
	// take back the last move, ask if necessary
	{ // System.out.println("undo");
		if (boardState.hasChildren() || boardState.hasParent()
			&& boardState.current().parent().lastchild() != boardState.current().parent().firstchild()
			&& boardState.current() == boardState.current().parent().firstchild())
		{
			if (GF.askUndo()) boardState.doundo(boardState.current());
		}
		else {
			boardState.doundo(boardState.current());
		}
	}

	public void resume ()
	// Resume playing after marking
	{
		State = 1;
		showinformation();
	}

	public void active (boolean f)
	{
		Active = f;
	}

	public int getboardsize ()
	{
		return S;
	}

	public int maincolor ()
	{
		return MainColor;
	}

	boolean valid (int i, int j)
	{
		return i >= 0 && i < S && j >= 0 && j < S;
	}

	public void clearrange ()
	{
		if (Range == -1) return;
		Range = -1;
		updateall();
		copy();
	}

	// *****************************************
	// Methods to be called from outside sources
	// *****************************************

	// ****** navigational things **************

	// methods to move in the game tree, including
	// update of the visible board:



	// ***** change the node at end of main tree ********
	// usually called by another board or server


	// ********** set board state ******************

	public void setblack ()
	// set black mode
	{
		State = 3;
		showinformation();
	}

	public void setwhite ()
	// set white mode
	{
		State = 4;
		showinformation();
	}

	public void black ()
	// black to play
	{
		State = 1;
		boardPosition.color(1);
		showinformation();
	}

	public void white ()
	// white to play
	{
		State = 2;
		boardPosition.color( -1);
		showinformation();
	}

	public void mark ()
	// marking
	{
		State = 5;
		showinformation();
	}

	void specialmark (Field.Marker i)
	// marking
	{
		State = 9;
		SpecialMarker = i;
		showinformation();
	}

	void textmark (String s)
	// marking
	{
		State = 10;
		TextMarker = s;
		showinformation();
	}

	public void letter ()
	// letter
	{
		State = 6;
		showinformation();
	}

	void deletestones ()
	// hide stones
	{
		State = 7;
		showinformation();
	}

	public boolean score ()
	// board state is removing groups
	{
		if (boardState.hasChildren()) {
			return false;
		}
		State = 8;
		Removing = true;
		showinformation();
		if (boardState.current().content().main())
			return true;
		else return false;
	}

	// ***************** several other things ******

	public void print (Frame f)
	// print the board
	{
		Position p = new Position(boardPosition);
		new Thread(new PrintBoard(p, Range, f)).start();
	}

	public void lastrange (int n)
	// set the range for stone numbers
	{
		int l = boardState.current().content().number() - 2;
		Range = l / n * n;
		if (Range < 0) Range = 0;
		KeepRange = true;
		updateall();
		copy();
		KeepRange = false;
	}

	public void handicap (int n)
	// set number of handicap points
	{
		int h = S < 13?3:4;
		if (n > 5)
		{
			boardState.setblack(h - 1, S / 2);
			boardState.setblack(S - h, S / 2);
		}
		if (n > 7)
		{
			boardState.setblack(S / 2, h - 1);
			boardState.setblack(S / 2, S - h);
		}
		switch (n)
		{
			case 9:
			case 7:
			case 5:
				boardState.setblack(S / 2, S / 2);
			case 8:
			case 6:
			case 4:
				boardState.setblack(S - h, S - h);
			case 3:
				boardState.setblack(h - 1, h - 1);
			case 2:
				boardState.setblack(h - 1, S - h);
			case 1:
				boardState.setblack(S - h, h - 1);
		}
		MainColor = -1;
	}

	public void updateall ()
	// update all of the board
	{
		if (ActiveImage == null) return;
		synchronized (this)
		{
			ActiveImage.getGraphics().drawImage(Empty, 0, 0, this);
		}
		int i, j;
		for (i = 0; i < S; i++)
			for (j = 0; j < S; j++)
				update(i, j);
		showinformation();
	}

	public void updateboard ()
	// redraw the board and its background
	{
		BlackStone = WhiteStone = null;
		EmptyShadow = null;
		setfonts();
		makeimages();
		updateall();
		copy();
	}

	Image getBoardImage ()
	{
		return ActiveImage;
	}

	Dimension getBoardImageSize ()
	{
		return new Dimension(ActiveImage.getWidth(this), ActiveImage
			.getHeight(this));
	}

	// *****************************************************
	// procedures that might be overloaded for more control
	// (Callback to server etc.)
	// *****************************************************

	void movemouse (int i, int j)
	// set a move at i,j
	{
		if (boardState.hasChildren()) return;
		if (captured == 1 && capturei == i && capturej == j
			&& GF.getParameter("preventko", true)) return;
		boardState.set(i, j); // try to set a new move
	}

	void setmouse (int i, int j, int c)
	// set a stone at i,j with specified color
	{
		boardState.set(i, j, c);
		showinformation();
	}

	void setmousec (int i, int j, int c)
	// set a stone at i,j with specified color
	{
		boardState.setc(i, j, c);
		showinformation();
	}

	void deletemouse (int i, int j)
	// delete a stone at i,j
	{
		deletemousec(i, j);
	}

	void deletemousec (int i, int j)
	// delete a stone at i,j
	{
		boardState.delete(i, j);
		showinformation();
	}

	void setVariationStyle (boolean hide, boolean current)
	{
		boardPosition.undonode(boardState.current().content());
		VHide = hide;
		VCurrent = current;
		boardPosition.act(boardState.current().content());
		updateall();
		copy();
	}

	@Override
	public void positionUpdated(int i, int j) {
		update(i, j);
	}

	@Override
	public void allPositionsUpdated() {
		updateall();
	}
}
