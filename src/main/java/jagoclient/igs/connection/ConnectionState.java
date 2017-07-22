package jagoclient.igs.connection;

import jagoclient.igs.IgsConnection;
import jagoclient.igs.IgsStream;
import jagoclient.igs.games.GameInfo;
import jagoclient.igs.games.GameInfoChangedHandler;
import jagoclient.igs.users.UserInfo;
import jagoclient.igs.users.UsersChangedHandler;

import java.util.ArrayList;
import java.util.List;

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

    public void addUsersChangedHandler(UsersChangedHandler whoChangedHandler) {
        usersChangedHandlers.add(whoChangedHandler);
    }

    public void addGamesChangedHandler(GameInfoChangedHandler gameInfoChangedHandler) {
        gameInfoChangedHandlers.add(gameInfoChangedHandler);
    }

    private void onUsersChanged() {
        for (UsersChangedHandler handler : usersChangedHandlers) {
            handler.usersChanged(userInfos);
        }
    }

    private void onGameInfosChanged() {
        for (GameInfoChangedHandler handler : gameInfoChangedHandlers) {
            handler.gameInfosChanged(gameInfos);
        }
    }

    public ConnectionState(IgsStream stream, IgsConnection connection, Runnable onLogin) {
        this.onLogin = onLogin;
        this.stream = stream;
        this.connection = connection;
    }

    private void handleLogin() {
        onLogin.run();
    }

    public void login(String username, String password) {
        stream.login(username, password).thenAccept(aVoid -> handleLogin());
    }

    public void userList() {
        connection.userList().thenAccept(newWhoObjects -> {
            this.userInfos = newWhoObjects;
            onUsersChanged();
        });
    }

    public void gameList() {
        connection.gameList().thenAccept(newGameInfoObjects -> {
            this.gameInfos = newGameInfoObjects;
            onGameInfosChanged();
        });
    }
}
