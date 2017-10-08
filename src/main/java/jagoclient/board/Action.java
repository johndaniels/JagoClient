package jagoclient.board;

import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import rene.util.parser.StringParser;
import rene.util.xml.XmlWriter;

/** 
Has a type and arguments (as in SGF, e.g. B[ih] of type "B" and
Methods include the printing on a PrintWriter.
*/
public class Action
{
	public enum Type {
		FILE_FORMAT("FF"),
		GAME_TYPE("GM"),
		WHITE("W"),
		BLACK("B"),
		SIZE("SZ"),
		COMMENT("C"),
		BLACK_TIME("BL"),
		WHITE_TIME("WL"),
		NAME("N"),
		GAME_NAME("GN"),
		DATE("DT"),
		BLACK_PLAYER_NAME("BP"),
		BLACK_PLAYER_RANK("BR"),
		WHITE_PLAYER_NAME("WP"),
		WHITE_PLAYER_RANK("WR"),
		RESULT("RE"),
		TIME("TM"),
		KOMI("KM"),
		HANDICAP("HA"),
		USER("US"),
		COPYRIGHT("CP"),
		ADD_WHITE("AW"),
		ADD_BLACK("AB"),
		ADD_EMPTY("AE"),
		CROSS("MA"),
		SQUARE("SQ"),
		TRIANGLE("TR"),
		CIRCLE("CR"),
		LABEL("LB"),
		BLACK_TERRITORY("TB"),
		WHITE_TERRITORY("TW"),
		APPLICATION("AP"),
		MARK("M"),
		SELECT("SL");

		private String value;

		private static Map<String, Type> stringToTypeMap = new HashMap<>();

		static {
			for (Type t : Type.values()) {
				stringToTypeMap.put(t.value, t);
			}
		}

		Type(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

		public static Type fromString(String s) {
			return stringToTypeMap.get(s);
		}
	}
	Type type; // the type
	List<String> Arguments; // the list of argument strings
	
	/**
	Initialize with type only
	*/
	public Action (Type s)
	{
		type =s;
		Arguments=new ArrayList<String>();
	}

	/**
	Initialize with type and one argument to that type tag.
	*/
	public Action (Type s, String arg)
	{
		type =s;
		Arguments=new ArrayList<String>();
		addargument(arg);
	}
	
	public void addargument (String s)
	// add an argument ot the list (at end)
	{
		Arguments.add(s);
	}
	
	public void toggleargument (String s)
	// add an argument ot the list (at end)
	{
		Arguments.remove(s);
		Arguments.add(s);
	}

	/** Find an argument */	
	public boolean contains (String s)
	{
		return Arguments.contains(s);
	}
	
	public void print (PrintWriter o)
	// print the action
	{	if (Arguments.isEmpty() || (Arguments.size()==1 && Arguments.get(0).isEmpty()))
			return;
		o.println();
		o.print(type);
		for (String s : Arguments)
		{	o.print("[");
			StringParser p=new StringParser(s);
			Vector v=p.wrapwords(60);
			for (int i=0; i<v.size(); i++)
			{	s=(String)v.elementAt(i);
				if (i>0) o.println();
				int k=s.indexOf(']');
				while (k>=0)
				{	if (k>0) o.print(s.substring(0,k));
					o.print("\\]");
					s=s.substring(k+1);
					k=s.indexOf(']');
				}
				o.print(s);
			}
			o.print("]");
		}
	}
	
	/**
	Print the node content in XML form.
	*/
	public void print (XmlWriter xml, int size, int number)
	{	if (type.equals("C"))
		{	xml.startTagNewLine("Comment");
			printTextArgument(xml);
			xml.endTagNewLine("Comment");
		}
		else if (type.equals("GN")
			|| type.equals("AP")
			|| type.equals("FF")
			|| type.equals("GM")
			|| type.equals("N")
			|| type.equals("SZ")
			|| type.equals("PB")
			|| type.equals("BR")
			|| type.equals("PW")
			|| type.equals("WR")
			|| type.equals("HA")
			|| type.equals("KM")
			|| type.equals("RE")
			|| type.equals("DT")
			|| type.equals("TM")
			|| type.equals("US")
			|| type.equals("WL")
			|| type.equals("BL")
			|| type.equals("CP")
			)
		{
		}
		else if (type.equals("B"))
		{	xml.startTagStart("Black");
			xml.printArg("number",""+number);
			xml.printArg("at",getXMLMove(size));
			xml.finishTagNewLine();
		}
		else if (type.equals("W"))
		{	xml.startTagStart("White");
			xml.printArg("number",""+number);
			xml.printArg("at",getXMLMove(size));
			xml.finishTagNewLine();
		}
		else if (type.equals("AB"))
		{	printAllFields(xml,size,"AddBlack");
		}
		else if (type.equals("AW"))
		{	printAllFields(xml,size,"AddWhite");
		}
		else if (type.equals("AE"))
		{	printAllFields(xml,size,"Delete");
		}
		else if (type.equals(Field.Marker.CROSS.value))
		{	printAllFields(xml,size,"Mark");
		}
		else if (type.equals("M"))
		{	printAllFields(xml,size,"Mark");
		}
		else if (type.equals(Field.Marker.SQUARE.value))
		{	printAllFields(xml,size,"Mark","type","square");
		}
		else if (type.equals(Field.Marker.CIRCLE.value))
		{	printAllFields(xml,size,"Mark","type","circle");
		}
		else if (type.equals(Field.Marker.TRIANGLE.value))
		{	printAllFields(xml,size,"Mark","type","triangle");
		}
		else if (type.equals("TB"))
		{	printAllFields(xml,size,"Mark","territory","black");
		}
		else if (type.equals("TW"))
		{	printAllFields(xml,size,"Mark","territory","white");
		}
		else if (type.equals("LB"))
		{	printAllSpecialFields(xml,size,"Mark","label");
		}
		else
		{	xml.startTag("SGF","type", type.toString());
			for (String argument : Arguments)
			{	xml.startTag("Arg");
				StringParser p=new StringParser(argument);
				Vector<String> v=p.wrapwords(60);
				for (int i=0; i<v.size(); i++)
				{	argument=v.elementAt(i);
					if (i>0) xml.println();
					xml.print(argument);
				}
				xml.endTag("Arg");
			}
			xml.endTagNewLine("SGF");
		}
	}

