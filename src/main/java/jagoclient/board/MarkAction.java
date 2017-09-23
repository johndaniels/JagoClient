package jagoclient.board;

import java.io.PrintWriter;

/**
This is a special action for marks. It will print it content
depending on the "puresgf" parameter. This is because the
new SGF format no longer allows the "M" tag.
@see jagoclient.board.Action
*/

public class MarkAction extends Action {
	public MarkAction (String arg)
	{
		super("M",arg);
	}
	public MarkAction ()
	{
		super("M");
	}
	public void print (PrintWriter o)
	{
		super.print(o);
	}
}
