package sexy.fairly.smartwatch.game2048.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "purchase_state")
public class PurchaseState {
    public static final int DEFAULT_ID = 1;

    @DatabaseField(id = true)
    private int id = DEFAULT_ID;

    @DatabaseField
    private boolean fullVersion;

    @DatabaseField
    private long timeStamp;

    public PurchaseState() {}

    public PurchaseState(boolean fullVersion) {
        this.fullVersion = fullVersion;
        this.timeStamp = System.currentTimeMillis();
    }

    public boolean isFullVersion() {
        return fullVersion;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
