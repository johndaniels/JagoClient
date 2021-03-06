package jagoclient.board;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import rene.util.list.Tree;
import rene.util.parser.StringParser;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlReaderException;
import rene.util.xml.XmlTag;
import rene.util.xml.XmlTagPI;
import rene.util.xml.XmlTagText;
import rene.util.xml.XmlTree;
import rene.util.xml.XmlWriter;

/**
This is a class wich contains a Tree<Node>. It used to store complete
game trees.
@see jagoclient.board.Tree<Node>
*/

public class SGFTree
{	protected Tree<Node> History; // the game history

	/** initlialize with a specific Node */
	public SGFTree (Node n)
	{	History=new Tree<Node>(n);
		History.content().main(true);
	}
	
	/** return the top node of this game tree */
	public Tree<Node> top () { return History; }

	final int maxbuffer=4096;
	char[] Buffer=new char[maxbuffer]; // the buffer for reading of files
	int BufferN;

	char readnext (BufferedReader in) throws IOException
	{	int c=readchar(in);
		while (c=='\n' || c=='\t' || c==' ')
		{	if (c==-1) throw new IOException();
			c=readchar(in);
		}
		return (char)c;
	}

	static int lastnl=0;
	
	char readchar (BufferedReader in) throws IOException
	{	int c;
		while (true)
		{	c=in.read();
			if (c==-1) throw new IOException();
			if (c==13)
			{	if (lastnl==10) lastnl=0;
				else
				{	lastnl=13; return '\n';
				}
			}
			else if (c==10)
			{	if (lastnl==13) lastnl=0;
				else
				{	lastnl=10; return '\n';
				}
			}
			else
			{	lastnl=0;
				return (char)c;
			}
		}
	}

	// read a node assuming that ; has been found
	// return the character, which did not fit into node properties,
	// usually ;, ( or )
	char readnode (Tree<Node> p, BufferedReader in) throws IOException
	{	boolean sgf=false;
		char c=readnext(in);
		Action a;
		Node n=new Node(p.content().number());
		Action.Type s;
		loop: while (true) // read all actions
		{	BufferN=0;
			while (true)
			{	if (c>='A' && c<='Z') store(c);
					// note only capital letters
				else if (c=='(' || c==';' || c==')') break loop;
					// last property reached
					// BufferN should be 0 then
				else if (c=='[') break;
					// end of porperty type, arguments starting
				else if (c<'a' || c>'z') throw new IOException();
					// this is an error
				c=readnext(in);
			}
			if (BufferN==0) throw new IOException();
			s = Action.Type.fromString(new String(Buffer,0,BufferN));
			if (s.equals(Action.Type.LABEL)) a=new LabelAction();
			else if (s.equals(Action.Type.MARK)) a=new MarkAction();
			else a=new Action(s);
			while (c=='[')
			{	BufferN=0;
				while (true)
				{	c=readchar(in);
					if (c=='\\')
					{	c=readchar(in);
						if (sgf && c=='\n')
						{	if (BufferN>1 && Buffer[BufferN-1]==' ') continue;
							else c=' ';
						}
					}
					else if (c==']') break;
					store(c);
				}
				c=readnext(in); // prepare next argument
				String s1;
				if (BufferN>0) s1=new String(Buffer,0,BufferN);
				else s1="";
				if (!expand(a,s1)) a.addargument(s1);
			}
			// no more arguments
			n.addaction(a);
			if (a.type().equals(Action.Type.BLACK) || a.type().equals(Action.Type.WHITE))
			{	n.number(n.number()+1);
			}
		} // end of actions has been found
		// append node
		n.main(p);
		Tree<Node> newp;
		if (p.content().actions()==null)
			p.content(n);
		else
		{	p.addchild(newp=new Tree<Node>(n));
			n.main(p);
			p=newp;
			if (p.parent()!=null && p!=p.parent().firstchild())
				p.content().number(2);
		}
		return c;
	}

