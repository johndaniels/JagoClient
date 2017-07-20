package jagoclient.igs;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
This is a stream, which filters Telnet commands.
*/

public class TelnetStream extends FilterInputStream
{	private static final Logger LOG = Logger.getLogger(TelnetStream.class.getName());
	InputStream In;
	OutputStream Out;


	public TelnetStream (InputStream in,
		OutputStream out)
	{	super(in);
		In=in;
		Out=out;
	}
		
	public int read () throws IOException
	{	while (true)
		{	int c=In.read();
			if (c==255) // Telnet ??
			{	int command=In.read();
				LOG.log(Level.INFO, "Telnet received!{0}", command);
				if (command==253)
				{	c=In.read();
					Out.write(255);
					Out.write(252);
					Out.write(c);
				}
				else if (command==246)
				{	Out.write(255);
					Out.write(241);
				}
				if (c==-1) return c;
				return 0;
			}
			if (c>=0 && c<=9) return 0;
			if (c==12) return 0;
			return c;
		}
	}
	
	public int read (byte b[], int off, int len)
		throws IOException
	{	int i=0;
		int c=read();
		if (c==0) c=' ';
		b[off+i]=(byte)c;
		i++;
		if (c==-1) return 1;
		while (In.available()>0 && i<len)
		{	c=read();
			if (c==0) c=' ';
			b[off+i]=(byte)c;
			i++;
			if (c==-1) break;
		}
		return i;
	}
	
}
