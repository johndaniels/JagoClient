package jagoclient.board;

import jagoclient.sound.JagoSound;

/**
This board overrides some methods from the Board class
for connected boards. It knows a ConnectedGoFrame to
ask for permission of operations and to report moves
to it.
*/

public class ConnectedBoard extends Board
{
	ConnectedGoFrame CGF;

	public ConnectedBoard (ConnectedGoFrame gf, GameViewerState gameViewerState)
	{	super(gameViewerState);
		CGF=gf;
	}

	public synchronized void setmouse (int i, int j, int c)	
	{	if (gameViewerState.getBoardState().current().content().main() && CGF.wantsmove()) return;
		super.setmouse(i,j,c);
	}

	/**
	In a ConnectedBoard you cannot fix the game tree this way.
	*/
	public synchronized void setmousec (int i, int j, int c) {}

	public synchronized void movemouse (int i, int j)
	{	if (gameViewerState.getBoardState().hasChildren()) return;
		if (gameViewerState.getBoardPosition().color(i,j)!=0) return;
		if (captured==1 && capturei==i && capturej==j) return;
		if (gameViewerState.getBoardState().current().content().main() && CGF.wantsmove())
		{	if (CGF.moveset(i,j))
			{
				update(i,j); copy();
				MyColor= gameViewerState.getBoardPosition().color();
			}
			JagoSound.play("click","",false);
		}
		else gameViewerState.getBoardState().set(i,j); // try to set a new move
	}

	/**
	Completely remove a group (at end of game, before count), and
	note all removals. This is only allowed in end nodes and if
	the GoFrame wants the removal, it gets it.
	*/
	public synchronized void removegroup (int i0, int j0)
	{	if (gameViewerState.getBoardState().hasChildren()) return;
		if (gameViewerState.getBoardPosition().color(i0,j0)==0) return;
		if (CGF.wantsmove() && gameViewerState.getBoardState().current().content().main())
		{	CGF.moveset(i0,j0);
		}
		//gameViewerState.getBoardState().removegroup(i0,j0);
	}

	/**
	Take back the last move.
	*/
	public synchronized void undo ()
	{	if (gameViewerState.getBoardState().current().content().main()
			&& CGF.wantsmove())
		{	if (!gameViewerState.getBoardState().hasChildren())
			{	if (gameViewerState.getUiMode() != GameViewerState.UIMode.PLAY_BLACK && gameViewerState.getUiMode() != GameViewerState.UIMode.PLAY_WHITE) gameViewerState.getBoardState().clearremovals();
				CGF.undo();
			}
			return;
		}
		super.undo();
	}

	/**
	Pass (report to the GoFrame if necessary.
	*/
	public synchronized void pass ()
	{	if (gameViewerState.getBoardState().hasChildren()) return;
		if (gameViewerState.getBoardState().current().content().main()) return;
		if (gameViewerState.getBoardState().current().content().main() && CGF.wantsmove())
		{	CGF.movepass(); return;
		}
	}

	/**
	This is used to fix the game tree (after confirmation).
	Will not be possible, if the GoFrame wants moves.
	*/
	public synchronized void insertnode ()
	{	if (!gameViewerState.getBoardState().hasChildren() && gameViewerState.getBoardState().current().content().main() && CGF.wantsmove()) return;
	}
	
	/**
	In a ConnectedBoard you cannot delete stones.
	*/
	public synchronized void deletemousec (int i, int j) {}
}
