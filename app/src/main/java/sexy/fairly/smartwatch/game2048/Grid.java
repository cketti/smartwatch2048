package sexy.fairly.smartwatch.game2048;

import java.util.ArrayList;
import java.util.List;

public class Grid {
    public static final int EMPTY_CELL = 0;
    public Tile[][] mRows;

    public Grid(int size) {
        mRows = new Tile[size][];
        for (int i = 0; i < size; i++) {
            Tile[] row = new Tile[size];
            mRows[i] = row;
        }
        empty();
    }

    private void empty() {
        for (int y = 0; y < mRows.length; y++) {
            Tile[] row = mRows[y];
            for (int x = 0; x < row.length; x++) {
                row[x] = null;
            }
        }
    }

    private List<Cell> getAvailableRandomCells() {
        List<Cell> cells = new ArrayList<Cell>();
        for (int y = 0; y < mRows.length; y++) {
            Tile[] row = mRows[y];
            for (int x = 0; x < row.length; x++) {
                if (row[x] == null) {
                    cells.add(new Cell(x, y));
                }
            }
        }

        return cells;
    }

    public Cell getAvailableRandomCell() {
        List<Cell> cells = getAvailableRandomCells();
        return cells.get((int) (Math.random() * cells.size()));
    }

    public void insert(Cell cell, int value) {
        mRows[cell.y][cell.x] = new Tile(cell.x, cell.y, value);
    }

    public void insertTile(Tile tile) {
        mRows[tile.y][tile.x]=  tile;
    }

    public void removeTile(Tile tile) {
        mRows[tile.y][tile.x] = null;
    }

    public Tile valueAt(Cell cell) {
        if (isWithinBounds(cell)) {
            return mRows[cell.y][cell.x];
        } else {
            return null;
        }
    }

    public int valueAt(int x, int y) {
        Tile tile = mRows[y][x];
        return (tile == null) ? EMPTY_CELL : tile.value;
    }

    public int getSize() {
        return mRows.length;
    }

    public boolean isWithinBounds(Cell cell) {
        return 0 <= cell.x && 0 <= cell.y && cell.x < getSize() && cell.y < getSize();
    }

    public void moveTile(Tile tile, Cell cell) {
        mRows[tile.y][tile.x] = null;
        mRows[cell.y][cell.x] = tile;
        tile.x = cell.x;
        tile.y = cell.y;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                int value = valueAt(x, y);
                sb.append(value);
                sb.append(", ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
