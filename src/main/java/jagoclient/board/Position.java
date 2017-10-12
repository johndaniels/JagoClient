package jagoclient.board;

import rene.util.list.Tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * Store a complete game position. This class contains all the information
 * necessary to render the current state of the board, including stones,
 * marks, and variations.
 * Also keeps track of score and game information.
 * Contains methods for group determination and liberties.
*/
public class Position
{	int S; // size (9,11,13 or 19)
	int C; // next turn (1 is black, -1 is white)
	Field[][] F; // the board
	private int lasti = -1, lastj = 0; // last move (used to highlight the move)
	int Pw,Pb; // total prisioners captured
	private int captured = 0, capturei, capturej;
	int number;
	int let;
	String currentComment;

	List<Tree<Node>> nodes = new ArrayList<>(); // A list of the nodes that got us to this point.
	
	/** 
	Initialize F with an empty board, and set the next turn to black.
	*/
	public Position (int size)
	{
		S=size;
		F=new Field[S][S];
		int i,j;
		for (i=0; i<S; i++)
			for (j=0; j<S; j++)
				F[i][j]=new Field();
		C=1;
	}

	/** 
	Initialize F with an empty board, and set
	the next turn to black.
	*/
	public Position (Position P)
	{
		S=P.S;
		F=new Field[S][S];
		int i,j;
		for (i=0; i<S; i++)
			for (j=0; j<S; j++)
			{	F[i][j]=new Field();
			}
		for (i=0; i<S; i++)
			for (j=0; j<S; j++)
			{	color(i,j,P.color(i,j));
			    number(i,j,P.number(i,j));
			    marker(i,j,P.marker(i,j));
			    letter(i,j,P.letter(i,j));
			    if (P.haslabel(i,j)) setlabel(i,j,P.label(i,j));
			}
		color(P.color());
	}

	// Interface routines to set or ask a field:
	int color (int i, int j)
	{	return F[i][j].color();
	}
	void color (int i, int j, int c)
	{	F[i][j].color(c);
	}
	void number (int i, int j, int n) { F[i][j].number(n); }
	int number (int i, int j) { return F[i][j].number(); }
	int color () { return C; }
	void color (int c) { C=c; }

	/**
	Recursively mark all unmarked places with state c,
	starting from (i,j).
	*/
	private void markrek (int i, int j, int c)
	{	if (F[i][j].mark() || F[i][j].color()!=c) return;
		F[i][j].mark(true);
		if (i>0) markrek(i-1,j,c);
		if (j>0) markrek(i,j-1,c);
		if (i<S-1) markrek(i+1,j,c);
		if (j<S-1) markrek(i,j+1,c);		
	}
	
	/**
	Mark a group at (n,m)
	*/
	private void markgroup (int n, int m)
	{	unmarkall();
		// recursively do the marking
		markrek(n,m,F[n][m].color());
	}
	
	/**
	Recursively mark a group of state c
	starting from (i,j) with the main goal to
	determine, if there is a neighbor of state ct to
	this group.
	If yes abandon the mark and return true.
	*/
	boolean markrektest (int i, int j, int c, int ct)
	{	if (F[i][j].mark()) return false;
	 	if (F[i][j].color()!=c)
	 	{	if (F[i][j].color()==ct) return true;
	 		else return false;
	 	}
		F[i][j].mark(true);
		if (i>0) { if (markrektest(i-1,j,c,ct)) return true; }
		if (j>0) { if (markrektest(i,j-1,c,ct)) return true; }
		if (i<S-1) { if (markrektest(i+1,j,c,ct)) return true; }
		if (j<S-1) { if (markrektest(i,j+1,c,ct)) return true; }
		return false;
	}
	
	/**
	Test if the group at (n,m) has a neighbor of state ct.
	If yes, mark all elements of the group.
	Else return false.
	*/
	private boolean markgrouptest (int n, int m, int ct)
	{	unmarkall();
		return markrektest(n,m,F[n][m].color(),ct);
	}

	/** cancel all markings
	*/
	private void unmarkall ()
	{	int i,j;
		for (i=0; i<S; i++)
			for (j=0; j<S; j++)
			{	F[i][j].mark(false);
			}
	}
	
	/** mark and count
	*/
	public int count (int i, int j)
	{	unmarkall();
		markgroup(i,j);
		int count=0;
		for (i=0; i<S; i++)
			for (j=0; j<S; j++)
				if (F[i][j].mark()) count++;
		return count;
	}

