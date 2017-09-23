package jagoclient.igs.games;

import rene.util.parser.StringParser;

public class TimeInfo {
    Color player;
    int game;
    int initialTime;
    int remainingInitialTime;
    int initialByoYomi;
    int remainingByoYomi;
    int initialByoYomiStones;
    int remainingByoYomiStones;

    public TimeInfo(String timeCommand) {
        StringParser parser = new StringParser(timeCommand);
        parser.parseword(':');
        parser.next();
        game = parser.parseint();
        parser.next();
        String username = parser.parseword('(');
        char color = parser.next();
        player = color == 'W' ? Color.WHITE : Color.BLACK;
        parser.next();
        parser.next();
        parser.parseint();
        remainingInitialTime = parser.parseint('/');
        parser.next();
        initialTime = parser.parseint();
        remainingByoYomi = parser.parseint('/');
        parser.next();
        initialByoYomi = parser.parseint();
        remainingByoYomiStones = parser.parseint('/');
        parser.next();
        initialByoYomiStones = parser.parseint();


    }

    public int getInitialTime() {
        return initialTime;
    }

    public int getRemainingInitialTime() {
        return remainingInitialTime;
    }

    public int getInitialByoYomi() {
        return initialByoYomi;
    }

    public int getRemainingByoYomi() {
        return remainingByoYomi;
    }

    public int getInitialByoYomiStones() {
        return initialByoYomiStones;
    }

    public int getRemainingByoYomiStones() {
        return remainingByoYomiStones;
    }

    public Color getPlayer() {
        return player;
    }

    public int getGame() {
        return game;
    }
}
