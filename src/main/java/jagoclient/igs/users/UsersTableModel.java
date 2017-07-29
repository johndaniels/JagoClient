package jagoclient.igs.users;

import jagoclient.igs.games.GameInfo;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class UsersTableModel extends AbstractTableModel {
    List<UserInfo> users;
    UsersTableModel(List<UserInfo> users) {
        this.users = users;
    }

    @Override
    public int getRowCount() {
        return users.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        UserInfo game = users.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return game.getUsername();
            case 1:
                return game.getRank();
            case 2:
                return game.getCountry();
        }
        return null;
    }
}
