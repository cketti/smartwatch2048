package sexy.fairly.smartwatch.game2048;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

import sexy.fairly.smartwatch.game2048.storage.BestScore;
import sexy.fairly.smartwatch.game2048.storage.DatabaseHelper;
import sexy.fairly.smartwatch.game2048.storage.State;


public class PersistenceService extends IntentService {
    public static final String ACTION_SAVE = "save";
    public static final String ACTION_READ = "read";

    public static final String EXTRA_RESULT_RECEIVER = "resultReceiver";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_GRID_STATE = "cells";
    public static final String EXTRA_BEST_SCORE = "bestScore";

    private DatabaseHelper mDatabaseHelper;

    public PersistenceService() {
        super("PersistenceService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        try {
            Dao<State, Integer> stateDao = mDatabaseHelper.getDao(State.class);
            Dao<BestScore, Integer> scoreDao = mDatabaseHelper.getDao(BestScore.class);

            if (ACTION_SAVE.equals(action)) {
                State state = intent.getParcelableExtra(EXTRA_GRID_STATE);
                stateDao.createOrUpdate(state);

                int score = intent.getIntExtra(EXTRA_BEST_SCORE, -1);
                if (score > -1) {
                    BestScore bestScore = new BestScore(score);
                    scoreDao.createOrUpdate(bestScore);
                }
            } else if (ACTION_READ.equals(action)) {
                ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
                int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);

                State state = stateDao.queryForId(State.DEFAULT_ID);
                BestScore bestScore = scoreDao.queryForId(BestScore.DEFAULT_ID);

                Bundle bundle = new Bundle();
                bundle.putParcelable(EXTRA_GRID_STATE, state);
                if (bestScore != null) {
                    bundle.putInt(EXTRA_BEST_SCORE, bestScore.getScore());
                }

                receiver.send(resultCode, bundle);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDatabaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
    }

    @Override
    public void onDestroy() {
        OpenHelperManager.releaseHelper();
        super.onDestroy();
    }
}
