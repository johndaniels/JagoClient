package jagoclient.board;

import jagoclient.Global;
import rene.util.list.Tree;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlReaderException;
import rene.util.xml.XmlWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

public class BoardState {
    int sendi = -1, sendj;
    Position boardPosition;
    private int lasti = -1, lastj = 0; // last move (used to highlight the move)
    private int S;
    public int Pw,Pb; // changes in prisoners in this node
    int captured = 0, capturei, capturej;
    int Range;
    Tree<Node> Pos; // the current board position in this presentation
    private SGFTree T; // the game tree
    int number; // number of the next move
    private String TextMarker = "A";
    String comment;
    private Field.Marker SpecialMarker = Field.Marker.SQUARE;

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

    public void setsize (int s)
    // set the board size
    // clears the board !!!
    {
        if (s < 5 || s > 59) return;
        S = s;
        boardPosition = new Position(S);
        number = 1;
        Node n = new Node(number);
        n.main(true);
        lasti = lastj = -1;
        T = new SGFTree(n);
        Pos = T.top();
    }

    public void setaction (Node n, Action a, int c)
    // interpret a set move action, update the last move marker,
    // c being the color of the move.
    {
        String s = a.argument();
        int i = Field.i(s);
        int j = Field.j(s);
        if ( !valid(i, j)) return;
        n.addchange(new Change(i, j, boardPosition.color(i, j), boardPosition.number(i, j)));
        boardPosition.color(i, j, c);
        boardPosition.number(i, j, n.number() - 1);
        boardPosition.update(lasti, lastj);
        lasti = i;
        lastj = j;
        boardPosition.update(i, j);
        boardPosition.color( -c);
        capture(i, j, n);
    }

    public void capture (int i, int j, Node n)
    // capture neighboring groups without liberties
    // capture own group on suicide
    {
        int c = -boardPosition.color(i, j);
        captured = 0;
        if (i > 0) capturegroup(i - 1, j, c, n);
        if (j > 0) capturegroup(i, j - 1, c, n);
        if (i < S - 1) capturegroup(i + 1, j, c, n);
        if (j < S - 1) capturegroup(i, j + 1, c, n);
        if (boardPosition.color(i, j) == -c)
        {
            capturegroup(i, j, -c, n);
        }
        if (captured == 1 && boardPosition.count(i, j) != 1) captured = 0;
    }

    /**
     * Used by capture to determine the state of the groupt at (i,j)
     * Remove it, if it has no liberties and note the removals
     * as actions in the current node.
     */
    public void capturegroup (int i, int j, int c, Node n)
    // Used by capture to determine the state of the groupt at (i,j)
    // Remove it, if it has no liberties and note the removals
    // as actions in the current node.
    {
        int ii, jj;
        Action a;
        if (boardPosition.color(i, j) != c) return;
        if ( !boardPosition.markgrouptest(i, j, 0)) // liberties?
        {
            for (ii = 0; ii < S; ii++)
                for (jj = 0; jj < S; jj++)
                {
                    if (boardPosition.marked(ii, jj))
                    {
                        n.addchange(new Change(ii, jj, boardPosition.color(ii, jj), boardPosition
                                .number(ii, jj)));
                        if (boardPosition.color(ii, jj) > 0)
                        {
                            Pb++;
                            n.Pb++;
                        }
                        else
                        {
                            Pw++;
                            n.Pw++;
                        }
                        boardPosition.color(ii, jj, 0);
                        boardPosition.update(ii, jj); // redraw the field (offscreen)
                        captured++;
                        capturei = ii;
                        capturej = jj;
                    }
                }
        }
    }