	/**
	Store c into the Buffer extending its length, if necessary.
	This is a fix by Bogdar Creanga from 2000-10-17 (Many Thanks)
	*/
	private void store (char c)
	{	try
		{	Buffer[BufferN]=c;
			BufferN++;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{	int newLength = Buffer.length + maxbuffer;
			char[] newBuffer = new char[newLength];
			System.arraycopy(Buffer,0,newBuffer,0,Buffer.length);
			Buffer = newBuffer;
			Buffer[BufferN++]=c;
		}
	}

	// Check for the terrible compressed point list and expand into
	// single points
	private boolean expand (Action a, String s)
	{	Action.Type t=a.type();
		if (!(t.equals(Field.Marker.CROSS.value) || t.equals(Field.Marker.SQUARE.value) ||
			 t.equals(Field.Marker.TRIANGLE.value) || t.equals(Field.Marker.CIRCLE.value) ||
			 t.equals(Action.Type.ADD_WHITE) || t.equals(Action.Type.ADD_BLACK) || t.equals(Action.Type.ADD_EMPTY) || t.equals(Action.Type.SELECT))) return false;
		if (s.length()!=5 || s.charAt(2)!=':') return false;
		String s0=s.substring(0,2),s1=s.substring(3);
		int i0=Field.i(s0),j0=Field.j(s0);
		int i1=Field.i(s1),j1=Field.j(s1);
		if (i1<i0 || j1<j0) return false;
		int i,j;
		for (i=i0; i<=i1; i++)
			for (j=j0; j<=j1; j++)
			{	a.addargument(Field.string(i,j));
			}
		return true;
	}

	/**
	Read the nodes belonging to a tree.
	this assumes that ( has been found.
	*/
	void readnodes (Tree<Node> p, BufferedReader in)
		throws IOException
	{	char c=readnext(in);
		while (true)
		{	if (c==';')
			{	c=readnode(p,in);
				if (p.haschildren()) p=p.lastchild();
				continue;
			}
			else if (c=='(')
			{	readnodes(p,in);
			}
			else if (c==')') break;
			c=readnext(in);
		}
	}
	
	/**
	Read the tree from an BufferedReader in SGF format.
	The BoardInterfaces is only used to determine the "sgfcomments" parameter.
	*/	
	public static Vector load (BufferedReader in)
		throws IOException
	{	Vector v=new Vector();
		boolean linestart=true;
		int c;
		reading : while (true)
		{	SGFTree T=new SGFTree(new Node(1));
			while (true) // search for ( at line start
			{	try
				{	c=T.readchar(in);
				}
				catch (IOException ex)
				{	break reading;
				}
				if (linestart && c=='(') break;
				if (c=='\n') linestart=true;
				else linestart=false;
			}
			T.readnodes(T.History,in); // read the nodes
			v.addElement(T);
		}
		return v;
	}

	/*
	XML Reader Stuff
	*/
	
	// Assumption on Boardsize of xml file, if <BoardSize> is not found
	static int BoardSize=19;	
	static String GameName="";
	
	/**
	Read all games from a tree.
	@return Vector of trees.
	*/
	static Vector readnodes (XmlTree tree)
		throws XmlReaderException
	{	Vector v=new Vector();
		Enumeration root=tree.getContent();
		while (root.hasMoreElements())
		{	tree=(XmlTree)root.nextElement();	
			XmlTag tag=tree.getTag();
			if (tag instanceof XmlTagPI) continue;
			testTag(tag,"Go");
			Enumeration trees=tree.getContent();
			while (trees.hasMoreElements())
			{	tree=(XmlTree)trees.nextElement();
				tag=tree.getTag();
				testTag(tag,"GoGame");
				if (tag.hasParam("name"))
				{	GameName=tag.getValue("name");
				}
				Enumeration e=tree.getContent();
				if (!e.hasMoreElements()) xmlMissing("Information");
				XmlTree information=(XmlTree)e.nextElement();
				testTag(information.getTag(),"Information");
				getBoardSize(information);
				SGFTree t=new SGFTree(new Node(1));
				Tree<Node> p=t.readnodes(e,null,tree,true,1);
				if (p!=null) setInformation(p,information);
				t.History=p;
				if (p!=null) v.addElement(t);
			}
		}
		return v;
	}
	
	public static void setInformation (Tree<Node> p, XmlTree information)
		throws XmlReaderException
	{	Enumeration e=information.getContent();
		while (e.hasMoreElements())
		{	XmlTree tree=(XmlTree)e.nextElement();
			XmlTag tag=tree.getTag();
			if (tag.name().equals("BoardSize"))
			{	p.content().addaction(new Action(Action.Type.SIZE,""+BoardSize));
			}
			else if (tag.name().equals("BlackPlayer"))
			{	p.content().addaction(new Action(Action.Type.BLACK_PLAYER_NAME,getText(tree)));
			}
			else if (tag.name().equals("BlackRank"))
			{	p.content().addaction(new Action(Action.Type.BLACK_PLAYER_RANK,getText(tree)));
			}
			else if (tag.name().equals("WhitePlayer"))
			{	p.content().addaction(new Action(Action.Type.WHITE_PLAYER_NAME,getText(tree)));
			}
			else if (tag.name().equals("WhiteRank"))
			{	p.content().addaction(new Action(Action.Type.WHITE_PLAYER_RANK,getText(tree)));
			}
			else if (tag.name().equals("Date"))
			{	p.content().addaction(new Action(Action.Type.DATE,getText(tree)));
			}
			else if (tag.name().equals("Time"))
			{	p.content().addaction(new Action(Action.Type.TIME,getText(tree)));
			}
			else if (tag.name().equals("Komi"))
			{	p.content().addaction(new Action(Action.Type.KOMI,getText(tree)));
			}
			else if (tag.name().equals("Result"))
			{	p.content().addaction(new Action(Action.Type.RESULT,getText(tree)));
			}
			else if (tag.name().equals("Handicap"))
			{	p.content().addaction(new Action(Action.Type.HANDICAP,getText(tree)));
			}
			else if (tag.name().equals("User"))
			{	p.content().addaction(new Action(Action.Type.USER,getText(tree)));
			}
			else if (tag.name().equals("Copyright"))
			{	p.content().addaction(new Action(Action.Type.COPYRIGHT,parseComment(tree)));
			}
		}
		if (!GameName.equals(""))
			p.content().addaction(new Action(Action.Type.GAME_NAME,GameName));
	}
	
	public static String getText (XmlTree tree)
		throws XmlReaderException
	{	Enumeration e=tree.getContent();
		if (!e.hasMoreElements()) return "";
		XmlTree t=(XmlTree)e.nextElement();
		XmlTag tag=t.getTag();
		if (!(tag instanceof XmlTagText) || e.hasMoreElements())
			throw new XmlReaderException(
				"<"+tree.getTag().name()+"> has wrong content.");
		return ((XmlTagText)tag).getContent();
	}
	
	public static void getBoardSize (XmlTree tree)
		throws XmlReaderException
	{	Enumeration e=tree.getContent();
		BoardSize=19;
		while (e.hasMoreElements())
		{	tree=(XmlTree)e.nextElement();
			if (tree.getTag().name().equals("BoardSize"))
			{	tree=(XmlTree)tree.xmlFirstContent();
				XmlTag tag=tree.getTag();
				if (tag instanceof XmlTagText)
				{	try
					{	BoardSize=Integer.parseInt(
							((XmlTagText)tag).getContent());
					}
					catch (Exception ex)
					{	throw new XmlReaderException(
							"Illegal <BoardSize>");
					}
				}
				else
					throw new XmlReaderException(
							"Illegal <BoardSize>");
				break;
			}
		}
	}
	
	Tree<Node> readnodes (Enumeration e, Tree<Node> p, XmlTree father, boolean main,
		int number)
		throws XmlReaderException
	{	Tree<Node> ret=null;
		while (e.hasMoreElements())
		{	XmlTree tree=(XmlTree)e.nextElement();
			XmlTag tag=tree.getTag();
			if (tag.name().equals("Nodes"))
			{	return readnodes(tree.getContent(),p,father,main,number);
			}
			else if (tag.name().equals("Node"))
			{	if (p!=null) number=p.content().number();
				Node n=readnode(number,tree);
				n.main(main);
				Tree<Node> newp=new Tree<Node>(n);
				if (p==null) ret=newp;
				if (p!=null) p.addchild(newp);
				p=newp;
			}
			else if (tag.name().equals("White"))
			{	if (p!=null) number=p.content().number();
				Node n=new Node(number);
				try
				{	n.addaction(new Action(Action.Type.WHITE,xmlToSgf(tree)));
					n.number(n.number()+1);
					n.main(main);
				}
				catch (XmlReaderException ey) { n.addaction(new Action(Action.Type.COMMENT,"Pass")); }
				if (tag.hasParam("name"))
				{	n.addaction(new Action(Action.Type.NAME,tag.getValue("name")));
				}
				if (tag.hasParam("timeleft"))
				{	n.addaction(new Action(Action.Type.WHITE_TIME,tag.getValue("timeleft")));
				}
				Tree<Node> newp=new Tree<Node>(n);
				if (p==null) ret=newp;
				if (p!=null) p.addchild(newp);
				p=newp;
			}
			else if (tag.name().equals("Black"))
			{	if (p!=null) number=p.content().number();
				Node n=new Node(number);
				try
				{	n.addaction(new Action(Action.Type.BLACK,xmlToSgf(tree)));
					n.number(n.number()+1);
					n.main(main);
				}
				catch (XmlReaderException ey) { n.addaction(new Action(Action.Type.COMMENT,"Pass")); }
				if (tag.hasParam("name"))
				{	n.addaction(new Action(Action.Type.NAME,tag.getValue("name")));
				}
				if (tag.hasParam("timeleft"))
				{	n.addaction(new Action(Action.Type.BLACK_TIME,tag.getValue("timeleft")));
				}
				Tree<Node> newp=new Tree<Node>(n);
				if (p==null) ret=newp;
				if (p!=null) p.addchild(newp);
				p=newp;
			}
			else if (tag.name().equals("Comment"))
			{	if (p==null)
				{	Node n=new Node(number);
					n.main(main);
					p=new Tree<Node>(n);
					ret=p;					
				}
				Node n=p.content();
				n.addaction(new Action(Action.Type.COMMENT,parseComment(tree)));
			}
			else if (tag.name().equals("Variation"))
			{	Tree<Node> parent=p.parent();
				if (parent==null)
					throw new XmlReaderException("Root node cannot have variation");
				Tree<Node> newp=readnodes(tree.getContent(),null,tree,false,1);
				parent.addchild(newp);
			}
			else
			{	throw new XmlReaderException(
					"Illegal Node or Variation <"+tag.name()+">");
			}
		}
		return ret;
	}
	
	public Node readnode (int number, XmlTree tree)
		throws XmlReaderException
	{	Node n=new Node(number);
		XmlTag tag=tree.getTag();
		if (tag.hasParam("name"))
		{	n.addaction(new Action(Action.Type.NAME,tag.getValue("name")));
		}
		if (tag.hasParam("blacktime"))
		{	n.addaction(new Action(Action.Type.BLACK_TIME,tag.getValue("blacktime")));
		}
		if (tag.hasParam("whitetime"))
		{	n.addaction(new Action(Action.Type.WHITE_TIME,tag.getValue("whitetime")));
		}
		Enumeration e=tree.getContent();
		while (e.hasMoreElements())
		{	XmlTree t=(XmlTree)e.nextElement();
			tag=t.getTag();
			if (tag.name().equals("Black"))
			{	try
				{	n.addaction(new Action(Action.Type.BLACK,xmlToSgf(t)));
					n.number(n.number()+1);
				}
				catch (XmlReaderException ey) {}
			}
			else if (tag.name().equals("White"))
			{	try
				{	n.addaction(new Action(Action.Type.WHITE,xmlToSgf(t)));
					n.number(n.number()+1);
				}
				catch (XmlReaderException ey) {}
			}
			else if (tag.name().equals("AddBlack"))
			{	n.addaction(new Action(Action.Type.ADD_BLACK,xmlToSgf(t)));
			}
			else if (tag.name().equals("AddWhite"))
			{	n.addaction(new Action(Action.Type.ADD_WHITE,xmlToSgf(t)));
			}
			else if (tag.name().equals("Delete"))
			{	n.expandaction(new Action(Action.Type.ADD_EMPTY,xmlToSgf(t)));
			}
			else if (tag.name().equals("Mark"))
			{	if (tag.hasParam("type"))
				{	switch(tag.getValue("type"))
					{
					case "triangle":
						n.expandaction(new Action(Field.Marker.TRIANGLE.value,xmlToSgf(t)));
						break;
					case "square":
						n.expandaction(new Action(Field.Marker.SQUARE.value,xmlToSgf(t)));
						break;
					case "circle":
						n.expandaction(new Action(Field.Marker.CIRCLE.value,xmlToSgf(t)));
						break;
					}
				}
				else if (tag.hasParam("label"))
				{	String s=tag.getValue("label");
					n.expandaction(new Action(Action.Type.LABEL,xmlToSgf(t)+":"+s));
				}
				else if (tag.hasParam("territory"))
				{	String s=tag.getValue("territory");
					if (s.equals("white"))
					{	n.expandaction(new Action(Action.Type.WHITE_TERRITORY,xmlToSgf(t)));
					}
					else if (s.equals("black"))
					{	n.expandaction(new Action(Action.Type.BLACK_TERRITORY,xmlToSgf(t)));
					}
				}
				else n.expandaction(new MarkAction(xmlToSgf(t)));
			}
			else if (tag.name().equals("BlackTimeLeft"))
			{	n.addaction(new Action(Action.Type.BLACK_TIME,getText(t)));
			}
			else if (tag.name().equals("WhiteTimeLeft"))
			{	n.addaction(new Action(Action.Type.WHITE_TIME,getText(t)));
			}
			else if (tag.name().equals("Comment"))
			{	n.addaction(new Action(Action.Type.COMMENT,parseComment(t)));
			}
			else if (tag.name().equals("SGF"))
			{	if (!tag.hasParam("type"))
					throw new XmlReaderException("Illegal <SGF> tag.");
				Action a;
				if (tag.getValue("type").equals("M")) {
					a=new MarkAction();
				}
				else {
					a=new Action(Action.Type.fromString(tag.getValue("type")));
				}
				Enumeration eh=t.getContent();
				while (eh.hasMoreElements())
				{	XmlTree th=(XmlTree)eh.nextElement();
					XmlTag tagh=th.getTag();
					if (!tagh.name().equals("Arg"))
						throw new XmlReaderException("Illegal <SGF> tag.");
					if (!th.isText())
						throw new XmlReaderException("Illegal <SGF> tag.");
					else a.addargument(th.getText());
				}
				n.addaction(a);
			}
		}
		return n;
	}
	
	public static String parseComment (XmlTree t)
		throws XmlReaderException
	{	StringBuffer s=new StringBuffer();
		Enumeration e=t.getContent();
		while (e.hasMoreElements())
		{	XmlTree tree=(XmlTree)e.nextElement();
			XmlTag tag=tree.getTag();
			if (tag.name().equals("boardPosition"))
			{	if (tree.children().isEmpty()) s.append("\n");
				else
				{	Tree<XmlTag> h=tree.xmlFirstContent();
					String k=((XmlTagText)h.content()).getContent();
					k=k.replace('\n',' ');
					StringParser p=new StringParser(k);
					Vector v=p.wraplines(1000);
					for (int i=0; i<v.size(); i++)
					{	s.append((String)v.elementAt(i));
						s.append("\n");
					}
				}
			}
			else if (tag instanceof XmlTagText)
			{	String k=((XmlTagText)tag).getContent();
					k=k.replace('\n',' ');
					StringParser p=new StringParser(k);
					Vector v=p.wraplines(1000);
					for (int i=0; i<v.size(); i++)
					{	s.append((String)v.elementAt(i));
						s.append("\n");
					}				
			}
			else
				throw new XmlReaderException("<"+tag.name()+"> not proper here.");
		}
		return s.toString();
	}	
	
	public String xmlToSgf (XmlTree tree)
		throws XmlReaderException
	{	XmlTag tag=tree.getTag();
		if (tag.hasParam("at"))
		{	return xmlToSgf(tag.getValue("at"));
		}
		Enumeration e=tree.getContent();
		if (!e.hasMoreElements())
			throw new XmlReaderException("Missing board position.");
		tag=((XmlTree)e.nextElement()).getTag();
		if (tag instanceof XmlTagText)
		{	String pos=((XmlTagText)tag).getContent();
			return xmlToSgf(pos);
		}
		else if (tag.name().equals("at"))
		{	String pos=((XmlTagText)tag).getContent();
			return xmlToSgf(pos);
		}
		else
			throw new XmlReaderException(tag.name()
				+" contains wrong board position.");
	}
	
	public String xmlToSgf (String pos)
		throws XmlReaderException
	{	if (pos.length()<2) wrongBoardPosition(pos);
		int n=pos.indexOf(",");
		if (n>0 && n<pos.length())
		{	String s1=pos.substring(0,n),s2=pos.substring(n+1);
			try
			{	int i=Integer.parseInt(s1)-1;
				int j=Integer.parseInt(s2);
				j=BoardSize-j;
				if (i<0 || i>=BoardSize || j<0 || j>=BoardSize)
					wrongBoardPosition(pos);
				return Field.string(i,j);
			}
			catch (Exception ex)
			{	wrongBoardPosition(pos);
			}
		}
		char c=Character.toUpperCase(pos.charAt(0));
		if (c>='J') c--;
		int i=c-'A';
		int j=0;
		try
		{	j=Integer.parseInt(pos.substring(1));
		}
		catch (Exception ex)
		{	wrongBoardPosition(pos);
		}
		j=BoardSize-j;
		if (i<0 || i>=BoardSize || j<0 || j>=BoardSize)
			wrongBoardPosition(pos);
		return Field.string(i,j);
	}
	
	public void wrongBoardPosition (String s)
		throws XmlReaderException
	{	throw new XmlReaderException("Wrong Board Position "+s);
	}
	
	public static void xmlMissing (String s)
		throws XmlReaderException
	{	throw new XmlReaderException("Missing <"+s+">");
	}
	
	public static void testTag (XmlTag tag, String name)
		throws XmlReaderException
	{	if (!tag.name().equals(name))
		{	throw new XmlReaderException(
				"<"+name+"> expected instead of <"+tag.name()+">");
		}
	}

	/**
	Read a number of trees from an XML file.
	*/
	public static Vector load (XmlReader xml)
		throws XmlReaderException
	{	XmlTree t=xml.scan();
		if (t==null) throw new XmlReaderException("Illegal file format");
		Vector v=readnodes(t);
		return v;
	}

	/**
	Print the tree to the specified PrintWriter.
	@param p the subtree to be printed
	*/
	void printtree (Tree<Node> p, PrintWriter o)
	{	o.println("(");
	    while (true)
		{	p.content().print(o);
			if (!p.haschildren()) break;
			if (p.lastchild()!=p.firstchild())
			{
				for (Tree<Node> e : p.children())
				{	printtree(e,o);
				}
				break;
			}
			p=p.firstchild();
		}
		o.println(")");
	}

	/**
	Print the tree to the specified PrintWriter.
	@param p the subtree to be printed
	*/
	void printtree (Tree<Node> p, XmlWriter xml, int size, boolean top)
	{	if (top)
		{	String s=p.content().getaction(Action.Type.GAME_NAME);
			if (s!=null && !s.equals(""))
				xml.startTagNewLine("GoGame","name",s);
			else
				xml.startTagNewLine("GoGame");
			xml.startTagNewLine("Information");
			printInformation(xml,p, Action.Type.APPLICATION,"Application");
			printInformation(xml,p, Action.Type.SIZE,"BoardSize");
			printInformation(xml,p, Action.Type.BLACK_PLAYER_NAME,"BlackPlayer");
			printInformation(xml,p, Action.Type.BLACK_PLAYER_RANK,"BlackRank");
			printInformation(xml,p, Action.Type.WHITE_PLAYER_NAME,"WhitePlayer");
			printInformation(xml,p, Action.Type.WHITE_PLAYER_RANK,"WhiteRank");
			printInformation(xml,p, Action.Type.DATE,"Date");
			printInformation(xml,p, Action.Type.TIME,"Time");
			printInformation(xml,p, Action.Type.KOMI,"Komi");
			printInformation(xml,p, Action.Type.RESULT,"Result");
			printInformation(xml,p, Action.Type.HANDICAP,"Handicap");
			printInformation(xml,p, Action.Type.USER,"User");
			printInformationText(xml,p, Action.Type.COPYRIGHT,"Copyright");
			xml.endTagNewLine("Information");
		}
		else xml.startTagNewLine("Variation");
		if (top) xml.startTagNewLine("Nodes");
		for ( ; ; p=p.firstchild())
		{	p.content().print(xml,size);
			if (!p.haschildren()) break;
			if (p.children().size() == 1) continue;
			Tree<Node> node=p.children().getFirst();
			node.content().print(xml,size); // PV - siblings are alternatives (comments)
			for (node = p.nextchild(node); node != null; node = p.nextchild(node))
			{	printtree(node,xml,size,false);
			}
			if (!p.haschildren()) break;
		}
		if (top) xml.endTagNewLine("Nodes");
		if (top) xml.endTagNewLine("GoGame");
		else xml.endTagNewLine("Variation");
	}

	private void printInformation (XmlWriter xml, Tree<Node> p,
		Action.Type tag, String xmltag)
	{	String s=p.content().getaction(tag);
		if (s!=null && !s.equals(""))
			xml.printTagNewLine(xmltag,s);
	}

	private void printInformationText (XmlWriter xml, Tree<Node> p,
		Action.Type tag, String xmltag)
	{
		String s=p.content().getaction(tag);
		if (s!=null && !s.equals(""))
		{	xml.startTagNewLine(xmltag);
			xml.printParagraphs(s,60);
			xml.endTagNewLine(xmltag);
		}
	}

	/**
	Print this tree to the PrintWriter starting at the root node.
	*/
	public void print (PrintWriter o)
	{	printtree(History,o);
	}
	
	public void printXML (XmlWriter xml)
	{	printtree(History,xml,getSize(),true);
	}
	
	public int getSize ()
	{	try
		{	return Integer.parseInt(History.content().getaction(Action.Type.SIZE));
		}
		catch (Exception e)
		{	return 19;
		}
	}
}
