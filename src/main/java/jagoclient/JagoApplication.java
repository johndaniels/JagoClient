package jagoclient;

import jagoclient.board.Board;
import jagoclient.board.GoFrame;
import jagoclient.board.WoodPaint;
import jagoclient.igs.ConnectionFrame;
import jagoclient.igs.IgsConnection;
import jagoclient.igs.IgsStream;
import jagoclient.igs.connection.ConnectionState;
import jagoclient.sound.JagoSound;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/**
 * Created by johndaniels on 7/19/17.
 */
public class JagoApplication {
    private MainFrame startFrame;
    private LoginDialog loginDialog;
    private Go go;
    private ConnectionState connectionState;
    IgsStream igsStream;
    IgsConnection igsConnection;
    ConnectionFrame connectionFrame;

    public void loginSuccess() {
        loginDialog.setVisible(false);
        connectionFrame.pack();
        connectionFrame.setVisible(true);
        connectionState.userList();
        connectionState.gameList();
    }

    public void tryLogin(String username, String password) {
        connectionState.login(username, password);
    }

    public void startLogin() {
        loginDialog.pack();
        loginDialog.setVisible(true);
    }

    public void run(String[] args) {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {}
        loginDialog = new LoginDialog(this::tryLogin);

        // scan arguments
        int na = 0;
        boolean homefound = false;
        String localgame = "";
        igsStream = new IgsStream();
        igsConnection = new IgsConnection(igsStream);
        connectionState = new ConnectionState(igsStream, igsConnection, this::loginSuccess);
        while (args.length > na)
        {
            if (args.length - na >= 2 && args[na].startsWith("-l"))
            {
                Locale.setDefault(new Locale(args[na + 1], ""));
                na += 2;
            }
            else if (args.length - na >= 2 && args[na].startsWith("-h"))
            {
                Global.home(args[na + 1]);
                na += 2;
                homefound = true;
            }
            else
            {
                localgame = args[na];
                na++;
            }
        }
        // initialize some Global things
        Global.setApplet(false);
        if ( !homefound) Global.home(System.getProperty("user.home"));
        Global.readparameter(".go.cfg"); // read setup
        Global.createfonts();
        connectionFrame = new ConnectionFrame("IGS", connectionState);
        // create a MainFrame
        startFrame = new MainFrame(Global.resourceString("_Jago_"));
        // add a go applet to it and initialize it
        startFrame.add("Center", go = new Go(this::startLogin));
        startFrame.setVisible(true);
        Global.loadmessagefilter(); // load message filters, if available
        JagoSound.play("high", "", true); // play a welcome sound
        if ( !localgame.equals("")) {
            openlocal(localgame);
            // open a SGF file, if there was a parameter
        }

    }

    private void openlocal (String file)
    {
        GoFrame gf = new GoFrame(new Frame(), "Local");
        gf.setVisible(true);
        gf.load(file);
    }

    /**
     * This is the main method for the JagoClient application. It is normally
     * envoced via "java Go".
     * <P>
     * It opens a frame of class MainFrame, initializes the parameter list and
     * starts a dump on request in the command line. If a game name is entered
     * in the command line, it will also open a GoFrame displaying the game.
     * <P>
     * An important point is that the application will write its setup to
     * go.cfg.
     * <P>
     * Available arguments are
     * <ul>
     * <li>-h sets the home directory for the application
     * <li>another argument opens a local SGF file immediately
     * </ul>
     */
    public static void main(String[] args) {
        new JagoApplication().run(args);
    }
}