    void gotovariation (int i, int j)
    // goto the variation at (i,j)
    {
        Tree<Node> newpos = boardPosition.tree(i, j);
        getinformation();
        if (newpos.parent() == Pos.parent())
        {
            goback();
            Pos = newpos;
            act(Pos.content());
            setcurrent(Pos.content());
        }
        else if (newpos.parent() == Pos)
        {
            Pos = newpos;
            act(Pos.content());
            setcurrent(Pos.content());
        }
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
            boardPosition.update(i, j);
        }
    }

    void doundo (Tree<Node> pos1)
    {
        if (pos1 != Pos) return;
        if (Pos.parent() == null)
        {
            undonode();
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
    }

    void goback ()
    // go one move back
    {
        if (Pos.parent() == null) return;
        undonode();
        Pos = Pos.parent();
        setcurrent(Pos.content());
    }

    void goforward ()
    // go one move forward
    {
        if ( !Pos.haschildren()) return;
        Pos = Pos.firstchild();
        act(Pos.content());
        setcurrent(Pos.content());
    }

    public void setcurrent (Node n)
    // update the last move marker applying all
    // set move actions in the node
    {
        String s;
        int i = lasti, j = lastj;
        lasti = -1;
        lastj = -1;
        boardPosition.update(i, j);
        for (Action a : n.actions())
        {
            if (a.type().equals("B") || a.type().equals("W"))
            {
                s = a.argument();
                i = Field.i(s);
                j = Field.j(s);
                if (valid(i, j))
                {
                    lasti = i;
                    lastj = j;
                    boardPosition.update(lasti, lastj);
                    boardPosition.color( -boardPosition.color(i, j));
                }
            }
        }
        number = n.number();
    }

    void gotoMove (int move)
    {
        while (number <= move && Pos.firstchild() != null)
        {
            goforward();
        }
    }

    void tovarright ()
    {
        if (Pos.parent() == null) return;
        if (Pos.parent().previouschild(Pos) == null) return;
        Tree<Node> newpos = nextchild(Pos);
        goback();
        Pos = newpos;
        act(Pos.content());
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

    public void undonode ()
    // Undo everything that has been changed in the node.
    // (This will not correct the last move marker!)
    {
        Node n = Pos.content();
        clearrange();
        Iterator<Change> i = n.changes().descendingIterator();
        while (i.hasNext())
        {
            Change c = i.next();
            boardPosition.color(c.I, c.J, c.C);
            boardPosition.number(c.I, c.J, c.N);
            boardPosition.update(c.I, c.J);
        }
        n.clearchanges();
        Pw -= n.Pw;
        Pb -= n.Pb;
    }

    public void act (Node n)
    // interpret all actions of a node
    {
        if (n.actions().isEmpty()) return;
        String s;
        int i, j;
        for (Action a : n.actions())
        {

        }
        n.clearchanges();
        n.Pw = n.Pb = 0;
        for (Action a : n.actions())
        {
            if (a.type().equals("B"))
            {
                setaction(n, a, 1);
            }
            else if (a.type().equals("W"))
            {
                setaction(n, a, -1);
            }
            if (a.type().equals("AB"))
            {
                placeaction(n, a, 1);
            }
            if (a.type().equals("AW"))
            {
                placeaction(n, a, -1);
            }
            else if (a.type().equals("AE"))
            {
                emptyaction(n, a);
            }
        }
    }

    public void placeaction (Node n, Action a, int c)
    // interpret a set move action, update the last move marker,
    // c being the color of the move.
    {
        int i, j;
        for (String s : a.arguments())
        {
            i = Field.i(s);
            j = Field.j(s);
            if (valid(i, j))
            {
                n.addchange(new Change(i, j, boardPosition.color(i, j), boardPosition.number(i, j)));
                boardPosition.color(i, j, c);
                boardPosition.update(i, j);
            }
        }
    }

    public void emptyaction (Node n, Action a)
    // interpret a remove stone action
    {
        int i, j, r = 1;
        for (String s : a.arguments())
        {
            i = Field.i(s);
            j = Field.j(s);
            if (valid(i, j))
            {
                n.addchange(new Change(i, j, boardPosition.color(i, j), boardPosition.number(i, j)));
                if (boardPosition.color(i, j) < 0)
                {
                    n.Pw++;
                    Pw++;
                }
                else if (boardPosition.color(i, j) > 0)
                {
                    n.Pb++;
                    Pb++;
                }
                boardPosition.color(i, j, 0);
                boardPosition.update(i, j);
            }
        }
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
        captured = 0;
    }

    void resettree ()
    {
        Pos = T.top();
        boardPosition = new Position(S);
        lasti = lastj = -1;
        Pb = Pw = 0;
        boardPosition.updateall();
    }

    public void clearrange ()
    {
        if (Range == -1) return;
        Range = -1;
        boardPosition.updateall();
    }

    boolean valid (int i, int j)
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
        act(Pos.content());
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
        act(Pos.content());
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
    }

    void gotonext ()
    // goto next named node
    {
        getinformation();
        goforward();
        while (Pos.content().getaction("N").equals(""))
        {
            if ( !Pos.haschildren()) break;
            goforward();
        }
    }

    void gotoprevious ()
    // gotoprevious named node
    {
        getinformation();
        goback();
        while (Pos.content().getaction("N").equals(""))
        {
            if (Pos.parent() == null) break;
            goback();
        }
    }

    void allback ()
    // to top of tree
    {
        getinformation();
        while (Pos.parent() != null)
            goback();
    }

    void allforward ()
    // to end of variation
    {
        getinformation();
        while (Pos.haschildren())
            goforward();
    }

    private void getinformation ()
    // update the comment, when leaving the position
    {
        clearsend();
        for (Action a : Pos.content().actions())
        {
            if (a.type().equals("C"))
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
        if ( !s.equals(""))
        {
            Pos.content().addaction(new Action("C", s));
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
            boardPosition.update(i, sendj);
        }
    }

    public synchronized void black (int i, int j)
    // white move at i,j at the end of the main tree
    {
        if (i < 0 || j < 0 || i >= S || j >= S) return;
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Action a = new Action("B", Field.string(i, j));
        Node n = new Node(p.content().number() + 1);
        n.addaction(a);
        p.addchild(new Tree<Node>(n));
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
        Action a = new Action("W", Field.string(i, j));
        Node n = new Node(p.content().number() + 1);
        n.addaction(a);
        p.addchild(new Tree<Node>(n));
        n.main(p);
        if (Pos == p) forward();
    }

    public synchronized void setblack (int i, int j)
    // set a white stone at i,j at the end of the main tree
    {
        if (i < 0 || j < 0 || i >= S || j >= S) return;
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Action a = new Action("AB", Field.string(i, j));
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
    }

    public synchronized void setwhite (int i, int j)
    // set a white stone at i,j at the end of the main tree
    {
        if (i < 0 || j < 0 || i >= S || j >= S) return;
        Tree<Node> p = T.top();
        while (p.haschildren())
            p = p.firstchild();
        Action a = new Action("AW", Field.string(i, j));
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
        setcurrent(Pos.content());
        captured = 0;
    }

    public synchronized void remove (int i0, int j0)
    // completely remove a group there
    {
        varmaindown();
        if (boardPosition.color(i0, j0) == 0) return;
        Action a;
        boardPosition.markgroup(i0, j0);
        int i, j;
        int c = boardPosition.color(i0, j0);
        Node n = Pos.content();
        if ((n.contains("B") || n.contains("W"))) {
            n = newnode();
        }
        for (i = 0; i < S; i++)
            for (j = 0; j < S; j++)
            {
                if (boardPosition.marked(i, j))
                {
                    a = new Action("AE", Field.string(i, j));
                    n
                            .addchange(new Change(i, j, boardPosition.color(i, j), boardPosition.number(i,
                                    j)));
                    n.expandaction(a);
                    if (boardPosition.color(i, j) > 0)
                    {
                        n.Pb++;
                        Pb++;
                    }
                    else
                    {
                        n.Pw++;
                        Pw++;
                    }
                    boardPosition.color(i, j, 0);
                    boardPosition.update(i, j);
                }
            }
    }

    // ************ change the current node *****************

    public void clearmarks ()
    // clear all marks in the current node
    {
        getinformation();
        undonode();
        Pos.content().actions().removeIf((Action a) -> {
            return (a.type().equals("M") || a.type().equals("L")
                    || a.type().equals(Field.Marker.CROSS.value) || a.type().equals(Field.Marker.SQUARE.value)
                    || a.type().equals("SL") || a.type().equals(Field.Marker.CIRCLE.value)
                    || a.type().equals(Field.Marker.TRIANGLE.value) || a.type().equals("LB"));
        });
        act(Pos.content());
    }

    public void clearremovals ()
    // undo all removals in the current node
    {
        if (!Pos.children().isEmpty()) return;
        getinformation();
        undonode();
        Pos.content().actions().removeIf((Action a) -> {
            return (a.type().equals("AB") || a.type().equals("AW") || a.type().equals("AE"));
        });
        act(Pos.content());
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
        setcurrent(Pos.content());
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
        setcurrent(Pos.content());
        boardPosition.color( -c);
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
    }

    public void delete (int i, int j)
    // delete the stone and note it
    {
        if (boardPosition.color(i, j) == 0) return;
        synchronized (Pos)
        {
            Node n = Pos.content();
            if ((n.contains("B") || n.contains("W"))) n = newnode();
            String field = Field.string(i, j);
            if (n.contains("AB", field))
            {
                undonode();
                n.toggleaction(new Action("AB", field));
                act(Pos.content());
            }
            else if (n.contains("AW", field))
            {
                undonode();
                n.toggleaction(new Action("AW", field));
                act(Pos.content());
            }
            else if (n.contains("B", field))
            {
                undonode();
                n.toggleaction(new Action("B", field));
                act(Pos.content());
            }
            else if (n.contains("W", field))
            {
                undonode();
                n.toggleaction(new Action("W", field));
                act(Pos.content());
            }
            else
            {
                Action a = new Action("AE", field);
                n.expandaction(a);
                n.addchange(new Change(i, j, boardPosition.color(i, j)));
                boardPosition.color(i, j, 0);
                boardPosition.update(i, j);
            }
        }
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
                    undonode();
                    a.arguments().set(0, Field.string(i, j));
                    act(Pos.content());
                    break;
                }
            }
        }
    }

    public void removegroup (int i0, int j0)
    // completely remove a group (at end of game, before count)
    // note all removals
    {
        if (Pos.haschildren()) return;
        if (boardPosition.color(i0, j0) == 0) return;
        Action a;
        boardPosition.markgroup(i0, j0);
        int i, j;
        int c = boardPosition.color(i0, j0);
        Node n = Pos.content();
        if (n.contains("B") || n.contains("W")) n = newnode();
        for (i = 0; i < S; i++)
            for (j = 0; j < S; j++)
            {
                if (boardPosition.marked(i, j))
                {
                    a = new Action("AE", Field.string(i, j));
                    n
                            .addchange(new Change(i, j, boardPosition.color(i, j), boardPosition.number(i,
                                    j)));
                    n.expandaction(a);
                    if (boardPosition.color(i, j) > 0)
                    {
                        n.Pb++;
                        Pb++;
                    }
                    else
                    {
                        n.Pw++;
                        Pw++;
                    }
                    boardPosition.color(i, j, 0);
                }
            }
    }

    public void specialmark (int i, int j)
    // Emphasize with the SpecialMarker
    {
        Node n = Pos.content();
        String s = SpecialMarker.value;
        Action a = new Action(s, Field.string(i, j));
        n.toggleaction(a);
        boardPosition.update(i, j);
    }

    public void markterritory (int i, int j, int color)
    {
        Action a;
        if (color > 0)
            a = new Action("TB", Field.string(i, j));
        else a = new Action("TW", Field.string(i, j));
        Pos.content().expandaction(a);
        boardPosition.update(i, j);
    }

    public void textmark (int i, int j)
    {
        Action a = new Action("LB", Field.string(i, j) + ":" + TextMarker);
        Pos.content().expandaction(a);
        boardPosition.update(i, j);
    }

    public void letter (int i, int j)
    // Write a character to the field at i,j
    {
        Action a = new LabelAction(Field.string(i, j));
        Pos.content().toggleaction(a);
        boardPosition.update(i, j);
    }

    public Node newnode ()
    {
        Node n = new Node(++number);
        Tree<Node> newpos = new Tree<Node>(n);
        Pos.addchild(newpos); // note the move
        n.main(Pos);
        Pos = newpos; // update current position pointerAction a;
        setcurrent(Pos.content());
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
            if ((n.contains("B") || n.contains("W"))) n = newnode();
            n.addchange(new Change(i, j, 0));
            if (c > 0)
            {
                a = new Action("AB", Field.string(i, j));
            }
            else
            {
                a = new Action("AW", Field.string(i, j));
            }
            n.expandaction(a); // note the move action
            boardPosition.color(i, j, c);
            boardPosition.update(i, j);
        }
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
                + Global.resourceString("Black__") + (Pw + tb)
                + Global.resourceString("__White__") + (Pb + tw);
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
                + Global.resourceString("Black__") + (Pw + tb)
                + Global.resourceString("__White__") + (Pb + tw);
    }

    public void load (BufferedReader in) throws IOException
    // load a game from the stream
    {
        Vector v = SGFTree.load(in);
        synchronized (this)
        {
            if (v.size() == 0) return;
            boardPosition.update(lasti, lastj);
            lasti = lastj = -1;
            T = (SGFTree)v.elementAt(0);
            resettree();
            act(Pos.content());
        }
    }

    public void loadXml (XmlReader xml) throws XmlReaderException
    // load a game from the stream
    {
        Vector v = SGFTree.load(xml);
        synchronized (this)
        {
            if (v.size() == 0) return;
            boardPosition.update(lasti, lastj);
            lasti = lastj = -1;
            T = (SGFTree)v.elementAt(0);
            resettree();
            act(Pos.content());
        }
    }

    public void save (PrintWriter o)
    // saves the file to the specified print stream
    // in SGF
    {
        getinformation();
        T.top().content().setaction("AP", "Jago:" + Global.version(), true);
        T.top().content().setaction("SZ", "" + S, true);
        T.top().content().setaction("GM", "1", true);
        T.top().content().setaction("FF", "4", true);
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
        T.top().content().setaction("AP", "Jago:" + Global.version(), true);
        T.top().content().setaction("SZ", "" + S, true);
        T.top().content().setaction("GM", "1", true);
        T.top().content().setaction("FF", "4", true);
        XmlWriter xml = new XmlWriter(o);
        xml.printEncoding(encoding);
        xml.printXls("go.xsl");
        xml.printDoctype("Go", "go.dtd");
        xml.startTagNewLine("Go");
        ((SGFTree)T).printXML(xml);
        xml.endTagNewLine("Go");
    }

    public void saveXMLPos (PrintWriter o, String encoding)
    // save the file in Jago's XML format
    {
        getinformation();
        T.top().content().setaction("AP", "Jago:" + Global.version(), true);
        T.top().content().setaction("SZ", "" + S, true);
        T.top().content().setaction("GM", "1", true);
        T.top().content().setaction("FF","4", true);
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

    boolean ishand (int i)
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
        o.println(T.top().content().getaction("GN"));
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

    public void positionToNode (Node n)
    // copy the current position to a node.
    {
        n.setaction("AP", "Jago:" + Global.version(), true);
        n.setaction("SZ", "" + S, true);
        n.setaction("GM", "1", true);
        n.setaction("FF", "4", true);
        n.copyAction(T.top().content(), "GN");
        n.copyAction(T.top().content(), "DT");
        n.copyAction(T.top().content(), "PB");
        n.copyAction(T.top().content(), "BR");
        n.copyAction(T.top().content(), "PW");
        n.copyAction(T.top().content(), "WR");
        n.copyAction(T.top().content(), "PW");
        n.copyAction(T.top().content(), "US");
        n.copyAction(T.top().content(), "CP");
        int i, j;
        for (i = 0; i < S; i++)
        {
            for (j = 0; j < S; j++)
            {
                String field = Field.string(i, j);
                switch (boardPosition.color(i, j))
                {
                    case 1:
                        n.expandaction(new Action("AB", field));
                        break;
                    case -1:
                        n.expandaction(new Action("AW", field));
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
                    n.expandaction(new Action("LB", field + ":" + boardPosition.label(i, j)));
                else if (boardPosition.letter(i, j) > 0)
                    n.expandaction(new Action("LB", field + ":" + boardPosition.letter(i, j)));
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
        outer: while (!Pos.content().getaction("C").contains(s) || Pos == pos)
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
                    a = new Action("B", Field.string(i, j));
                }
                else
                {
                    a = new Action("W", Field.string(i, j));
                }
                n.addaction(a); // note the move action
                setaction(n, a, boardPosition.color()); // display the action
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
                    a = new Action("B", Field.string(i, j));
                }
                else
                {
                    a = new Action("W", Field.string(i, j));
                }
                n.addaction(a); // note the move action
                setaction(n, a, boardPosition.color()); // display the action
                // !!! We alow for suicide moves
            }
        }
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
                if (a.type().equals("C"))
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
            p.content().addaction(new Action("C", s));
            break;
        }
    }

    public String twodigits (int n)
    {
        if (n < 10)
            return "0" + n;
        else return "" + n;
    }

    public String formtime (int t)
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

    public String lookuptime (String type)
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
        Pos.content().setaction("N", s, true);
    }

    public String extraInformation ()
    // get a mixture from handicap, komi and prisoners
    {
        StringBuffer b = new StringBuffer(Global.resourceString("_("));
        Node n = T.top().content();
        if (n.contains("HA"))
        {
            b.append(Global.resourceString("Ha_"));
            b.append(n.getaction("HA"));
        }
        if (n.contains("KM"))
        {
            b.append(Global.resourceString("__Ko"));
            b.append(n.getaction("KM"));
        }
        b.append(Global.resourceString("__B"));
        b.append("" + Pw);
        b.append(Global.resourceString("__W"));
        b.append("" + Pb);
        b.append(Global.resourceString("_)"));
        return b.toString();
    }

    public void setinformation (String black, String blackrank, String white,
                                String whiterank, String komi, String handicap)
    // set various things like names, rank etc.
    {
        T.top().content().setaction("PB", black, true);
        T.top().content().setaction("BR", blackrank, true);
        T.top().content().setaction("PW", white, true);
        T.top().content().setaction("WR", whiterank, true);
        T.top().content().setaction("KM", komi, true);
        T.top().content().setaction("HA", handicap, true);
        T.top().content().setaction("GN", white + "-" + black, true);
        T.top().content().setaction("DT", new Date().toString());
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
        return T.top().content().getaction("N");
    }

    public String getKomi ()
    // get Komi string
    {
        return T.top().content().getaction("KM");
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
}
