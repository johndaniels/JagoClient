package jagoclient.igs.who;

import jagoclient.igs.Distributor;
import jagoclient.igs.IgsStream;

/**
A distributor for the player listing.
*/

public class WhoDistributor extends Distributor
{
	public interface WhoHandler {
		void handleWho(WhoObject object);
	}

	WhoHandler handler;
	public WhoDistributor (IgsStream in, WhoHandler handler)
	{	super(in,27,0,null,null);
		this.handler = handler;
	}
	@Override
	public void send (String c)
	{
		handler.handleWho(new WhoObject(c, false));
	}
}
