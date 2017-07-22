package jagoclient.igs;

import jagoclient.Global;
import jagoclient.igs.games.GameInfo;
import jagoclient.igs.users.UserInfo;
import jagoclient.sound.JagoSound;
import rene.util.parser.StringParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by johndaniels on 7/19/17.
 */
public class IgsConnection {
    private static final Logger LOG = Logger.getLogger(IgsStream.class.getName());

    Map<Integer, List<IgsStream.LineResult>> linesSeen = new HashMap<>();

    CompletableFuture<List<UserInfo>> pendingUserList;
    CompletableFuture<List<GameInfo>> pendingGameList;

    List<Distributor> distributorList;
    IgsStream igsStream;
    public IgsConnection(IgsStream igsStream) {
        distributorList = new ArrayList<Distributor>();
        this.igsStream = igsStream;
        this.igsStream.addLineListener(this::handleLine);
    }

    /**
     * Processes the input, until there is a raw, which is not suited for the
     * specified distributor.
     */
    /*void sendall (Distributor dis) throws IOException
    {
        while (true)
        {
            IgsStream.LineResult lineResult = readlineprim();
            LOG.log(Level.INFO, "IGS:({0} for {1}) {2}", new Object[]{lineResult.getNumber(), dis.number(), lineResult.getCommand()});
            if (lineResult.getNumber() == dis.number())
                dis.send(lineResult.getCommand());
            else if (lineResult.getNumber() == 9) // information got in other content
            {
                Distributor dist = findDistributor(9);
                if (dist != null)
                {
                    LOG.info("Sending information");
                    dist.send(lineResult.getCommand());
                    // sendall(dist); // logical error
                }
            }
            else
            {
                if (dis.sizetask() != null || dis.task() != null)
                {
                    unchain(dis);
                    if (dis.sizetask() != null)
                        dis.sizetask().sizefinished();
                    if (dis.task() != null)
                        dis.task().finished();
                    LOG.log(Level.INFO, "Distributor {0} finished", dis.number());
                }
                break;
            }
        }
        LOG.log(Level.INFO, "sendall() for {0} finished", dis.number());
        dis.allsended();
    }

    /**
     * Same as above, but the distrubutor get the input with the String
     * prepended.
     */
    /*
    void sendall (String s, Distributor dis) throws IOException
    {
        while (true)
        {
            IgsStream.LineResult lineResult = readlineprim();
            LOG.log(Level.INFO, "IGS: {0}", lineResult.getCommand());
            if (lineResult.getNumber() == 11)
                dis.send(s + lineResult.getCommand());
            else
            {
                if (dis.sizetask() != null || dis.task() != null)
                {
                    unchain(dis);
                    if (dis.sizetask() != null)
                        dis.sizetask().sizefinished();
                    if (dis.task() != null)
                        dis.task().finished();
                }
                break;
            }
        }
        dis.allsended();
    }*/

    public Distributor findDistributor (int n, int g)
    {
        synchronized (distributorList)
        {
            for (Distributor dis : distributorList)
            {
                if (dis.number() == n)
                {
                    if (dis.game() == g) return dis;
                }
            }
            return null;
        }
    }

    public Distributor findDistributor (int n)
    {
        for (Distributor dis : distributorList)
        {
            if (dis.number() == n) return dis;
        }
        return null;
    }

    /**
     * Chains a new distributor to the distributor list.
     */
    public void append (Distributor dis)
    {
        distributorList.add(dis);
    }

    /**
     * Seeks a game distributor, which waits for that game.
     */
    public boolean gamewaiting (int g)
    {
        for (Distributor dis : distributorList)
        {
            if (dis.number() == 15)
            {
                if (dis.game() == g) return true;
            }
        }
        return false;
    }

    public void unchain (Distributor dis)
    {
        distributorList.remove(dis);
    }

    /**
     * Removes all distributors from the distributor list.
     */
    public void removeall ()
    {
        distributorList.clear();
    }

