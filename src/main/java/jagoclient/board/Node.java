package jagoclient.board;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rene.util.list.ListClass;
import rene.util.list.ListElement;
import rene.util.list.Tree;
import rene.util.xml.XmlWriter;

/**
A node has 
<UL>
<LI> a list of actions and a number counter (the number is the number
of the next expected move in the game tree),
<LI> a flag, if the node is in the main game tree,
<LI> a list of changes in this node to be able to undo the node,
<LI> the changes in the prisoner count in this node.
</UL>
@see jagoclient.board.Action
@see jagoclient.board.Change
*/

public class Node
{
	LinkedList<Action> Actions; // actions and variations
	int N; // next exptected number
	boolean Main; // belongs to main variation
	LinkedList<Change> Changes;
	public int Pw,Pb; // changes in prisoners in this node
	
	/** initialize with the expected number */
	public Node (int n)
	{
		Actions=new LinkedList<>();
		N=n;
		Main=false;
		Changes=new LinkedList<>();
		Pw=Pb=0;
	}
	
	/** add an action (at end) */
	public void addaction (Action a)
	{
		Actions.add(a);
	}

	/** expand an action of the same type as a, else generate a new action */
	public void expandaction (Action a)
	{
		Action pa = find(a.type());
		if (pa==null) {
			addaction(a);
		}
		else {
			pa.addargument(a.argument());
		}
	}

	/**
	Expand an action of the same type as a, else generate a new action.
	If the action is already present with the same argument, delete
	that argument from the action.
	*/
	public void toggleaction (Action a)
	{	Action pa =find(a.type());
		if (a==null) {
			addaction(a);
		}
		else
		{
			pa.toggleargument(a.argument());
		}
	}

	/** find the list element containing the action of type s */
	Action find (Action.Type s)
	{	
		for (Action a : Actions)
		{
			if (a.type().equals(s)) return a;
		}
		return null;
	}
	
	/** find the action and a specified tag */
	public boolean contains (Action.Type s, String argument)
	{
		Action a=find(s);
		if (a==null) return false;
		return a.contains(argument);
	}
	
	/** see if the list contains an action of type s */
	public boolean contains (Action.Type s)
	{	return find(s)!=null;
	}

	/** add an action (at front) */
	public void prependaction (Action a)
	{	Actions.add(0, a);
	}
	
	/**
	If there is an action of the type:
	Remove it, if arg is "", else set its argument to arg.
	Else add a new action in front (if it is true)
	*/
	public void setaction (Action.Type type, String arg, boolean front)
	{	
		for (Action a : Actions)
		{
			if (a.type().equals(type))
			{	if (arg.equals(""))
				{	Actions.remove(a);
					return;
				}
				else
				{	a.arguments().set(0, arg);
				}
				return;
			}
		}
		if (front) prependaction(new Action(type,arg));
		else addaction(new Action(type,arg));
	}

	/** set the action of this type to this argument */
	public void setaction (Action.Type type, String arg)
	{	setaction(type, arg,false);
	}

	/** get the argument of this action (or "") */
	public String getaction (Action.Type type)
	{
		for (Action a : Actions)
		{
			if (a.type().equals(type))
			{	if (!a.arguments().isEmpty()) return a.arguments().get(0);
				else return "";
			}
		}
		return "";
	}
	
	/** 
	Print the node in SGF.
	@see jagoclient.board.Action#print
	*/
	public void print (PrintWriter o)
	{	o.print(";");
		for (Action a : Actions)
		{
			a.print(o);
		}
		o.println("");
	}
	
	public void print (XmlWriter xml, int size)
	{	int count=0;
		Action ra=null;
		for (Action a : Actions)
		{
			if (a.isRelevant())
			{	count++;
				ra=a;
			}
		}
		if (count==0 && !contains(Action.Type.COMMENT))
		{	xml.finishTagNewLine("Node");
			return;
		}
		int number=N-1;
		if (count==1)
		{	if (ra.type().equals(Action.Type.BLACK) || ra.type().equals(Action.Type.WHITE))
			{	ra.printMove(xml,size,number,this);
				number++;
				if (contains(Action.Type.COMMENT))
				{
					Action a=find(Action.Type.COMMENT);
					a.print(xml,size,number);
				}
				return;
			}
		}
		xml.startTagStart("Node");
		if (contains(Action.Type.NAME)) xml.printArg("name",getaction(Action.Type.NAME));
		if (contains(Action.Type.BLACK_TIME)) xml.printArg("blacktime",getaction(Action.Type.BLACK_TIME));
		if (contains(Action.Type.WHITE_TIME)) xml.printArg("whitetime",getaction(Action.Type.WHITE_TIME));
		xml.startTagEndNewLine();
		for (Action a : Actions)
		{
			a.print(xml,size,number);
			if (a.type().equals(Action.Type.BLACK) || a.type().equals(Action.Type.WHITE)) number++;
		}
		xml.endTagNewLine("Node");
	}

	/** remove all actions */
	public void removeactions ()
	{	Actions= new LinkedList<>();
	}

	/** add a new change to this node */
	public void addchange (Change c)
	{	Changes.add(c);
	}

	/** clear the list of changes */
	public void clearchanges ()
	{	Changes.clear();
	}	

	// modification methods:
	public void main (boolean m) { Main=m; }
	/** 
	Set the Main flag
	@param p is the tree, which contains this node on root.
	*/
	public void main (Tree p)
	{   Main=false;
		try
		{	if (((Node)p.content()).main())
			{	Main=(this==((Node)p.firstchild().content()));
			}
			else if (p.parent()==null) Main=true;
		}
		catch (Exception e) {}
	}
	public void number (int n) { N=n; }
	
	/**
	Copy an action from another node.
	*/
	public void copyAction (Node n, Action.Type action)
	{	if (n.contains(action))
		{	expandaction(new Action(action,n.getaction(action)));
		}
	}

	// access methods:
	public LinkedList<Action> actions () { return Actions; }
	public LinkedList<Change> changes () { return Changes; }
	public int number () { return N; }
	public boolean main () { return Main; }
}
