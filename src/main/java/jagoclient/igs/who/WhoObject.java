package jagoclient.igs.who;

import jagoclient.Global;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rene.util.parser.StringParser;

/**
This is needed for the Sorter class.
*/

public class WhoObject implements Comparable<WhoObject>
{
	protected static final Pattern WORD_PATTERN = Pattern.compile("\\w+");
	String S,Name,Stat;
	public int rankValue;
	boolean sortName;

	private boolean quiet;
	private boolean silent;
	private boolean looking;

	private class WhoParser {
		String s;
		int position;


		public WhoParser(String s) {
			this.s = s;
			this.position = 0;
		}

		private void parseStatus() {
			while (true) {
				switch (s.charAt(position)) {
					case 'Q':
						quiet = true;
						break;
					case 'X':
						silent = true;
						break;
					case '!':
						looking = true;
						break;
					default:
						return;
				}
				position++;
			}
		}

		private void skipSpaces() {
			while (position < s.length() && s.charAt(position) == ' ') {
				position++;
			}
		}

		private int parseNumber() {
			char digit = s.charAt(position);
			int number = 0;
			while (digit <= '9' && digit >= '0') {
				number *= 10;
				number += digit - '0';
				position++;
				digit = s.charAt(position);
			}
			return number;
		}

		private void parseNumberOrDashes() {
			if (s.charAt(position) == '-') {
				while (s.charAt(position) == '-') {
					position++;
				}
				return;
			}
			parseNumber();
		}

		private String parseUsername() {
			StringBuilder sb = new StringBuilder();
			while (position < s.length() && s.charAt(position) != ' ') {
				sb.append(s.charAt(position));
				position++;
			}
			return sb.toString();
		}

		private void parseTime() {
			parseNumber();
			position++; // Skip minute/second marker
		}

		private int parseRank() {
			if (s.charAt(position) == 'N') {
				position += 2;
				return 0 ;
			}
			int number = parseNumber();
			int finalRank;
			if (s.charAt(position) == 'k') {
				finalRank = (20 - number) * 2;
			} else if (s.charAt(position) == 'd'){
				finalRank = 100 + number * 2;
			} else {
				finalRank = 200 + number * 2;
			}
			position++;
			if (position < s.length() && s.charAt(position) == '+') {
				finalRank++;
				position++;
			}
			return finalRank;
		}

		public void parse() {
			skipSpaces();
			parseStatus();
			skipSpaces();
			parseNumberOrDashes();
			skipSpaces();
			parseNumberOrDashes();
			skipSpaces();
			Name = parseUsername();
			skipSpaces();
			parseTime();
			skipSpaces();
			rankValue = parseRank();
		}
	}

	public WhoObject (String s, boolean sortname)
	{
		this.S = s;
		this.sortName=sortname;
		new WhoParser(s).parse();
	}
	String who () { return S; }
	@Override
	public int compareTo (WhoObject g)
	{	if (sortName)
		{	return Name.compareTo(g.Name);
		}
		else
		{	if (rankValue <g.rankValue) return 1;
			else if (rankValue >g.rankValue) return -1;
			else return 0;
		}
	}
	public boolean looking ()
	{	return looking;
	}
	public boolean quiet ()
	{	return quiet;
	}
	public boolean silent ()
	{	return silent;
	}
	public boolean friend ()
	{	return Global.getParameter("friends","").indexOf(" "+Name)>=0;
	}
	public boolean marked ()
	{	return Global.getParameter("marked","").indexOf(" "+Name)>=0;
	}
}
