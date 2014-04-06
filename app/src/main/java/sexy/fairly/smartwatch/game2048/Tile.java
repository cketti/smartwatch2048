package sexy.fairly.smartwatch.game2048;

import android.util.Pair;

public class Tile {
    public int x;
    public int y;
    public int value;

    public Cell previousPosition;
    public Pair<Tile, Tile> mergedFrom;

    public Tile(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }
}
