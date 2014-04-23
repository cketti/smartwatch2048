package sexy.fairly.smartwatch.game2048.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "score")
public class BestScore {
    public static final int DEFAULT_ID = 1;

    @DatabaseField(id = true)
    private int id = DEFAULT_ID;

    @DatabaseField
    private int score;

    public BestScore() {}

    public BestScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
