package jagoclient.igs.users;

import jagoclient.Global;
import jagoclient.igs.connection.ConnectionState;
import rene.viewer.Lister;
import rene.viewer.SystemLister;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by johndaniels on 7/20/17.
 */
public class UsersPanel extends JPanel {
    ConnectionState connectionState;
    JTable whoLister;
    public UsersPanel(ConnectionState connectionState) {
        super();
        this.connectionState = connectionState;
        setLayout(new BorderLayout());
        whoLister = new JTable();
        whoLister.setFont(Global.Monospaced);
        JScrollPane scrollPane = new JScrollPane(whoLister);
        connectionState.addUsersChangedHandler(this::usersChanged);
        add("Center", scrollPane);
    }

    public void usersChanged(List<UserInfo> userInfos) {
        whoLister.setModel(new UsersTableModel(userInfos));
    }
}
