package sexy.fairly.smartwatch.game2048;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;

class GameControlSmartWatch2 extends ControlExtension {
    private Handler mHandler;
    private Game mGame;

    private static final int[][] FIELD_IDS = new int[][] {
        new int[] { R.id.field1_1, R.id.field1_2, R.id.field1_3, R.id.field1_4 },
        new int[] { R.id.field2_1, R.id.field2_2, R.id.field2_3, R.id.field2_4 },
        new int[] { R.id.field3_1, R.id.field3_2, R.id.field3_3, R.id.field3_4 },
        new int[] { R.id.field4_1, R.id.field4_2, R.id.field4_3, R.id.field4_4 },
    };

    GameControlSmartWatch2(final String hostAppPackageName, final Context context,
            Handler handler) {
        super(context, hostAppPackageName);
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        mHandler = handler;
    }

    public static int getSupportedControlWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_width);
    }

    public static int getSupportedControlHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_height);
    }

    @Override
    public void onStart() {
        mGame = new Game();
    }

    @Override
    public void onDestroy() {
        Log.d(SampleExtensionService.LOG_TAG, "SampleControlSmartWatch onDestroy");
        mHandler = null;
    };

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
    public void onSwipe(int direction) {
        switch (direction) {
            case Control.Intents.SWIPE_DIRECTION_UP: {
                mGame.move(Game.Direction.UP);
                break;
            }
            case Control.Intents.SWIPE_DIRECTION_DOWN: {
                mGame.move(Game.Direction.DOWN);
                break;
            }
            case Control.Intents.SWIPE_DIRECTION_LEFT: {
                mGame.move(Game.Direction.LEFT);
                break;
            }
            case Control.Intents.SWIPE_DIRECTION_RIGHT: {
                mGame.move(Game.Direction.RIGHT);
                break;
            }
        }
        renderGame();
    }

    @Override
    public void onObjectClick(ControlObjectClickEvent event) {
        if (event.getLayoutReference() == R.id.new_game) {
            mGame.newGame();
            renderGame();
        }
    }

    private void renderGame() {
        if (mGame.isGameRunning()) {
            renderGrid();
        } else if (mGame.isGameWon()) {
            showLayout(R.layout.you_won, null);
        } else {
            showLayout(R.layout.you_lost, null);
        }
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
                String output = value == Grid.EMPTY_CELL ? "" : Integer.toString(value);
                bundle.putString(Control.Intents.EXTRA_TEXT, output);
                data[bundleIndex++] = bundle;
            }
        }

        showLayout(R.layout.game_field, data);
    }
}
