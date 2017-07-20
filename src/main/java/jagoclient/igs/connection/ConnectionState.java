package jagoclient.igs.connection;

import jagoclient.igs.Connect;
import jagoclient.igs.IgsStream;

import java.util.ArrayList;
import java.util.List;

/**
 * This contains all the known state about an open Connection to IGS.
 * All mutations to this class should happen on the UI thread.
 */
public class ConnectionState {
    IgsStream stream;
    Runnable onLogin;

    public ConnectionState(IgsStream stream, Runnable onLogin) {
        this.onLogin = onLogin;
        this.stream = stream;
    }

    private void handleLogin() {
        onLogin.run();
    }

    public void login(String username, String password) {
        stream.login(username, password).thenAccept(aVoid -> handleLogin());
    }
}
