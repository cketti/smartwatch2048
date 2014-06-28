package sexy.fairly.smartwatch.game2048;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import sexy.fairly.smartwatch.game2048.util.PurchaseHelper;


public class BuyGameActivity extends Activity {
    private static final int RC_REQUEST = 10001;
    private static final String EXTRA_DISPLAY_SUCCESS_MESSAGE = "display_success_message";
    private static final String EXTRA_DISPLAY_ERROR_MESSAGE = "display_error_message";


    public static void show(Context context) {
        Intent intent = new Intent(context, BuyGameActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }


    private PurchaseHelper mPurchaseHelper;
    private boolean mDisplaySuccessMessage;
    private boolean mDisplayErrorMessage;
    private GamePurchaseListener mPurchaseListener;
    private GamePurchaseFinishedListener mPurchaseFinishedListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_game);

        findViewById(R.id.buy_game_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPurchaseFlow();
            }
        });

        if (savedInstanceState != null) {
            mDisplaySuccessMessage = savedInstanceState.getBoolean(EXTRA_DISPLAY_SUCCESS_MESSAGE);
            mDisplayErrorMessage = savedInstanceState.getBoolean(EXTRA_DISPLAY_ERROR_MESSAGE);
        } else {
            mDisplaySuccessMessage = false;
        }

        mPurchaseListener = new GamePurchaseListener();
        mPurchaseFinishedListener = new GamePurchaseFinishedListener();
        mPurchaseHelper = new PurchaseHelper(this, mPurchaseListener);

        if (mDisplaySuccessMessage) {
            displaySuccessMessage();
        } else if (mDisplayErrorMessage) {
            displayErrorMessage();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_DISPLAY_SUCCESS_MESSAGE, mDisplaySuccessMessage);
        outState.putBoolean(EXTRA_DISPLAY_ERROR_MESSAGE, mDisplayErrorMessage);
    }

    class GamePurchaseListener implements PurchaseHelper.PurchaseListener {
        @Override
        public void purchaseState(boolean fullVersion) {
            if (fullVersion) {
                GameExtensionService.purchaseComplete(getApplicationContext());
                finish();
            }

            startPurchaseFlow();
        }

        @Override
        public void billingNotAvailable() {
            finish();
        }
    }

    private void startPurchaseFlow() {
        mPurchaseHelper.launchPurchaseFlow(BuyGameActivity.this, RC_REQUEST,
                mPurchaseFinishedListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mPurchaseHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class GamePurchaseFinishedListener implements PurchaseHelper.PurchaseFinishedListener {
        @Override
        public void purchaseFinished(boolean success, boolean userCancelled) {
            if (success) {
                displaySuccessMessage();
                GameExtensionService.purchaseComplete(getApplicationContext());
            } else {
                if (userCancelled) {
                    GameExtensionService.purchaseCancelled(getApplicationContext());
                    finish();
                } else {
                    displayErrorMessage();
                }
            }
        }
    }

    private void displaySuccessMessage() {
        mDisplaySuccessMessage = true;
        findViewById(R.id.thanks_for_buying_message).setVisibility(View.VISIBLE);
    }

    private void displayErrorMessage() {
        mDisplayErrorMessage = true;
        findViewById(R.id.error_container).setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPurchaseHelper.destroy();
    }
}
