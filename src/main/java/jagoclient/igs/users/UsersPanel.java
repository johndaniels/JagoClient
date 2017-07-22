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
    Lister whoLister;
    public UsersPanel(ConnectionState connectionState) {
        super();
        this.connectionState = connectionState;
        setLayout(new BorderLayout());
        whoLister = Global.getParameter("systemlister", false)?new SystemLister():new Lister();
        whoLister.setFont(Global.Monospaced);
        whoLister.setText(Global.resourceString("Loading"));
        connectionState.addUsersChangedHandler(this::usersChanged);
        add("Center", whoLister);
    }

    public void usersChanged(List<UserInfo> userInfos) {
        List<UserInfo> localUserInfos = new ArrayList<>(userInfos);
        Collections.sort(localUserInfos, (UserInfo a, UserInfo b) -> Integer.compare(a.getRankValue(), b.getRankValue()));
        whoLister.setText("");
        whoLister.appendLine0(Global
                .resourceString("_Info_______Name_______Idle___Rank"));
        Color FC = Color.green.darker(), CM = Color.red.darker();
        for (UserInfo who : localUserInfos)
        {
            whoLister.appendLine0(who.getUsername());
        }
        whoLister.doUpdate(false);
    }
}
