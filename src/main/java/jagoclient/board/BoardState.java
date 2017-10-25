package jagoclient.board;

import jagoclient.Global;
import org.jetbrains.annotations.NotNull;
import rene.util.list.Tree;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlReaderException;
import rene.util.xml.XmlWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


/**
 * Stores all of the game state associated with an SGF, including
 * variations, comments, etc.
 */
public class BoardState implements UIState {
    private int sendi = -1, sendj;
    private Position boardPosition;
    private int S;
    Tree<Node> Pos; // the current board position in this presentation
    private SGFTree T; // the game tree
    int number; // number of the next move
    private String TextMarker = "A";
    private String comment;
    private Field.Marker SpecialMarker = Field.Marker.SQUARE;
    private List<StateChangedHandler> stateChangedHandlers = new ArrayList<>();

    public BoardState(int s) {
        /*if (a.type().equals("SZ"))
        {
            if (Pos.parent() == null)
            // only at first node
            {
                try
                {
                    int ss = Integer.parseInt(a.argument().trim());
                    if (ss != S)
                    {
                        S = ss;
                        boardPosition = new Position(S);
                        makeimages();
                        updateall();
                        copy();
                    }
                }
                catch (NumberFormatException e)
                {}
            }
        }*/
        setsize(s);
    }

    private void setsize (int s)
    // set the board size
    // clears the board !!!
    {
        if (s < 5 || s > 59) return;
        S = s;
        boardPosition = new Position(S);
        number = 1;
        Node n = new Node(number);
        n.main(true);
        T = new SGFTree(n);
        Pos = T.top();
    }

