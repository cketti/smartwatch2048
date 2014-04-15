package sexy.fairly.smartwatch.game2048.storage;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import sexy.fairly.smartwatch.game2048.Grid;

@DatabaseTable(tableName = "state")
public class State implements Parcelable {
    public static final int DEFAULT_ID = 1;

    @DatabaseField(id = true)
    private int id = DEFAULT_ID;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private int[][] cells;


    public State() {}

    public State(Parcel in) {
        int size = in.readInt();
        cells = new int[size][];
        for (int i = 0; i < size; i++) {
            cells[i] = in.createIntArray();
        }
    }

    public State(Grid grid) {
        int size = grid.getSize();
        cells = new int[size][];

        for (int y = 0; y < size; y++) {
            cells[y] = new int[size];
            for (int x = 0; x < size; x++) {
                int cell = grid.valueAt(x, y);
                cells[y][x] = cell;
            }
        }
    }

    public int[][] getCells() {
        return cells;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(cells.length);
        for (int[] cell : cells) {
            dest.writeIntArray(cell);
        }
    }

    public static final Parcelable.Creator<State> CREATOR = new Parcelable.Creator<State>() {
        public State createFromParcel(Parcel in) {
            return new State(in);
        }

        public State[] newArray(int size) {
            return new State[size];
        }
    };
}