    public void handleLine(IgsStream.LineResult lineResult) {
        int number = lineResult.getNumber();
        String command = lineResult.getCommand();
        if (number == 1) {
            if (linesSeen.size() > 0) {
                //userlist response
                if (linesSeen.containsKey(42)) {
                    List<UserInfo> whoObjects = new ArrayList<UserInfo>();
                    for (IgsStream.LineResult l : linesSeen.get(42)) {
                        if (l.getCommand().startsWith("Name")) {
                            continue; // This is the header line.
                        }
                        whoObjects.add(new UserInfo(l.getCommand()));
                    }
                    if (pendingUserList != null) {
                        pendingUserList.complete(whoObjects);
                        pendingUserList = null;
                    }
                }
                // gamelist response
                if (linesSeen.containsKey(87)) {
                    List<GameInfo> gameInfos = new ArrayList<>();
                    for (IgsStream.LineResult l : linesSeen.get(87)) {
                        if (l.getCommand().startsWith("[##]")) {
                            continue; // This is the header line.
                        }
                        gameInfos.add(new GameInfo(l.getCommand()));
                    }
                    if (pendingGameList != null) {
                        pendingGameList.complete(gameInfos);
                        pendingGameList = null;
                    }
                }
                linesSeen.clear();
            }
        } else {
            if (!linesSeen.containsKey(lineResult.getNumber())) {
                linesSeen.put(lineResult.getNumber(), new ArrayList<>());
            }
            linesSeen.get(lineResult.getNumber()).add(lineResult);
        }
        StringParser sp;
        if (number == 21
                && (command.startsWith("{Game") || command
                .startsWith("{ Game")))
        {
            sp = new StringParser(command);
            sp.skip("{");
            sp.skipblanks();
            sp.skip("Game");
            sp.skipblanks();
            if ( !sp.isint()) {
                return;
            }
            int G = sp.parseint(':');
            Distributor dis = findDistributor(15, G);
            if (dis != null)
            {
                LOG.log(Level.INFO, "Sending comment to game {0}", G);
                dis.send(command);
                return;
            }
            if (Global.getParameter("reducedoutput", true)
                    && !Global.posfilter(command))
            {
                return;
            }
        }
        else if (number == 21 && command.startsWith("{")
                && Global.getParameter("reducedoutput", true)
                && !Global.posfilter(command))
        {
            return;
        }
        else if (number == 21 && Global.posfilter(command))
        {
            JagoSound.play("message", "wip", true);
        }
        else if (number == 11 && command.startsWith("Kibitz"))
        {
            sp = new StringParser(command);
            sp.upto('[');
            if (sp.error()) {
                return;
            }
            sp.upto(']');
            if (sp.error()) {
                return;
            }
            sp.upto('[');
            if (sp.error()) {
                return;
            }
            sp.skip("[");
            sp.skipblanks();
            if ( !sp.isint()) {
                return;
            }
            int G = sp.parseint(']');
            Distributor dis = findDistributor(15, G);
            if (dis != null)
            {
                LOG.log(Level.INFO, "Sending kibitz to game {0}", G);
                dis.send(command);
                //sendall("Kibitz-> ", dis);
                return;
            }
        }
        else if (number == 9 && command.startsWith("Removing @"))
        {
            sp = new StringParser(command);
            sp.skip("Removing @");
            Distributor dis = findDistributor(15);
            if (dis != null)
            {
                LOG.log(Level.INFO, "Got {0}", command);
                dis.send(command);
                //sendall(dis);
            }
            return;
        }
        else if (number == 9 && !command.startsWith("File"))
        {
            Distributor dis = findDistributor(9);
            if (dis != null)
            {
                LOG.info("Sending information");
                dis.send(command);
                //sendall(dis);
                LOG.info("End of information");
                return;
            }
        }
        else if (number == 15 && command.startsWith("Game"))
        {
            sp = new StringParser(command);
            sp.skip("Game");
            sp.skipblanks();
            if ( !sp.isint()) {
                return;
            }
            int G = sp.parseint();
            Distributor dis = findDistributor(15, G);
            if (dis != null)
            {
                LOG.log(Level.INFO, "Sending to game {0}", G);
                dis.send(command);
                //sendall(dis);
                return;
            }
            dis = findDistributor(15, -1);
            if (dis != null)
            {
                LOG.log(Level.INFO, "Game {0} started", G);
                dis.game(G);
                dis.send(command);
                //sendall(dis);
            }
            return;
        }
        else if (number == 32)
        {
            sp = new StringParser(command);
            int G = sp.parseint(':');
            sp.skip(":");
            sp.skipblanks();
            if (G == 0)
            {
                number = 9;
                return;
            }
            Distributor dis = findDistributor(32, G);
            if (dis != null)
            {
                LOG.log(Level.INFO, "Sending to channel {0}", G);
                dis.send(sp.upto((char)0));
                return;
            }
            //ChannelDistributor cd = new ChannelDistributor(CF,
            //	this, Out, G);
            //cd.send(sp.upto((char)0));
            return;
        }
        else if (number == 22)
        {
						/*Distributor dis = findDistributor(22);
						if (dis != null)
						{
							dis.send(Command);
							sendall(dis);
							continue loop1;
						}
						IgsGoFrame gf = new IgsGoFrame(CF, "Peek game");
						gf.setVisible(true);
						gf.repaint();
						Status s = new Status(gf, this, Out);
						s.PD.send(Command);
						sendall(s.PD);*/
        }
        else if (number == 9 && command.startsWith("-- "))
        {
            Distributor dis = findDistributor(32);
            if (dis != null)
            {
                LOG.log(Level.INFO, "Sending to channel {0}", dis.game());
                dis.send(command);
            }
            return;
        }
        else if (number == 1
                && (command.startsWith("8") || command.startsWith("5") || command
                .startsWith("6")))
        {
            LOG.log(Level.INFO, "1 received {0}", command);
        }
        else if (number != 9)
        {
            Distributor dis = findDistributor(number);
            if (dis != null)
            {
                dis.send(command);
                //sendall(dis);
                return;
            }
            else {
                //LOG.log(Level.WARNING, "Distributor {0} not found", number);
            }
        }
    }

    public CompletionStage<List<UserInfo>> userList() {
        igsStream.out("userlist2");
        if (pendingUserList == null) {
            pendingUserList = new CompletableFuture<>();
        }
        return pendingUserList;
    }

    public CompletionStage<List<GameInfo>> gameList() {
        igsStream.out("gamelist3");
        if (pendingGameList == null) {
            pendingGameList = new CompletableFuture<>();
        }
        return pendingGameList;
    }

    public void addDistributor(Distributor distributor) {
        distributorList.add(distributor);
    }
}
