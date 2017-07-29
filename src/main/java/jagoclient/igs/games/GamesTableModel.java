package jagoclient.igs.games;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class GamesTableModel extends AbstractTableModel{

    List<GameInfo> games;
    GamesTableModel(List<GameInfo> games) {
        this.games = games;
    }

    @Override
    public int getRowCount() {
        return games.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        GameInfo game = games.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return game.getGameNumber();
            case 1:
                return game.getWhitePlayer();
            case 2:
                return game.getBlackPlayer();
        }
        return null;
    }

    public GameInfo getGame(int index) {
        return games.get(index);
    }
}
