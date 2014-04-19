package sexy.fairly.smartwatch.game2048;


import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class Game {

    private final InsertCellCallback mCallback;
    private Grid mGrid;
    private int mScore;
    private boolean mGameRunning;
    private boolean mGameWon;

    public Game(InsertCellCallback callback) {
        mCallback = callback;
        newGame();
    }

    public void newGame() {
        mScore = 0;
        mGrid = new Grid(4);
        addRandomTile();
        addRandomTile();
        mGameWon = false;
        mGameRunning = true;
    }

    public void setGrid(int[][] cells) {
        mGrid = new Grid(cells);
        gameOverCheck();
    }

    public boolean isGameRunning() {
        return mGameRunning;
    }

    public boolean isGameWon() {
        return mGameWon;
    }

    public Grid getGrid() {
        return mGrid;
    }

    private void addRandomTile() {
        Cell cell = mGrid.getAvailableRandomCell();
        int value = Math.random() < 0.9 ? 2 : 4;
        mGrid.insert(cell, value);
    }

    public void move(Direction direction) {
        if (!mGameRunning) {
            return;
        }

        System.out.println("Move: " + direction);
        boolean moved = false;

        Vector vector = getVector(direction);
        Pair<int[], int[]> traversals = getTraversals(vector);

        prepareTiles();

        for (int x : traversals.first) {
            for (int y : traversals.second) {

                Cell cell = new Cell(x, y);
                Tile tile = mGrid.valueAt(cell);

                if (tile != null) {
                    Pair<Cell, Cell> position = findFarthestPosition(x, y, vector);

                    Tile next = mGrid.valueAt(position.second);

                    if (next != null && next.value == tile.value && next.mergedFrom == null) {
                        Cell posNext = position.second;
                        Tile merged = new Tile(posNext.x, posNext.y, tile.value * 2);
                        merged.mergedFrom = new Pair<Tile, Tile>(tile, next);

                        mGrid.insertTile(merged);
                        mGrid.removeTile(tile);

                        tile.x = posNext.x;
                        tile.y = posNext.y;

                        mScore += merged.value;

                        if (merged.value == 2048) {
                            mGameRunning = false;
                            mGameWon = true;
                            System.out.println("You won!");
                        }

                    } else {
                        mGrid.moveTile(tile, position.first);
                    }

                    if (tile.x != x || tile.y != y) {
                        moved = true;
                    }
                }
            }
        }

        if (moved) {
            if (mCallback != null) {
                mCallback.insertCell();
            } else {
                addRandomTile();
            }

            gameOverCheck();
        }
    }

    private void gameOverCheck() {
        if (!movesAvailable()) {
            mGameRunning = false;
            mGameWon = false;
            System.out.println("You lose!");
        }
    }

    public void insertTile() {
        addRandomTile();
        gameOverCheck();
    }

    private boolean movesAvailable() {
        int gridSize = mGrid.getSize();
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int value = mGrid.valueAt(x, y);
                if (value == Grid.EMPTY_CELL) {
                    return true;
                }

                if (x + 1 < gridSize) {
                    int right = mGrid.valueAt(x + 1, y);
                    if (right == Grid.EMPTY_CELL || value == right) {
                        return true;
                    }
                }

                if (y + 1 < gridSize) {
                    int bottom = mGrid.valueAt(x, y + 1);
                    if (bottom == Grid.EMPTY_CELL || value == bottom) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void prepareTiles() {
        for (int y = 0; y < mGrid.getSize(); y++) {
            for (int x = 0; x < mGrid.getSize(); x++) {
                Tile tile = mGrid.valueAt(new Cell(x, y));
                if (tile != null) {
                    tile.mergedFrom = null;
                }
            }
        }
    }

    private Pair<Cell, Cell> findFarthestPosition(int x, int y, Vector vector) {
        Cell previous = new Cell(x, y);
        Cell cell = new Cell(x, y);

        do {
            previous.x = cell.x;
            previous.y = cell.y;

            cell.x = previous.x + vector.x;
            cell.y = previous.y + vector.y;
        } while (mGrid.isWithinBounds(cell) && mGrid.valueAt(cell) == null);

        return new Pair<Cell, Cell>(previous, cell);
    }

    private Pair<int[], int[]> getTraversals(Vector vector) {
        int[] horizontal = new int[mGrid.getSize()];
        int[] vertical = new int[mGrid.getSize()];

        int end = mGrid.getSize() - 1;
        for (int i = 0; i < mGrid.getSize(); i++) {
            if (vector.x == 1) {
                horizontal[i] = end - i;
            } else {
                horizontal[i] = i;
            }
            if (vector.y == 1) {
                vertical[i] = end - i;
            } else {
                vertical[i] = i;
            }
        }

        return new Pair<int[], int[]>(horizontal, vertical);
    }

    private static final Map<Direction, Vector> VECTOR_MAP = new HashMap<Direction, Vector>();
    static {
        VECTOR_MAP.put(Direction.UP, new Vector(0, -1));
        VECTOR_MAP.put(Direction.RIGHT, new Vector(1, 0));
        VECTOR_MAP.put(Direction.DOWN, new Vector(0, 1));
        VECTOR_MAP.put(Direction.LEFT, new Vector(-1, 0));
    }

    private Vector getVector(Direction direction) {
        return VECTOR_MAP.get(direction);
    }

    public void setScore(int score) {
        mScore = score;
    }

    public static enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    public static class Vector {
        public int x;
        public int y;

        public Vector(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public int getScore() {
        return mScore;
    }

    public static interface InsertCellCallback {
        void insertCell();
    }
}