	/**
	Print the node content of a move in XML form and take care of times
	and names.
	*/
	public void printMove (XmlWriter xml, int size, int number, Node n)
	{	String s="";
		if (type.equals("B")) s="Black";
		else if (type.equals("W")) s="White";
		else return;
		xml.startTagStart(s);
		xml.printArg("number",""+number);
		if (n.contains(Type.NAME)) xml.printArg("name",n.getaction(Type.NAME));
		if (s.equals("Black") && n.contains(Type.BLACK_TIME))
			xml.printArg("timeleft",n.getaction(Type.BLACK_TIME));
		if (s.equals("White") && n.contains(Type.WHITE_TIME))
			xml.printArg("timeleft",n.getaction(Type.WHITE_TIME));
		xml.printArg("at",getXMLMove(size));
		xml.finishTagNewLine();
	}

	/**
	Test, if this action contains printed information
	*/
	public boolean isRelevant ()
	{	if (type.equals("GN")
			|| type.equals("AP")
			|| type.equals("FF")
			|| type.equals("GM")
			|| type.equals("N")
			|| type.equals("SZ")
			|| type.equals("PB")
			|| type.equals("BR")
			|| type.equals("PW")
			|| type.equals("WR")
			|| type.equals("HA")
			|| type.equals("KM")
			|| type.equals("RE")
			|| type.equals("DT")
			|| type.equals("TM")
			|| type.equals("US")
			|| type.equals("CP")
			|| type.equals("BL")
			|| type.equals("WL")
			|| type.equals("C")
			)
		return false;
		else return true;
	}

	/**
	Print all arguments as field positions with the specified tag.
	*/
	public void printAllFields (XmlWriter xml, int size, String tag)
	{
		for (String s : Arguments)
		{	xml.startTagStart(tag);
			xml.printArg("at",getXMLMove(s,size));
			xml.finishTagNewLine();
		}
	}
	
	public void printAllFields (XmlWriter xml, int size, String tag,
		String argument, String value)
	{
		for (String s : Arguments)
		{	xml.startTagStart(tag);
			xml.printArg(argument,value);
			xml.printArg("at",getXMLMove(s,size));
			xml.finishTagNewLine();
		}
	}

	public void printAllSpecialFields (XmlWriter xml, int size, String tag, String argument)
	{
		Pattern pattern = Pattern.compile("[^:]*:(\\w+).*");
		for (String s : Arguments)
		{	Matcher matcher = pattern.matcher(s);
			if (matcher.matches())
			{
				xml.startTagStart(tag);
				xml.printArg(argument,matcher.group(1));
				xml.printArg("at",getXMLMove(s,size));
				xml.finishTagNewLine();
			}
		}
	}

	/**
	@return The readable coordinate version (Q16) of a move,
	stored in first argument.
	*/
	public String getXMLMove (String s, int size)
	{	if (s==null) return "";
		int i=Field.i(s),j=Field.j(s);
		if (i<0 || i>=size || j<0 || j>=size) return "";
		return Field.coordinate(Field.i(s),Field.j(s),size);
	}
	
	public String getXMLMove (int size)
	{	return getXMLMove(Arguments.get(0),size);
	}

	public void printTextArgument (XmlWriter xml)
	{	if (Arguments.isEmpty()) return;
		xml.printParagraphs(Arguments.get(0),60);
	}

	// modifiers
	public void type (Type s) { type =s; }

	// access methods:
	public Type type () { return type; }
	public List<String> arguments () { return Arguments; }
	public String argument ()
	{	if (arguments()==null) return "";
		return arguments().get(0);
	}
}
