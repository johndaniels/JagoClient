package jagoclient.igs.games;

public enum Color {
    WHITE(-1),
    NONE(0),
    BLACK(1);
    int color;
    Color(int color) {
        this.color = color;
    }
    public int getColor() {
        return color;
    }
}
