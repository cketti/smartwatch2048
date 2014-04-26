package sexy.fairly.smartwatch.game2048;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.SparseIntArray;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;

import java.util.concurrent.TimeUnit;

import sexy.fairly.smartwatch.game2048.storage.State;
import sexy.fairly.smartwatch.game2048.util.IabHelper;
import sexy.fairly.smartwatch.game2048.util.IabResult;
import sexy.fairly.smartwatch.game2048.util.Inventory;
import sexy.fairly.smartwatch.game2048.util.Purchase;

class GameControlSmartWatch2 extends ControlExtension {
    public static final int ACTION_PURCHASE_COMPLETE = 1;
    public static final int ACTION_PURCHASE_CANCELLED = 2;

    private static final long PURCHASE_STATE_TIMEOUT = TimeUnit.DAYS.toMillis(14);
    private static final long WINNING_TIMEOUT = 800;
    private static final long LOSING_TIMEOUT = 1500;
    private static final long NEW_TILE_TIMEOUT = 180;

    private static final int[][] FIELD_IDS = new int[][] {
        new int[] { R.id.field1_1, R.id.field1_2, R.id.field1_3, R.id.field1_4 },
        new int[] { R.id.field2_1, R.id.field2_2, R.id.field2_3, R.id.field2_4 },
        new int[] { R.id.field3_1, R.id.field3_2, R.id.field3_3, R.id.field3_4 },
        new int[] { R.id.field4_1, R.id.field4_2, R.id.field4_3, R.id.field4_4 },
    };

    private static final SparseIntArray IMAGES = new SparseIntArray();
    static {
        IMAGES.put(0, R.drawable.cell_empty);
        IMAGES.put(2, R.drawable.cell_2);
        IMAGES.put(4, R.drawable.cell_4);
        IMAGES.put(8, R.drawable.cell_8);
        IMAGES.put(16, R.drawable.cell_16);
        IMAGES.put(32, R.drawable.cell_32);
        IMAGES.put(64, R.drawable.cell_64);
        IMAGES.put(128, R.drawable.cell_128);
        IMAGES.put(256, R.drawable.cell_256);
        IMAGES.put(512, R.drawable.cell_512);
        IMAGES.put(1024, R.drawable.cell_1024);
        IMAGES.put(2048, R.drawable.cell_2048);
        IMAGES.put(4096, R.drawable.cell_4096);
        IMAGES.put(8192, R.drawable.cell_8192);
    }

    private static final int MENU_ITEM_0 = 0;
    private static final int MENU_ITEM_1 = 1;
    private GameState mLastGameState;
    private boolean mMoveInProgress = false;
    private IabHelper mHelper;

    public static enum GameState {
        LOADING,
        RUNNING,
        LOST_WAIT,
        LOST,
        WON_WAIT,
        WON,
        SCORE,
        FREE_LIMIT_REACHED,
        PURCHASE_IN_PROGRESS,
        PURCHASE_CANCELLED
    }

    private Bundle[] mMenuItems = new Bundle[2];

    private Handler mHandler;
    private Game mGame;
    private GameState mGameState;

    GameControlSmartWatch2(final String hostAppPackageName, final Context context,
            Handler handler) {
        super(context, hostAppPackageName);
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        mHandler = handler;

        initializeMenu();
        startIabHelper(context);
    }

    private void startIabHelper(Context context) {
        mHelper = new IabHelper(context, BuyGameActivity.LICENSE_PUBLIC_KEY);
        mHelper.enableDebugLogging(BuyGameActivity.DEBUG);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    return;
                }

                if (mHelper == null) return;

                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener =
            new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;

            if (result.isFailure()) {
                Intent intent = new Intent(mContext, PersistenceService.class);
                intent.setAction(PersistenceService.ACTION_READ_PURCHASE_STATE);
                intent.putExtra(PersistenceService.EXTRA_RESULT_RECEIVER,
                        new PurchaseStateResultReceiver(mHandler));
                mContext.startService(intent);

                return;
            }

            Purchase premiumPurchase = inventory.getPurchase(BuyGameActivity.SKU_FULL_VERSION);
            boolean fullVersion = (premiumPurchase != null &&
                    BuyGameActivity.verifyDeveloperPayload(premiumPurchase));

