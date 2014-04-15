package sexy.fairly.smartwatch.game2048;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseIntArray;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;

import java.util.LinkedList;
import java.util.Queue;

class GameControlSmartWatch2 extends ControlExtension {
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
    private boolean mQueueMove = false;

    public static enum GameState {
        RUNNING,
        LOST_WAIT,
        LOST,
        WON_WAIT,
        WON,
        SCORE
    }

    private Bundle[] mMenuItems = new Bundle[2];

    private Handler mHandler;
    private Game mGame;
    private GameState mGameState;
    private Queue<Game.Direction> mMoveQueue = new LinkedList<Game.Direction>();

    GameControlSmartWatch2(final String hostAppPackageName, final Context context,
            Handler handler) {
        super(context, hostAppPackageName);
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        mHandler = handler;

        initializeMenu();
    }

    private void initializeMenu() {
        mMenuItems[0] = new Bundle();
        mMenuItems[0].putInt(Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_0);
        mMenuItems[0].putString(Control.Intents.EXTRA_MENU_ITEM_TEXT, "New game");

        mMenuItems[1] = new Bundle();
        mMenuItems[1].putInt(Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_1);
        mMenuItems[1].putString(Control.Intents.EXTRA_MENU_ITEM_TEXT, "Show score");
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
                mQueueMove = true;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGame.insertTile();
                        renderGame();

                        // timeout?
                        handleNextMove();
                    }
                }, NEW_TILE_TIMEOUT);
            }
        });
        mGameState = GameState.RUNNING;
    }

    @Override
    public void onDestroy() {
        Log.d(SampleExtensionService.LOG_TAG, "SampleControlSmartWatch onDestroy");
        mHandler = null;
    }

    @Override
    public void onResume() {
        Log.d(SampleExtensionService.LOG_TAG, "Starting animation");

        renderGame();
    }

    @Override
    public void onPause() {
        Log.d(SampleExtensionService.LOG_TAG, "onPause");
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
        if (mGameState != GameState.RUNNING) {
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

        if (mQueueMove) {
            mMoveQueue.add(gameDirection);
        } else {
            performMove(gameDirection);
        }
    }

    private void handleNextMove() {
        if (mMoveQueue.isEmpty()) {
            mQueueMove = false;
            return;
        }

        Game.Direction direction = mMoveQueue.remove();
        performMove(direction);

        if (mMoveQueue.isEmpty()) {
            mQueueMove = false;
        }
    }

    private void performMove(Game.Direction direction) {
        mGame.move(direction);
        updateGameState();
        renderGame();
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
        }
    }

    private void startNewGame() {
        mGame.newGame();
        mGameState = GameState.RUNNING;
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
        }
    }

    private void renderGame() {
        switch (mGameState) {
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
        }
    }

    private void showScore() {
        mLastGameState = mGameState;
        mGameState = GameState.SCORE;
        renderGame();
    }

    private void renderScore() {
        Bundle[] data = new Bundle[1];
        data[0] = new Bundle();
        data[0].putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.score);
        data[0].putString(Control.Intents.EXTRA_TEXT, "Score: " + mGame.getScore());

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
                Uri uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/" +
                        imageRes);
                bundle.putString(Control.Intents.EXTRA_DATA_URI, uri.toString());
                data[bundleIndex++] = bundle;
            }
        }

        showLayout(R.layout.game_field, data);
    }
}
