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

	public ConnectedBoard (ConnectedGoFrame gf, UIState uiState)
	{	super(uiState);
		CGF=gf;
	}

	public synchronized void setmouse (int i, int j, int c)	
	{	if (uiState.getBoardState().current().content().main() && CGF.wantsmove()) return;
		super.setmouse(i,j,c);
	}

	/**
	In a ConnectedBoard you cannot fix the game tree this way.
	*/
	public synchronized void setmousec (int i, int j, int c) {}

	public synchronized void movemouse (int i, int j)
	{	if (uiState.getBoardState().hasChildren()) return;
		if (uiState.getBoardPosition().color(i,j)!=0) return;
		if (captured==1 && capturei==i && capturej==j) return;
		if (uiState.getBoardState().current().content().main() && CGF.wantsmove())
		{	if (CGF.moveset(i,j))
			{
				update(i,j); copy();
				MyColor= uiState.getBoardPosition().color();
			}
			JagoSound.play("click","",false);
		}
		else uiState.getBoardState().set(i,j); // try to set a new move
	}

	/**
	Completely remove a group (at end of game, before count), and
	note all removals. This is only allowed in end nodes and if
	the GoFrame wants the removal, it gets it.
	*/
	public synchronized void removegroup (int i0, int j0)
	{	if (uiState.getBoardState().hasChildren()) return;
		if (uiState.getBoardPosition().color(i0,j0)==0) return;
		if (CGF.wantsmove() && uiState.getBoardState().current().content().main())
		{	CGF.moveset(i0,j0);
		}
		//uiState.getBoardState().removegroup(i0,j0);
	}

	/**
	Take back the last move.
	*/
	public synchronized void undo ()
	{	if (uiState.getBoardState().current().content().main()
			&& CGF.wantsmove())
		{	if (!uiState.getBoardState().hasChildren())
			{	if (uiState.getUiMode() != UIState.UIMode.PLAY_BLACK && uiState.getUiMode() != UIState.UIMode.PLAY_WHITE) uiState.getBoardState().clearremovals();
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
	{	if (uiState.getBoardState().hasChildren()) return;
		if (uiState.getBoardState().current().content().main()) return;
		if (uiState.getBoardState().current().content().main() && CGF.wantsmove())
		{	CGF.movepass(); return;
		}
	}

	/**
	This is used to fix the game tree (after confirmation).
	Will not be possible, if the GoFrame wants moves.
	*/
	public synchronized void insertnode ()
	{	if (!uiState.getBoardState().hasChildren() && uiState.getBoardState().current().content().main() && CGF.wantsmove()) return;
	}
	
	/**
	In a ConnectedBoard you cannot delete stones.
	*/
	public synchronized void deletemousec (int i, int j) {}
}