    void gotovariation (int i, int j)
    // goto the variation at (i,j)
    {
        Tree<Node> newpos = boardPosition.tree(i, j);
        getinformation();
        Pos = newpos;
        boardPosition.act(Pos.content());
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    public void variation (int i, int j)
    {
        if (Pos.parent() == null) return;
        if (boardPosition.color(i, j) == 0) // empty?
        {
            int c = boardPosition.color();
            goback();
            boardPosition.color( -c);
            set(i, j);
            boardPosition.number(i, j, 1);
            number = 2;
            Pos.content().number(2);
            stateChanged();
        }
    }

    void doundo (Tree<Node> pos1)
    {
        if (pos1 != Pos) return;
        if (Pos.parent() == null)
        {
            boardPosition.undonode(Pos.content());
            Pos.removeall();
            Pos.content().removeactions();
            return;
        }
        Tree<Node> pos = Pos;
        goback();
        if (pos == Pos.firstchild())
            Pos.removeall();
        else Pos.remove(pos);
        goforward();
        stateChanged();
    }

    private void goback ()
    // go one move back
    {
        if (Pos.parent() == null) return;
        boardPosition.undonode(Pos.content());
        Pos = Pos.parent();
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    private void goforward ()
    // go one move forward
    {
        if ( !Pos.haschildren()) return;
        Pos = Pos.firstchild();
        boardPosition.act(Pos.content());
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    void gotoMove (int move)
    {
        while (number <= move && Pos.firstchild() != null)
        {
            goforward();
        }
        stateChanged();
    }

    private void tovarright ()
    {
        if (Pos.parent() == null) return;
        if (Pos.parent().previouschild(Pos) == null) return;
        Tree<Node> newpos = nextchild(Pos);
        goback();
        Pos = newpos;
        boardPosition.act(Pos.content());
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    static Tree<Node> previouschild (Tree<Node> p)
    {
        if (p.parent() == null) return null;
        return p.parent().previouschild(p);
    }

    static Tree<Node> nextchild (Tree<Node> p)
    {
        if (p.parent() == null) return null;
        return p.parent().nextchild(p);
    }

    public void setpass ()
    {
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Node n = new Node(number);
        p.addchild(new Tree<Node>(n));
        n.main(p);
        if (Pos == p)
        {
            getinformation();
            int c = boardPosition.color();
            goforward();
            boardPosition.color( -c);
        }
        stateChanged();
    }

    private void resettree ()
    {
        Pos = T.top();
        boardPosition = new Position(S);
    }


    private boolean valid (int i, int j)
    {
        return i >= 0 && i < S && j >= 0 && j < S;
    }

    public int getboardsize ()
    {
        return S;
    }

    void varleft ()
    // one variation to the left
    {
        getinformation();
        if (Pos.parent() == null) return;
        if (Pos.parent().previouschild(Pos) == null) return;
        Tree<Node> newpos = Pos.parent().previouschild(Pos);
        goback();
        Pos = newpos;
        boardPosition.act(Pos.content());
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    void varright ()
    // one variation to the right
    {
        getinformation();
        if (Pos.parent() == null) return;
        if (Pos.parent().nextchild(Pos) == null) return;
        Tree<Node> newpos = Pos.parent().nextchild(Pos);
        goback();
        Pos = newpos;
        boardPosition.act(Pos.content());
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    void varmain ()
    // to the main variation
    {
        getinformation();
        while (Pos.parent() != null && !Pos.content().main())
        {
            goback();
        }
        if (Pos.haschildren()) goforward();
        stateChanged();
    }

    void varmaindown ()
    // to end of main variation
    {
        getinformation();
        while (Pos.parent() != null && !Pos.content().main())
        {
            goback();
        }
        while (Pos.haschildren())
        {
            goforward();
        }
        stateChanged();
    }

    public synchronized void varup ()
    // to the start of the variation
    {
        getinformation();
        if (Pos.parent() != null) goback();
        while (Pos.parent() != null
                && Pos.parent().firstchild() == Pos.parent().lastchild()
                && !Pos.content().main())
        {
            goback();
        }
        stateChanged();
    }

    void gotonext ()
    // goto next named node
    {
        getinformation();
        goforward();
        while (Pos.content().getaction(Action.Type.NAME).equals(""))
        {
            if ( !Pos.haschildren()) break;
            goforward();
        }
        stateChanged();
    }

    void gotoprevious ()
    // gotoprevious named node
    {
        getinformation();
        goback();
        while (Pos.content().getaction(Action.Type.NAME).equals(""))
        {
            if (Pos.parent() == null) break;
            goback();
        }
        stateChanged();
    }

    void allback ()
    // to top of tree
    {
        getinformation();
        while (Pos.parent() != null)
            goback();
        stateChanged();
    }

    void allforward ()
    // to end of variation
    {
        getinformation();
        while (Pos.haschildren())
            goforward();
        stateChanged();
    }

    private void getinformation ()
    // update the comment, when leaving the position
    {
        clearsend();
        for (Action a : Pos.content().actions())
        {
            if (a.type() == Action.Type.COMMENT)
            {
                if (comment.equals("")) {
                    Pos.content().actions().remove(a);
                } else {
                    a.arguments().set(0, comment);
                }
                return;
            }
        }
        String s = comment;
        if ( s != null && !s.equals(""))
        {
            Pos.content().addaction(new Action(Action.Type.COMMENT, s));
        }
    }

    public synchronized void back ()
    // one move up
    {
        getinformation();
        goback();
    }

    public synchronized void forward ()
    // one move down
    {
        getinformation();
        goforward();
    }

    public synchronized void fastback ()
    // 10 moves up
    {
        getinformation();
        for (int i = 0; i < 10; i++)
            goback();
    }

    void fastforward ()
    // 10 moves down
    {
        getinformation();
        for (int i = 0; i < 10; i++)
            goforward();
    }

    private void clearsend ()
    {
        if (sendi >= 0)
        {
            int i = sendi;
            sendi = -1;
        }
    }

    public synchronized void black (int i, int j)
    // white move at i,j at the end of the main tree
    {
        if (i < 0 || j < 0 || i >= S || j >= S) return;
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Action a = new Action(Action.Type.BLACK, Field.string(i, j));
        Node n = new Node(p.content().number() + 1);
        n.addaction(a);
        p.addchild(new Tree<>(n));
        n.main(p);
        if (Pos == p) forward();
    }

    public synchronized void white (int i, int j)
    // black move at i,j at the end of the main tree
    {
        if (i < 0 || j < 0 || i >= S || j >= S) return;
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Action a = new Action(Action.Type.WHITE, Field.string(i, j));
        Node n = new Node(p.content().number() + 1);
        n.addaction(a);
        p.addchild(new Tree<Node>(n));
        n.main(p);
        if (Pos == p) forward();
        stateChanged();
    }

    public synchronized void setblack (int i, int j)
    // set a white stone at i,j at the end of the main tree
    {
        if (i < 0 || j < 0 || i >= S || j >= S) return;
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Action a = new Action(Action.Type.ADD_BLACK, Field.string(i, j));
        Node n;
        if (p == T.top())
        {
            Tree<Node> newpos;
            p.addchild(newpos = new Tree<Node>(new Node(1)));
            if (Pos == p) Pos = newpos;
            p = newpos;
            p.content().main(true);
        }
        n = p.content();
        n.expandaction(a);
        if (Pos == p)
        {
            n.addchange(new Change(i, j, boardPosition.color(i, j), boardPosition.number(i, j)));
            boardPosition.color(i, j, 1);
        }
        stateChanged();
    }

    public synchronized void setwhite (int i, int j)
    // set a white stone at i,j at the end of the main tree
    {
        if (i < 0 || j < 0 || i >= S || j >= S) return;
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Action a = new Action(Action.Type.ADD_WHITE, Field.string(i, j));
        Node n;
        if (p == T.top())
        {
            Tree<Node> newpos;
            p.addchild(newpos = new Tree<Node>(new Node(1)));
            if (Pos == p) Pos = newpos;
            p = newpos;
            p.content().main(true);
        }
        n = p.content();
        n.expandaction(a);
        if (Pos == p)
        {
            n.addchange(new Change(i, j, boardPosition.color(i, j), boardPosition.number(i, j)));
            boardPosition.color(i, j, -1);
        }
        stateChanged();
    }

    public synchronized void pass ()
    // pass at current node
    // notify BoardInterface
    {
        if (Pos.haschildren()) return;
        if (Pos.content().main()) return;
        getinformation();
        boardPosition.color( -boardPosition.color());
        Node n = new Node(number);
        Pos.addchild(new Tree<Node>(n));
        n.main(Pos);
        goforward();
        boardPosition.setcurrent(Pos);
        stateChanged();
    }


    // ************ change the current node *****************

    public void clearmarks ()
    // clear all marks in the current node
    {
        getinformation();
        boardPosition.undonode(Pos.content());
        Pos.content().actions().removeIf((Action a) -> {
            return (a.type().equals(Action.Type.MARK) || a.type().equals(Action.Type.LABEL)
                    || a.type().equals(Field.Marker.CROSS.value) || a.type().equals(Field.Marker.SQUARE.value)
                    || a.type().equals(Action.Type.SELECT) || a.type().equals(Field.Marker.CIRCLE.value)
                    || a.type().equals(Field.Marker.TRIANGLE.value) || a.type().equals(Action.Type.LABEL));
        });
        boardPosition.act(Pos.content());
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    public void clearremovals ()
    // undo all removals in the current node
    {
        if (!Pos.children().isEmpty()) return;
        getinformation();
        boardPosition.undonode(Pos.content());
        Pos.content().actions().removeIf((Action a) -> {
            return (a.type().equals(Action.Type.ADD_BLACK) || a.type().equals(Action.Type.ADD_WHITE) || a.type().equals(Action.Type.ADD_EMPTY));
        });
        boardPosition.act(Pos.content());
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    // *************** change the game tree ***********

    public void insertnode ()
    // insert an empty node
    {
        if (Pos.haschildren()) return;
        Node n = new Node(Pos.content().number());
        Pos.insertchild(new Tree<Node>(n));
        n.main(Pos);
        getinformation();
        Pos = Pos.lastchild();
        boardPosition.setcurrent(Pos);
        stateChanged();
    }

    public void insertvariation ()
    // insert an empty variation to the current
    {
        if (Pos.parent() == null) return;
        getinformation();
        int c = boardPosition.color();
        back();
        Node n = new Node(2);
        Pos.addchild(new Tree<Node>(n));
        n.main(Pos);
        Pos = Pos.lastchild();
        boardPosition.setcurrent(Pos);
        boardPosition.color( -c);
        stateChanged();
    }

    public void undo (int n)
    // undo the n last moves, notify BoardInterface
    {
        varmaindown();
        for (int i = 0; i < n; i++)
        {
            goback();
            Pos.removeall();
        }
        stateChanged();
    }

    public void delete (int i, int j)
    // delete the stone and note it
    {
        if (boardPosition.color(i, j) == 0) return;
        synchronized (Pos)
        {
            Node n = Pos.content();
            if ((n.contains(Action.Type.BLACK) || n.contains(Action.Type.WHITE))) n = newnode();
            String field = Field.string(i, j);
            if (n.contains(Action.Type.ADD_BLACK, field))
            {
                boardPosition.undonode(Pos.content());
                n.toggleaction(new Action(Action.Type.ADD_BLACK, field));
                boardPosition.act(Pos.content());
                boardPosition.setcurrent(Pos);
            }
            else if (n.contains(Action.Type.ADD_WHITE, field))
            {
                boardPosition.undonode(Pos.content());
                n.toggleaction(new Action(Action.Type.ADD_WHITE, field));
                boardPosition.act(Pos.content());
                boardPosition.setcurrent(Pos);
            }
            else if (n.contains(Action.Type.BLACK, field))
            {
                boardPosition.undonode(Pos.content());
                n.toggleaction(new Action(Action.Type.BLACK, field));
                boardPosition.act(Pos.content());
                boardPosition.setcurrent(Pos);
            }
            else if (n.contains(Action.Type.WHITE, field))
            {
                boardPosition.undonode(Pos.content());
                n.toggleaction(new Action(Action.Type.WHITE, field));
                boardPosition.act(Pos.content());
                boardPosition.setcurrent(Pos);
            }
            else
            {
                Action a = new Action(Action.Type.ADD_EMPTY, field);
                n.expandaction(a);
                n.addchange(new Change(i, j, boardPosition.color(i, j)));
                boardPosition.color(i, j, 0);
            }
        }
        stateChanged();
    }

    public void changemove (int i, int j)
    // change a move to a new field (dangerous!)
    {
        if (boardPosition.color(i, j) != 0) return;
        synchronized (Pos)
        {
            for (Action a : Pos.content().actions())
            {
                if (a.type().equals("B") || a.type().equals("W"))
                {
                    boardPosition.undonode(Pos.content());
                    a.arguments().set(0, Field.string(i, j));
                    boardPosition.act(Pos.content());
                    boardPosition.setcurrent(Pos);
                    break;
                }
            }
        }
        stateChanged();
    }

    void movemouse (int i, int j)
    // set a move at i,j
    {
        if (hasChildren()) return;
        if (boardPosition.captured == 1 && boardPosition.capturei == i && boardPosition.capturej == j) return;
        set(i, j); // try to set a new move
        stateChanged();
    }

    public void specialmark (int i, int j)
    // Emphasize with the SpecialMarker
    {
        Node n = Pos.content();
        Action.Type s = SpecialMarker.value;
        Action a = new Action(s, Field.string(i, j));
        n.toggleaction(a);
        stateChanged();
    }

    public void markterritory (int i, int j, int color)
    {
        Action a;
        if (color > 0) {
            a = new Action(Action.Type.BLACK_TERRITORY, Field.string(i, j));
        }
        else {
            a = new Action(Action.Type.WHITE_TERRITORY, Field.string(i, j));
        }
        Pos.content().expandaction(a);
        stateChanged();
    }

    public void textmark (int i, int j)
    {
        Action a = new Action(Action.Type.LABEL, Field.string(i, j) + ":" + TextMarker);
        Pos.content().expandaction(a);
        stateChanged();
    }

    public void letter (int i, int j)
    // Write a character to the field at i,j
    {
        Action a = new LabelAction(Field.string(i, j));
        Pos.content().toggleaction(a);
        stateChanged();
    }

    public Node newnode ()
    {
        Node n = new Node(++number);
        Tree<Node> newpos = new Tree<Node>(n);
        Pos.addchild(newpos); // note the move
        n.main(Pos);
        Pos = newpos; // update current position pointerAction a;
        boardPosition.setcurrent(Pos);
        stateChanged();
        return n;
    }

    public void set (int i, int j, int c)
    // set a new stone, if the board position is empty
    // and we are on the last node.
    {
        if (Pos.haschildren()) return;
        setc(i, j, c);
    }

    public void setc (int i, int j, int c)
    {

        Action a;
        if (boardPosition.color(i, j) == 0) // empty?
        {
            Node n = Pos.content();
            if ((n.contains(Action.Type.BLACK) || n.contains(Action.Type.WHITE))) n = newnode();
            n.addchange(new Change(i, j, 0));
            if (c > 0)
            {
                a = new Action(Action.Type.ADD_BLACK, Field.string(i, j));
            }
            else
            {
                a = new Action(Action.Type.ADD_WHITE, Field.string(i, j));
            }
            n.expandaction(a); // note the move action
            boardPosition.color(i, j, c);
        }
        stateChanged();
    }

    public String done ()
    // count territory and return result string
    // notifies BoardInterface
    {
        if (Pos.haschildren()) return null;
        clearmarks();
        getinformation();
        int i, j;
        int tb = 0, tw = 0, sb = 0, sw = 0;
        boardPosition.getterritory();
        for (i = 0; i < S; i++)
            for (j = 0; j < S; j++)
            {
                if (boardPosition.territory(i, j) == 1 || boardPosition.territory(i, j) == -1)
                {
                    markterritory(i, j, boardPosition.territory(i, j));
                    if (boardPosition.territory(i, j) > 0)
                        tb++;
                    else tw++;
                }
                else
                {
                    if (boardPosition.color(i, j) > 0)
                        sb++;
                    else if (boardPosition.color(i, j) < 0) sw++;
                }
            }
        String s = Global.resourceString("Chinese_count_") + "\n"
                + Global.resourceString("Black__") + (sb + tb)
                + Global.resourceString("__White__") + (sw + tw) + "\n"
                + Global.resourceString("Japanese_count_") + "\n"
                + Global.resourceString("Black__") + (boardPosition.Pw + tb)
                + Global.resourceString("__White__") + (boardPosition.Pb + tw);
        return s;
    }

    public String docount ()
    // maka a local count and return result string
    {
        clearmarks();
        getinformation();
        int i, j;
        int tb = 0, tw = 0, sb = 0, sw = 0;
        boardPosition.getterritory();
        for (i = 0; i < S; i++)
            for (j = 0; j < S; j++)
            {
                if (boardPosition.territory(i, j) == 1 || boardPosition.territory(i, j) == -1)
                {
                    markterritory(i, j, boardPosition.territory(i, j));
                    if (boardPosition.territory(i, j) > 0)
                        tb++;
                    else tw++;
                }
                else
                {
                    if (boardPosition.color(i, j) > 0)
                        sb++;
                    else if (boardPosition.color(i, j) < 0) sw++;
                }
            }
        return Global.resourceString("Chinese_count_") + "\n"
                + Global.resourceString("Black__") + (sb + tb)
                + Global.resourceString("__White__") + (sw + tw) + "\n"
                + Global.resourceString("Japanese_count_") + "\n"
                + Global.resourceString("Black__") + (boardPosition.Pw + tb)
                + Global.resourceString("__White__") + (boardPosition.Pb + tw);
    }

    public void load (BufferedReader in) throws IOException
    // load a game from the stream
    {
        Vector v = SGFTree.load(in);
        synchronized (this)
        {
            if (v.size() == 0) return;
            T = (SGFTree)v.elementAt(0);
            resettree();
            boardPosition.act(Pos.content());
            boardPosition.setcurrent(Pos);
        }
        stateChanged();
    }

    public void loadXml (XmlReader xml) throws XmlReaderException
    // load a game from the stream
    {
        Vector v = SGFTree.load(xml);
        synchronized (this)
        {
            if (v.size() == 0) return;
            T = (SGFTree)v.elementAt(0);
            resettree();
            boardPosition.act(Pos.content());
            boardPosition.setcurrent(Pos);
        }
        stateChanged();
    }

    public void save (PrintWriter o)
    // saves the file to the specified print stream
    // in SGF
    {
        getinformation();
        T.top().content().setaction(Action.Type.APPLICATION, "Jago:" + Global.version(), true);
        T.top().content().setaction(Action.Type.SIZE, "" + S, true);
        T.top().content().setaction(Action.Type.GAME_TYPE, "1", true);
        T.top().content().setaction(Action.Type.FILE_FORMAT, "4", true);
            ((SGFTree)T).print(o);
    }

    public void savePos (PrintWriter o)
    // saves the file to the specified print stream
    // in SGF
    {
        getinformation();
        Node n = new Node(0);
        positionToNode(n);
        o.println("(");
        n.print(o);
        o.println(")");
    }

    public void saveXML (PrintWriter o, String encoding)
    // save the file in Jago's XML format
    {
        getinformation();
        T.top().content().setaction(Action.Type.APPLICATION, "Jago:" + Global.version(), true);
        T.top().content().setaction(Action.Type.SIZE, "" + S, true);
        T.top().content().setaction(Action.Type.GAME_TYPE, "1", true);
        T.top().content().setaction(Action.Type.FILE_FORMAT, "4", true);
        XmlWriter xml = new XmlWriter(o);
        xml.printEncoding(encoding);
        xml.printXls("go.xsl");
        xml.printDoctype("Go", "go.dtd");
        xml.startTagNewLine("Go");
        T.printXML(xml);
        xml.endTagNewLine("Go");
    }

    public void saveXMLPos (PrintWriter o, String encoding)
    // save the file in Jago's XML format
    {
        getinformation();
        T.top().content().setaction(Action.Type.APPLICATION, "Jago:" + Global.version(), true);
        T.top().content().setaction(Action.Type.SIZE, "" + S, true);
        T.top().content().setaction(Action.Type.GAME_TYPE, "1", true);
        T.top().content().setaction(Action.Type.FILE_FORMAT,"4", true);
        XmlWriter xml = new XmlWriter(o);
        xml.printEncoding(encoding);
        xml.printXls("go.xsl");
        xml.printDoctype("Go", "go.dtd");
        xml.startTagNewLine("Go");
        Node n = new Node(0);
        positionToNode(n);
        SGFTree t = new SGFTree(n);
        t.printXML(xml);
        xml.endTagNewLine("Go");
    }

    private boolean ishand (int i)
    {
        if (S > 13)
        {
            return i == 3 || i == S - 4 || i == S / 2;
        }
        else if (S > 9)
        {
            return i == 3 || i == S - 4;
        }
        else return false;
    }

    public void asciisave (PrintWriter o)
    // an ASCII image of the board.
    {
        int i, j;
        o.println(T.top().content().getaction(Action.Type.GAME_NAME));
        o.print("      ");
        for (i = 0; i < S; i++)
        {
            char a;
            if (i <= 7)
                a = (char)('A' + i);
            else a = (char)('A' + i + 1);
            o.print(" " + a);
        }
        o.println();
        o.print("      ");
        for (i = 0; i < S; i++)
            o.print("--");
        o.println("-");
        for (i = 0; i < S; i++)
        {
            o.print("  ");
            if (S - i < 10)
                o.print(" " + (S - i));
            else o.print(S - i);
            o.print(" |");
            for (j = 0; j < S; j++)
            {
                switch (boardPosition.color(j, i))
                {
                    case 1:
                        o.print(" #");
                        break;
                    case -1:
                        o.print(" O");
                        break;
                    case 0:
                        if (boardPosition.haslabel(j, i))
                            o.print(" " + boardPosition.label(j, i));
                        else if (boardPosition.letter(j, i) > 0)
                            o.print(" " + (char)(boardPosition.letter(j, i) + 'a' - 1));
                        else if (boardPosition.marker(j, i) != Field.Marker.NONE)
                            o.print(" X");
                        else if (ishand(i) && ishand(j))
                            o.print(" ,");
                        else o.print(" .");
                        break;
                }
            }
            o.print(" | ");
            if (S - i < 10)
                o.print(" " + (S - i));
            else o.print(S - i);
            o.println();
        }
        o.print("      ");
        for (i = 0; i < S; i++)
            o.print("--");
        o.println("-");
        o.print("      ");
        for (i = 0; i < S; i++)
        {
            char a;
            if (i <= 7)
                a = (char)('A' + i);
            else a = (char)('A' + i + 1);
            o.print(" " + a);
        }
        o.println();
    }

    private void positionToNode (Node n)
    // copy the current position to a node.
    {
        n.setaction(Action.Type.APPLICATION, "Jago:" + Global.version(), true);
        n.setaction(Action.Type.SIZE, "" + S, true);
        n.setaction(Action.Type.GAME_TYPE, "1", true);
        n.setaction(Action.Type.FILE_FORMAT, "4", true);
        n.copyAction(T.top().content(), Action.Type.GAME_NAME);
        n.copyAction(T.top().content(), Action.Type.DATE);
        n.copyAction(T.top().content(), Action.Type.BLACK_PLAYER_NAME);
        n.copyAction(T.top().content(), Action.Type.BLACK_PLAYER_RANK);
        n.copyAction(T.top().content(), Action.Type.WHITE_PLAYER_NAME);
        n.copyAction(T.top().content(), Action.Type.WHITE_PLAYER_RANK);
        n.copyAction(T.top().content(), Action.Type.USER);
        n.copyAction(T.top().content(), Action.Type.COPYRIGHT);
        int i, j;
        for (i = 0; i < S; i++)
        {
            for (j = 0; j < S; j++)
            {
                String field = Field.string(i, j);
                switch (boardPosition.color(i, j))
                {
                    case 1:
                        n.expandaction(new Action(Action.Type.ADD_BLACK, field));
                        break;
                    case -1:
                        n.expandaction(new Action(Action.Type.ADD_WHITE, field));
                        break;
                }
                if (boardPosition.marker(i, j) != Field.Marker.NONE)
                {
                    switch (boardPosition.marker(i, j))
                    {
                        case SQUARE:
                            n.expandaction(new Action(Field.Marker.SQUARE.value, field));
                            break;
                        case TRIANGLE:
                            n.expandaction(new Action(Field.Marker.TRIANGLE.value, field));
                            break;
                        case CIRCLE:
                            n.expandaction(new Action(Field.Marker.CIRCLE.value, field));
                            break;
                        default:
                            n.expandaction(new MarkAction(field));
                    }
                }
                else if (boardPosition.haslabel(i, j))
                    n.expandaction(new Action(Action.Type.LABEL, field + ":" + boardPosition.label(i, j)));
                else if (boardPosition.letter(i, j) > 0)
                    n.expandaction(new Action(Action.Type.LABEL, field + ":" + boardPosition.letter(i, j)));
            }
        }
    }

    /**
     * Search the string as substring of a comment, go to that node and report
     * success. On failure this routine will go up to the root node.
     */
    public boolean search (String s)
    {
        getinformation();
        Tree<Node> pos = Pos;
        boolean found = true;
        outer: while (!Pos.content().getaction(Action.Type.COMMENT).contains(s) || Pos == pos)
        {
            if ( !Pos.haschildren())
            {
                while (Pos.parent() == null || Pos.parent().nextchild(pos) == null)
                {
                    if (Pos.parent() == null)
                    {
                        found = false;
                        break outer;
                    }
                    else goback();
                }
                tovarright();
            }
            else goforward();
        }
        return found;
    }

    public void set (int i, int j)
    // set a new move, if the board position is empty
    {
        Action a;
        if (boardPosition.color(i, j) == 0) // empty?
        {
            if (Pos.content().actions() != null || Pos.parent() == null)
            // create a new node, if there the current node is not
            // empty, else use the current node. exception is the first
            // move, where we always create a new move.
            {
                Node n = new Node(++number);
                if (boardPosition.color() > 0)
                {
                    a = new Action(Action.Type.BLACK, Field.string(i, j));
                }
                else
                {
                    a = new Action(Action.Type.WHITE, Field.string(i, j));
                }
                n.addaction(a); // note the move action
                boardPosition.setaction(n, a, boardPosition.color()); // display the action
                // !!! We alow for suicide moves
                Tree<Node> newpos = new Tree<Node>(n);
                Pos.addchild(newpos); // note the move
                n.main(Pos);
                Pos = newpos; // update current position pointer
            }
            else
            {
                Node n = Pos.content();
                if (boardPosition.color() > 0)
                {
                    a = new Action(Action.Type.BLACK, Field.string(i, j));
                }
                else
                {
                    a = new Action(Action.Type.WHITE, Field.string(i, j));
                }
                n.addaction(a); // note the move action
                boardPosition.setaction(n, a, boardPosition.color()); // display the action
                // !!! We alow for suicide moves
            }
        }
        stateChanged();
    }

    public void addcomment (String s)
    // add a string to the comments, notifies comment area
    {
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        String Added = "";
        outer: while (true)
        {
            for (Action a : p.content().actions())
            {
                if (a.type().equals(Action.Type.COMMENT))
                {
                    String larg = a.arguments().get(0);
                    if (larg.isEmpty())
                    {
                        a.arguments().set(0, s);
                        Added = s;
                    }
                    else
                    {
                        a.arguments().set(0, larg + "\n" + s);
                        Added = "\n" + s;
                    }
                    break outer;
                }
            }
            p.content().addaction(new Action(Action.Type.COMMENT, s));
            break;
        }
        stateChanged();
    }

    private String twodigits (int n)
    {
        if (n < 10)
            return "0" + n;
        else return "" + n;
    }

    private String formtime (int t)
    {
        int s, m, h = t / 3600;
        if (h >= 1)
        {
            t = t - 3600 * h;
            m = t / 60;
            s = t - 60 * m;
            return "" + h + ":" + twodigits(m) + ":" + twodigits(s);
        }
        else
        {
            m = t / 60;
            s = t - 60 * m;
            return "" + m + ":" + twodigits(s);
        }
    }

    public String lookuptime (Action.Type type)
    {
        int t;
        if (Pos.parent() != null)
        {
            String s = Pos.parent().content().getaction(type);
            if ( !s.equals(""))
            {
                try
                {
                    t = Integer.parseInt(s);
                    return formtime(t);
                }
                catch (Exception e)
                {
                    return "";
                }
            }
            else return "";
        }
        return "";
    }

    void setname (String s)
    // set the name of the node
    {
        Pos.content().setaction(Action.Type.NAME, s, true);
    }

    public String extraInformation ()
    // get a mixture from handicap, komi and prisoners
    {
        StringBuffer b = new StringBuffer(Global.resourceString("_("));
        Node n = T.top().content();
        if (n.contains(Action.Type.HANDICAP))
        {
            b.append(Global.resourceString("Ha_"));
            b.append(n.getaction(Action.Type.HANDICAP));
        }
        if (n.contains(Action.Type.KOMI))
        {
            b.append(Global.resourceString("__Ko"));
            b.append(n.getaction(Action.Type.KOMI));
        }
        b.append(Global.resourceString("__B"));
        b.append("" + boardPosition.Pw);
        b.append(Global.resourceString("__W"));
        b.append("" + boardPosition.Pb);
        b.append(Global.resourceString("_)"));
        return b.toString();
    }

    public void setinformation (String black, String blackrank, String white,
                                String whiterank, String komi, String handicap)
    // set various things like names, rank etc.
    {
        T.top().content().setaction(Action.Type.BLACK_PLAYER_NAME, black, true);
        T.top().content().setaction(Action.Type.BLACK_PLAYER_RANK, blackrank, true);
        T.top().content().setaction(Action.Type.WHITE_PLAYER_NAME, white, true);
        T.top().content().setaction(Action.Type.WHITE_PLAYER_RANK, whiterank, true);
        T.top().content().setaction(Action.Type.KOMI, komi, true);
        T.top().content().setaction(Action.Type.HANDICAP, handicap, true);
        T.top().content().setaction(Action.Type.GAME_NAME, white + "-" + black, true);
        T.top().content().setaction(Action.Type.DATE, new Date().toString());
    }

    public int siblings ()
    {
        return Pos.parent().children().size()-1;
    }

    public boolean hasChildren() {
        return children() > 0;
    }

    public int children ()
    {
        return Pos.children().size();
    }

    public boolean ismain ()
    {
        return !Pos.haschildren() && Pos.content().main();
    }

    Node firstnode ()
    {
        return T.top().content();
    }

    public boolean canfinish ()
    {
        return !Pos.haschildren() && Pos.content().main();
    }

    String getname ()
    // get node name
    {
        return T.top().content().getaction(Action.Type.NAME);
    }

    public String getKomi ()
    // get Komi string
    {
        return T.top().content().getaction(Action.Type.KOMI);
    }

    boolean hasParent() {
        return Pos.parent() != null;
    }

    Tree<Node> current() {
        return Pos;
    }

    public Position getBoardPosition() {
        return boardPosition;
    }

    @Override
    public void addStateChangedHandler(@NotNull StateChangedHandler stateChangedHandler) {
        stateChangedHandlers.add(stateChangedHandler);
    }

    @Override
    public void stateChanged() {
        stateChangedHandlers.forEach(s -> s.stateChanged());
    }
}