	/**
	Find all B and W territory.
	Sets the territory flags to 0, 1 or -1.
	-2 is an intermediate state for unchecked points.
	*/
	public void getterritory ()
	{	int i,j,ii,jj;
		for (i=0; i<S; i++)
			for (j=0; j<S; j++)
			{	F[i][j].territory(-2);
			}
		for (i=0; i<S; i++)
			for (j=0; j<S; j++)
			{	if (F[i][j].color()==0)
				{	if (F[i][j].territory()==-2)
					{	if (!markgrouptest(i,j,1))
						{	for (ii=0; ii<S; ii++)
								for (jj=0; jj<S; jj++)
								{	if (F[ii][jj].mark())
										F[ii][jj].territory(-1);
								}
						}
						else if (!markgrouptest(i,j,-1))
						{	for (ii=0; ii<S; ii++)
								for (jj=0; jj<S; jj++)
								{	if (F[ii][jj].mark())
										F[ii][jj].territory(1);
								}
						}
						else
						{	markgroup(i,j);
							for (ii=0; ii<S; ii++)
								for (jj=0; jj<S; jj++)
								{	if (F[ii][jj].mark())
										F[ii][jj].territory(0);
								}
						}
					}
				}
			}
		
	}

	void setcurrent (Tree<Node> treeNode)
	// update the last move marker applying all
	// set move actions in the node
	{
		Node n = treeNode.content();
		String s;
		for (Action a : n.actions())
		{
			if (a.type().equals(Action.Type.BLACK) || a.type().equals(Action.Type.WHITE))
			{
				s = a.argument();
				int i = Field.i(s);
				int j = Field.j(s);
				if (valid(i, j))
				{
					lasti = i;
					lastj = j;
					color( -color(i, j));
				}
			}
			if (a.type().equals(Action.Type.COMMENT))
			{
				currentComment = a.argument();
			}
			else if (a.type().equals(Field.Marker.SQUARE.value) || a.type().equals(Action.Type.SELECT))
			{
				runForAllCoordinates(a, (i1, j1) -> marker(i1, j1, Field.Marker.SQUARE));
			}
			else if (a.type().equals(Field.Marker.CROSS.value) || a.type().equals(Action.Type.MARK)
					|| a.type().equals(Action.Type.WHITE_TERRITORY) || a.type().equals(Action.Type.BLACK_TERRITORY))
			{
				runForAllCoordinates(a, (i1, j1) -> marker(i1, j1, Field.Marker.CROSS));
			}
			else if (a.type().equals(Field.Marker.TRIANGLE.value))
			{
				runForAllCoordinates(a, (i1, j1) -> marker(i1, j1, Field.Marker.TRIANGLE));
			}
			else if (a.type().equals(Field.Marker.CIRCLE.value))
			{
				runForAllCoordinates(a, (i1, j1) -> marker(i1, j1, Field.Marker.CIRCLE));
			}
			else if (a.type().equals(Action.Type.LABEL))
			{
				runForAllCoordinates(a, (i1, j1) -> {
					letter(i1, j1, let);
					let++;
				});
			}
			else if (a.type().equals(Action.Type.LABEL))
			{
				for (String argString : a.arguments())
				{
					int i = Field.i(argString);
					int j = Field.j(argString);
					if (valid(i, j) && argString.length() >= 4 && argString.charAt(2) == ':')
					{
						setlabel(i, j, argString.substring(3));
					}
				}
			}
		}

		LinkedList<Tree<Node>> nodes = treeNode.children();
		for (Tree<Node> p : nodes)
		{
			for (Action a : p.content().actions())
			{
				if (a.type().equals(Action.Type.WHITE) || a.type().equals(Action.Type.BLACK))
				{
					String argString = a.argument();
					int i = Field.i(argString);
					int j = Field.j(argString);
					if (valid(i, j))
					{
						tree(i, j, p);
					}
					break;
				}
			}

		}
		number = n.number();
	}

	void setaction (Node n, Action a, int c)
	// interpret a set move action, update the last move marker,
	// c being the color of the move.
	{
		String s = a.argument();
		int i = Field.i(s);
		int j = Field.j(s);
		if ( !valid(i, j)) return;
		n.addchange(new Change(i, j, color(i, j), number(i, j)));
		color(i, j, c);
		number(i, j, n.number() - 1);
		lasti = i;
		lastj = j;
		color( -c);
		capture(i, j, n);
	}

	public void capture (int i, int j, Node n)
	// capture neighboring groups without liberties
	// capture own group on suicide
	{
		int c = -color(i, j);
		captured = 0;
		if (i > 0) capturegroup(i - 1, j, c, n);
		if (j > 0) capturegroup(i, j - 1, c, n);
		if (i < S - 1) capturegroup(i + 1, j, c, n);
		if (j < S - 1) capturegroup(i, j + 1, c, n);
		if (color(i, j) == -c)
		{
			capturegroup(i, j, -c, n);
		}
		if (captured == 1 && count(i, j) != 1) captured = 0;
	}

