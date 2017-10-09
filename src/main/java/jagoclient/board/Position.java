package jagoclient.board;

import rene.util.list.Tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Store a complete game position. This represents the current
 * state of the game board at a particular Node. the position
 * can be mutated by applying or undoing nodes.
 * Keeps track of score and other things
 * Contains methods for group determination and liberties.
*/
public class Position
{	int S; // size (9,11,13 or 19)
	int C; // next turn (1 is black, -1 is white)
	Field[][] F; // the board
	List<PositionUpdatedHandler> positionUpdatedHandlers = new ArrayList<>();
	private int lasti = -1, lastj = 0; // last move (used to highlight the move)
	int Pw,Pb; // total prisioners captured
	private int captured = 0, capturei, capturej;
	int number;



	public interface PositionUpdatedHandler {
		void positionUpdated(int i, int j);
		void allPositionsUpdated();
	}
	
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

	public void addPositionUpdatedHandler(PositionUpdatedHandler handler) {
		positionUpdatedHandlers.add(handler);
	}

	void update(int i, int j) {
		for (PositionUpdatedHandler handler : positionUpdatedHandlers) {
			handler.positionUpdated(i, j);
		}
	}

	void updateall() {
		for (PositionUpdatedHandler handler : positionUpdatedHandlers) {
			handler.allPositionsUpdated();
		}
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
	void markrek (int i, int j, int c)
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
	public void markgroup (int n, int m)
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
	public boolean markgrouptest (int n, int m, int ct)
	{	unmarkall();
		return markrektest(n,m,F[n][m].color(),ct);
	}

	/** cancel all markings
	*/
	public void unmarkall ()
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

	void setcurrent (Node n)
	// update the last move marker applying all
	// set move actions in the node
	{
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
					update(lasti, lastj);
					color( -color(i, j));
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
		update(lasti, lastj);
		lasti = i;
		lastj = j;
		update(i, j);
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
	public void capturegroup (int i, int j, int c, Node n)
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
						update(ii, jj); // redraw the field (offscreen)
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
			update(c.I, c.J);
		}
		n.clearchanges();
		Pw -= n.Pw;
		Pb -= n.Pb;
	}

	public void act (Node n)
	// interpret all actions of a node
	{
		if (n.actions().isEmpty()) return;
		String s;
		int i, j;
		n.clearchanges();
		n.Pw = n.Pb = 0;
		for (Action a : n.actions())
		{
			if (a.type().equals(Action.Type.BLACK))
			{
				setaction(n, a, 1);
			}
			else if (a.type().equals(Action.Type.WHITE))
			{
				setaction(n, a, -1);
			}
			if (a.type().equals(Action.Type.ADD_BLACK))
			{
				placeaction(n, a, 1);
			}
			if (a.type().equals(Action.Type.ADD_WHITE))
			{
				placeaction(n, a, -1);
			}
			else if (a.type().equals(Action.Type.ADD_EMPTY))
			{
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
				update(i, j);
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
				update(i, j);
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
}
