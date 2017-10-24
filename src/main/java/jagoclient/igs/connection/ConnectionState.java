package jagoclient.igs.connection;

import jagoclient.board.BoardState;
import jagoclient.board.Position;
import jagoclient.igs.IgsConnection;
import jagoclient.igs.IgsStream;
import jagoclient.igs.games.GameInfo;
import jagoclient.igs.games.GameInfoChangedHandler;
import jagoclient.igs.users.UserInfo;
import jagoclient.igs.users.UsersChangedHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This contains all the known state about an open Connection to IGS.
 * All mutations to this class should happen on the UI thread.
 */
public class ConnectionState {
    IgsStream stream;
    IgsConnection connection;
    Runnable onLogin;
    List<UserInfo> userInfos = new ArrayList<>();
    List<GameInfo> gameInfos = new ArrayList<>();
    List<UsersChangedHandler> usersChangedHandlers = new ArrayList<>();
    List<GameInfoChangedHandler> gameInfoChangedHandlers = new ArrayList<>();
    Map<Integer, BoardState> gameStateMap = new HashMap<>();

    public void addUsersChangedHandler(UsersChangedHandler whoChangedHandler) {
        usersChangedHandlers.add(whoChangedHandler);
    }

    public void addGamesChangedHandler(GameInfoChangedHandler gameInfoChangedHandler) {
        gameInfoChangedHandlers.add(gameInfoChangedHandler);
    }

    private void onUsersChanged(List<UserInfo> userInfos) {
        this.userInfos = userInfos;
        for (UsersChangedHandler handler : usersChangedHandlers) {
            handler.usersChanged(userInfos);
        }
    }

    private void onGameInfosChanged(List<GameInfo> gameInfos) {
        this.gameInfos = gameInfos;
        for (GameInfoChangedHandler handler : gameInfoChangedHandlers) {
            handler.gameInfosChanged(gameInfos);
        }
    }

    public ConnectionState(IgsStream stream, IgsConnection connection, Runnable onLogin) {
        this.onLogin = onLogin;
        this.stream = stream;
        this.connection = connection;
        connection.addGameInfoChangedHandler(this::onGameInfosChanged);
        connection.addUsersChangedHandler(this::onUsersChanged);
    }

    private void handleLogin() {
        onLogin.run();
    }

    public void login(String username, String password) {
        stream.login(username, password).thenAccept(aVoid -> handleLogin());
    }

    public void userList() {
        connection.userList();
    }

    public void gameList() {
        connection.gameList();
    }

    public void observeGame(int gameNumber) {
        if (!gameStateMap.containsKey(gameNumber)) {
            //Position state = new Position();
            //connection.addGameUpdateHandler(gameNumber, state);
            //gameStateMap.put(gameNumber, state);
        }
        connection.observeGame(gameNumber);
    }
}
