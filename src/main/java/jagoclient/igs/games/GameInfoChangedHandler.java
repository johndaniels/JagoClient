package jagoclient.igs.games;


import java.util.List;

public interface GameInfoChangedHandler {
    void gameInfosChanged(List<GameInfo> gameInfoList);
}