	/**
	 * Used by capture to determine the state of the groupt at (i,j)
	 * Remove it, if it has no liberties and note the removals
	 * as actions in the current node.
	 */
	private void capturegroup (int i, int j, int c, Node n)
	// Used by capture to determine the state of the groupt at (i,j)
	// Remove it, if it has no liberties and note the removals
	// as actions in the current node.
	{
		int ii, jj;
		Action a;
		if (color(i, j) != c) return;
		if ( !markgrouptest(i, j, 0)) // liberties?
		{
			for (ii = 0; ii < S; ii++)
				for (jj = 0; jj < S; jj++)
				{
					if (marked(ii, jj))
					{
						n.addchange(new Change(ii, jj, color(ii, jj), number(ii, jj)));
						if (color(ii, jj) > 0)
						{
							Pb++;
							n.Pb++;
						}
						else
						{
							Pw++;
							n.Pw++;
						}
						color(ii, jj, 0);
						captured++;
						capturei = ii;
						capturej = jj;
					}
				}
		}
	}

	public void undonode (Node n)
	// Undo everything that has been changed in the node.
	// (This will not correct the last move marker!)
	{
		Iterator<Change> i = n.changes().descendingIterator();
		while (i.hasNext())
		{
			Change c = i.next();
			color(c.I, c.J, c.C);
			number(c.I, c.J, c.N);
		}
		n.clearchanges();
		Pw -= n.Pw;
		Pb -= n.Pb;
	}

	private interface TwoIntFunc {
		void run(int i, int k);
	}

	void runForAllCoordinates(Action action, TwoIntFunc func) {
		for (String s : action.arguments()) {
			runForCoordinates(s, func);
		}
	}

	void runForCoordinates(String argument, TwoIntFunc func) {
		int i = Field.i(argument);
		int j = Field.j(argument);
		if (valid(i, j)) {
			func.run(i, j);
		}
	}

	public void act (Node n)
	// interpret all actions of a node
	{
		if (n.actions().isEmpty()) return;
		String s;
		int i, j;
		n.clearchanges();
		n.Pw = n.Pb = 0;

		// delete all marks and variations
		for (i = 0; i < S; i++) {
			for (j = 0; j < S; j++) {
				if (tree(i, j) != null) {
					tree(i, j, null);
				}
				if (marker(i, j) != Field.Marker.NONE) {
					marker(i, j, Field.Marker.NONE);
				}
				if (letter(i, j) != 0) {
					letter(i, j, 0);
				}
				if (haslabel(i, j)) {
					clearlabel(i, j);
				}
			}
		}

		for (Action a : n.actions()) {
			if (a.type().equals(Action.Type.BLACK)) {
				setaction(n, a, 1);
			} else if (a.type().equals(Action.Type.WHITE)) {
				setaction(n, a, -1);
			}
			if (a.type().equals(Action.Type.ADD_BLACK)) {
				placeaction(n, a, 1);
			}
			if (a.type().equals(Action.Type.ADD_WHITE)) {
				placeaction(n, a, -1);
			} else if (a.type().equals(Action.Type.ADD_EMPTY)) {
				emptyaction(n, a);
			}
		}
	}

	private void placeaction (Node n, Action a, int c)
	// interpret a set move action, update the last move marker,
	// c being the color of the move.
	{
		int i, j;
		for (String s : a.arguments())
		{
			i = Field.i(s);
			j = Field.j(s);
			if (valid(i, j))
			{
				n.addchange(new Change(i, j, color(i, j), number(i, j)));
				color(i, j, c);
			}
		}
	}

	private void emptyaction (Node n, Action a)
	// interpret a remove stone action
	{
		int i, j, r = 1;
		for (String s : a.arguments())
		{
			i = Field.i(s);
			j = Field.j(s);
			if (valid(i, j))
			{
				n.addchange(new Change(i, j, color(i, j), number(i, j)));
				if (color(i, j) < 0)
				{
					n.Pw++;
					Pw++;
				}
				else if (color(i, j) > 0)
				{
					n.Pb++;
					Pb++;
				}
				color(i, j, 0);
			}
		}
	}

	private boolean valid (int i, int j)
	{
		return i >= 0 && i < S && j >= 0 && j < S;
	}

	// Interface to determine field marks.
	boolean marked (int i, int j) { return F[i][j].mark(); }
	Field.Marker marker (int i, int j) { return F[i][j].marker(); }
	void marker (int i, int j, Field.Marker f) { F[i][j].marker(f); }
	void letter (int i, int j, int l) { F[i][j].letter(l); }
	int letter (int i, int j) { return F[i][j].letter(); }
	int territory (int i, int j) { return F[i][j].territory(); }
	boolean haslabel (int i, int j) { return F[i][j].havelabel(); }
	String label (int i, int j) { return F[i][j].label(); }
	void setlabel (int i, int j, String s) { F[i][j].setlabel(s); }
	void clearlabel (int i, int j) { F[i][j].clearlabel(); }

	// Interfact to variation trees
	Tree<Node> tree (int i, int j) { return F[i][j].tree(); }
	void tree (int i, int j, Tree<Node> t) { F[i][j].tree(t); }

	public String getCurrentComment() {
		return currentComment;
	}
}
