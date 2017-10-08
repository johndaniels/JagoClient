package jagoclient.board;

import java.io.PrintWriter;

import rene.util.list.ListElement;

/**
This action class takes special care to print labels in SGF form.
Jago notes labes in consecutive letters, but SGF does not have this
feature, thus it outputs labels as LB[field:letter].
*/

public class LabelAction extends Action {
	public LabelAction (String arg)
	{
		super(Type.LABEL,arg);
	}
	public LabelAction ()
	{
		super(Type.LABEL);
	}
	public void print (PrintWriter o)
	{
		o.println();
		o.print("LB");
		char[] c=new char[1];
		int i=0;
		for (String argument : Arguments)
		{	c[0]=(char)('a'+i);
			o.print("["+argument+":"+new String(c)+"]");
			i++;
		}
	}
}