            if (fullVersion) {
                setFullVersion(true);
            }
        }
    };

    private void initializeMenu() {
        mMenuItems[0] = new Bundle();
        mMenuItems[0].putInt(Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_0);
        mMenuItems[0].putString(Control.Intents.EXTRA_MENU_ITEM_TEXT,
                mContext.getString(R.string.menu_new_game));

        mMenuItems[1] = new Bundle();
        mMenuItems[1].putInt(Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_1);
        mMenuItems[1].putString(Control.Intents.EXTRA_MENU_ITEM_TEXT,
                mContext.getString(R.string.menu_show_score));
    }

    public static int getSupportedControlWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_width);
    }

    public static int getSupportedControlHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_height);
    }

    @Override
    public void onStart() {
        mGame = new Game(new Game.InsertCellCallback() {
            @Override
            public void insertCell() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGame.insertTile();
                        updateGameState();
                        renderGame();

                        saveState();
                        mMoveInProgress = false;
                    }
                }, NEW_TILE_TIMEOUT);
            }
        });

        mGameState = GameState.LOADING;
        Intent intent = new Intent(mContext, PersistenceService.class);
        intent.setAction(PersistenceService.ACTION_READ);
        intent.putExtra(PersistenceService.EXTRA_RESULT_RECEIVER,
                new StateResultReceiver(mHandler));
        mContext.startService(intent);
    }

    @Override
    public void onDestroy() {
        mHandler = null;

        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    @Override
    public void onResume() {
        renderGame();
    }

    @Override
    public void onKey(final int action, final int keyCode, final long timeStamp) {
        if (action == Control.Intents.KEY_ACTION_RELEASE &&
                keyCode == Control.KeyCodes.KEYCODE_OPTIONS) {
            showMenu(mMenuItems);
        }
    }

    @Override
    public void onMenuItemSelected(final int menuItem) {
        switch (menuItem) {
            case MENU_ITEM_0: {
                startNewGame();
                break;
            }
            case MENU_ITEM_1: {
                showScore();
                break;
            }
        }
    }

    @Override
    public void onSwipe(int direction) {
        if (mGameState != GameState.RUNNING || mMoveInProgress) {
            return;
        }

        Game.Direction gameDirection;
        switch (direction) {
            case Control.Intents.SWIPE_DIRECTION_UP: {
                gameDirection = Game.Direction.UP;
                break;
            }
            case Control.Intents.SWIPE_DIRECTION_DOWN: {
                gameDirection = Game.Direction.DOWN;
                break;
            }
            case Control.Intents.SWIPE_DIRECTION_LEFT: {
                gameDirection = Game.Direction.LEFT;
                break;
            }
            case Control.Intents.SWIPE_DIRECTION_RIGHT: {
                gameDirection = Game.Direction.RIGHT;
                break;
            }
            default: {
                return;
            }
        }

        performMove(gameDirection);
    }

    private void performMove(Game.Direction direction) {
        mMoveInProgress = true;
        if (!mGame.move(direction)) {
            mMoveInProgress = false;
        }
        updateGameState();
        renderGame();
    }

    private void saveState() {
        int score = mGame.getScore();
        int bestScore = mGame.getBestScore();

        Intent intent = new Intent(mContext, PersistenceService.class);
        intent.setAction(PersistenceService.ACTION_SAVE);
        intent.putExtra(PersistenceService.EXTRA_GRID_STATE, new State(mGame.getGrid(), score));
        if (score == bestScore) {
            intent.putExtra(PersistenceService.EXTRA_BEST_SCORE, bestScore);
        }
        mContext.startService(intent);
    }

    private void updateGameState() {
        if (!mGame.isGameRunning()) {
            if (mGame.isGameWon()) {
                mGameState = GameState.WON_WAIT;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGameState = GameState.WON;
                        renderGame();
                    }
                }, WINNING_TIMEOUT);
            } else {
                mGameState = GameState.LOST_WAIT;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGameState = GameState.LOST;
                        renderGame();
                    }
                }, LOSING_TIMEOUT);
            }
        } else if (mGame.isFreeLimitReached()) {
            mGameState = GameState.FREE_LIMIT_REACHED;
        }
    }

    private void startNewGame() {
        mGame.newGame();
        mGameState = GameState.RUNNING;
        saveState();
        renderGame();
    }

    @Override
    public void onObjectClick(ControlObjectClickEvent event) {
        switch (event.getLayoutReference()) {
            case R.id.new_game: {
                startNewGame();
                break;
            }
            case R.id.continue_game: {
                mGameState = mLastGameState;
                renderGame();
                break;
            }
            case R.id.buy_game_button: {
                BuyGameActivity.show(mContext);

                mGameState = GameState.PURCHASE_IN_PROGRESS;
                renderGame();
                break;
            }
        }
    }

    private void renderGame() {
        switch (mGameState) {
            case LOADING: {
                showLayout(R.layout.waiting, null);
                break;
            }
            case RUNNING:
            case LOST_WAIT:
            case WON_WAIT: {
                renderGrid();
                break;
            }
            case WON: {
                showLayout(R.layout.you_won, null);
                break;
            }
            case LOST: {
                showLayout(R.layout.you_lost, null);
                break;
            }
            case SCORE: {
                renderScore();
                break;
            }
            case FREE_LIMIT_REACHED: {
                showLayout(R.layout.buy_game, null);
                break;
            }
            case PURCHASE_IN_PROGRESS: {
                showLayout(R.layout.buy_on_phone, null);
                break;
            }
            case PURCHASE_CANCELLED: {
                showLayout(R.layout.purchase_cancelled, null);
                break;
            }
        }
    }

    private void showScore() {
        mLastGameState = mGameState;
        mGameState = GameState.SCORE;
        renderGame();
    }

    private void renderScore() {
        Bundle[] data = new Bundle[2];
        data[0] = new Bundle();
        data[0].putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.score);
        data[0].putString(Control.Intents.EXTRA_TEXT,
                mContext.getString(R.string.score_format, mGame.getScore()));
        data[1] = new Bundle();
        data[1].putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.best);
        data[1].putString(Control.Intents.EXTRA_TEXT,
                mContext.getString(R.string.best_score_format, mGame.getBestScore()));

        showLayout(R.layout.score, data);
    }

    private void renderGrid() {
        Grid grid = mGame.getGrid();

        int bundleIndex = 0;
        Bundle[] data = new Bundle[grid.getSize() * grid.getSize()]; //FIXME
        for (int y = 0; y < grid.getSize(); y++) {
            for (int x = 0; x < grid.getSize(); x++) {
                int value = grid.valueAt(x, y);
                int fieldRes = FIELD_IDS[y][x];
                Bundle bundle = new Bundle();
                bundle.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, fieldRes);
                int imageRes = IMAGES.get(value);
                bundle.putString(Control.Intents.EXTRA_DATA_URI,
                        ExtensionUtils.getUriString(mContext, imageRes));
                data[bundleIndex++] = bundle;
            }
        }

        showLayout(R.layout.game_field, data);
    }

    public class StateResultReceiver extends ResultReceiver {

        public StateResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mGameState = GameState.RUNNING;

            State state = resultData.getParcelable(PersistenceService.EXTRA_GRID_STATE);
            if (state != null) {
                mGame.setGrid(state.getCells());
                mGame.setScore(state.getScore());

                int bestScore = resultData.getInt(PersistenceService.EXTRA_BEST_SCORE);
                mGame.setBestScore(bestScore);

                updateGameState();
            } else {
                saveState();
            }

            renderGame();
        }
    }

    public class PurchaseStateResultReceiver extends ResultReceiver {

        public PurchaseStateResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            boolean fullVersion =
                    resultData.getBoolean(PersistenceService.EXTRA_FULL_VERSION, false);
            long timeStamp = resultData.getLong(PersistenceService.EXTRA_TIMESTAMP, -1);

            if (fullVersion && System.currentTimeMillis() - timeStamp <= PURCHASE_STATE_TIMEOUT) {
                setFullVersion(false);
            }
            renderGame();
        }
    }

    @Override
    public void onDoAction(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case ACTION_PURCHASE_COMPLETE: {
                setFullVersion(false);
                break;
            }
            case ACTION_PURCHASE_CANCELLED: {
                mGameState = GameState.PURCHASE_CANCELLED;
                renderGame();
                break;
            }
        }
    }

    private void setFullVersion(boolean save) {
        mGame.setFullVersion(true);
        if (mGameState == GameState.FREE_LIMIT_REACHED ||
                mGameState == GameState.PURCHASE_IN_PROGRESS ||
                mGameState == GameState.PURCHASE_CANCELLED) {
            mGameState = GameState.RUNNING;
            updateGameState();
            renderGame();
        }

        if (save) {
            Intent intent = new Intent(mContext, PersistenceService.class);
            intent.setAction(PersistenceService.ACTION_SAVE_PURCHASE_STATE);
            intent.putExtra(PersistenceService.EXTRA_FULL_VERSION, true);
            mContext.startService(intent);
        }
    }
}
