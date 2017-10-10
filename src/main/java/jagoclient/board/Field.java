package jagoclient.board;

import rene.util.list.Tree;

import java.util.Objects;

// ************** Field **************

/**
A class to hold a single field in the game board.
Contains data for labels, numbers, marks etc. and
of course the color of the stone on the board.
<boardPosition>
It may contain a reference to a tree, which is a variation
starting at this (empty) board position.
<boardPosition>
The Mark field is used for several purposes, like marking
a group of stones or a territory.
*/

public class Field
{	int C; // the state of the field (-1,0,1), 1 is black
	boolean Mark; // for several purposes (see Position.markgroup)
	Tree<Node> T; // Tree that starts this variation
	int Letter; // Letter to be displayed
	String LabelLetter; // Strings from the LB tag.
	boolean HaveLabel; // flag to indicate there is a label
	int Territory; // For Territory counting
	public static enum Marker
	{
		NONE(null),
		CROSS(Action.Type.CROSS),
		SQUARE(Action.Type.SQUARE),
		TRIANGLE(Action.Type.TRIANGLE),
		CIRCLE(Action.Type.CIRCLE);
		final Action.Type value;
		Marker(Action.Type value) { this.value = value; }
	};
	Marker marker; // emphasized field
	int Number;

	//** set the field to 0 initially */
	public Field ()
	{	C=0;
		T=null;
		Letter=0;
		HaveLabel=false;
		Number=0;
		marker = Marker.NONE;
	}

	public Field(Field f) {
		C = f.C;
		T = f.T;
		Letter = f.Letter;
		HaveLabel = f.HaveLabel;
		Number = f.Number;
		marker = f.marker;
		LabelLetter = f.LabelLetter;
	}

	/** return its state */
	int color ()
	{	return C;
	}

	/** set its state */
	void color (int c)
	{	C=c;
		Number=0;
	}
	
	final static int az='z'-'a';

	/** return a string containing the coordinates in SGF */
	static String string (int i, int j)
	{	char[] a=new char[2];
		if (i>=az) a[0]=(char)('A'+i-az-1);
		else a[0]=(char)('a'+i); 
		if (j>=az) a[1]=(char)('A'+j-az-1);
		else a[1]=(char)('a'+j);
		return new String(a);
	}

	/** return a string containing the coordinates in SGF */
	static String coordinate (int i, int j, int s)
	{	if (s>25)
		{	return (i+1)+","+(s-j);
		}
		else
		{	if (i>=8) i++;
			return ""+(char)('A'+i)+(s-j);
		}
	}

	/** get the first coordinate from the SGF string */
	static int i (String s)
	{	if (s.length()<2) return -1;
		char c=s.charAt(0);
		if (c<'a')  return c-'A'+az+1;
		return c-'a';
	}

	/** get the second coordinate from the SGF string */
	static int j (String s)
	{	if (s.length()<2) return -1;
		char c=s.charAt(1);
		if (c<'a')  return c-'A'+az+1;
		return c-'a';
	}

	// modifiers:
	void mark (boolean f) { Mark=f; } // set Mark
	void tree (Tree<Node> t) { T=t; } // set Tree
	void marker (Marker f) { marker=f; }
	void letter (int l) { Letter=l; }
	void territory (int c) { Territory=c; }
	void setlabel (String s) { HaveLabel=true; LabelLetter=s; }
	void clearlabel () { HaveLabel=false; }
	void number (int n) { Number=n; }

	// access functions:
	boolean mark () { return Mark; } // ask Mark
	Marker marker () { return marker; }
	Tree<Node> tree () { return T; }
	int letter () { return Letter; }
	int territory () { return Territory; }
	boolean havelabel () { return HaveLabel; }
	String label () { return LabelLetter; }
	int number() { return Number; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		return C == field.C &&
				Letter == field.Letter &&
				HaveLabel == field.HaveLabel &&
				Territory == field.Territory &&
				Number == field.Number &&
				Objects.equals(T, field.T) &&
				Objects.equals(LabelLetter, field.LabelLetter) &&
				marker == field.marker;
	}

	@Override
	public int hashCode() {
		return Objects.hash(C, T, Letter, LabelLetter, HaveLabel, Territory, marker, Number);
	}
}
