package jagoclient.igs.games;

public interface GameUpdatesHandler {
    void move(Color color, int x, int y);
    void time(TimeInfo time);
}
