package jagoclient.igs;

import jagoclient.Global;
import jagoclient.board.*;
import jagoclient.gui.*;
import jagoclient.igs.connection.ConnectionState;
import jagoclient.igs.games.Color;
import jagoclient.igs.games.GameUpdatesHandler;
import jagoclient.igs.games.TimeInfo;
import rene.gui.IconBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class IgsGamePanel extends JPanel implements KeyListener, GameUpdatesHandler {
    private OutputLabel L, Lm; // For board informations

    private boolean TimerInTitle, ExtraSendField, BigTimer;
    private HistoryTextField SendField;
    private OutputLabel TL;
    private BigTimerLabel BL;
    protected Menu FileMenu, Options;
    private JPanel CommentPanel;
    private TextArea AllComments;
    CardLayout CommentPanelLayout;
    protected CheckboxMenuItem KibitzWindow, Teaching, Silent;
    private Board board;
    TextArea Comment; // For comments
    IconBar IB;
    JPanel ButtonP;
    IgsConnection connection;


    public IgsGamePanel(int game, ConnectionState connectionState) {
        //this.connection = ;
        this.connection.addGameUpdateHandler(game, this);
        setLayout(new BorderLayout());
        TimerInTitle = Global.getParameter("timerintitle", true);
        ExtraSendField = Global.getParameter("extrasendfield", true);
        BigTimer = Global.getParameter("bigtimer", true);
        // Menu
        Silent.setState(Global.getParameter("boardsilent", false));
        if (Silent.getState()) Global.Silent++;
        Menu mc = new MyMenu(Global.resourceString("Coordinates"));
        CommentPanel = new MyPanel();

        AllComments = new MyTextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
        AllComments.setEditable(false);
        CommentPanel.setLayout(CommentPanelLayout = new CardLayout());
        CommentPanel.add("Comment", Comment);
        CommentPanel.add("AllComments", AllComments);
        CommentPanel.setLayout(new BorderLayout());
        CommentPanel.add("Center", Comment);
        GameViewerState gameViewerState = new GameViewerState(19);
        board = new Board(gameViewerState);
        JPanel BP = new MyPanel(new BorderLayout());
        BP.add("Center", board);
        // Add the label
        JPanel bpp = new MyPanel(new GridLayout(0, 1));
        SimplePanel sp = new SimplePanel(L, 80, Lm, 20);
        bpp.add(sp);
        // Add the timer label
        if ( !TimerInTitle)
        {
            SimplePanel btl = new SimplePanel(TL = new OutputLabel(Global.resourceString("Start")), 80, new OutputLabel(""), 20);
            bpp.add(btl);
        }
        BP.add("South", bpp);
        // Text Area
        JPanel cp = new MyPanel(new BorderLayout());
        cp.add("Center", CommentPanel);
        CommentPanelLayout.show(CommentPanel, "AllComments");
        // Add the extra send field
        SendField = new HistoryTextField((s) -> {}, "SendField");
        cp.add("South", SendField);
        JPanel bcp = new BoardCommentPanel(new Panel3D(BP), new Panel3D(cp), board);
        add("Center", bcp);
        // Add the big timer label
        if (BigTimer)
        {
            BL = new BigTimerLabel();
            cp.add("North", BL);
        }
        // Navigation panel
        IB = createIconBar();
        ButtonP = new Panel3D(IB);
        if (Global.getParameter("showbuttonsconnected", true))
            add("South", ButtonP);
        // Directory for FileDialog
        validate();
        board.addKeyListener(this);
    }

    public IconBar createIconBar ()
    {
        IconBar I = new IconBar(SwingUtilities.windowForComponent(this));
        I.Resource = "/icons/";
        I.addLeft("undo");
        I.addSeparatorLeft();
        I.addLeft("allback");
        I.addLeft("fastback");
        I.addLeft("back");
        I.addLeft("forward");
        I.addLeft("fastforward");
        I.addLeft("allforward");
        I.addSeparatorLeft();
        I.addLeft("variationback");
        I.addLeft("variationstart");
        I.addLeft("variationforward");
        I.addLeft("main");
        I.addLeft("mainend");
        I.addSeparatorLeft();
        I.addLeft("send");
        return I;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void move(Color color, int x, int y) {

    }

    @Override
    public void time(TimeInfo time) {

    }
}
