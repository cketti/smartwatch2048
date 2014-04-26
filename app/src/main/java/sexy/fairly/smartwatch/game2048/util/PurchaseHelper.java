package sexy.fairly.smartwatch.game2048.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import java.util.concurrent.TimeUnit;

import sexy.fairly.smartwatch.game2048.PersistenceService;


public class PurchaseHelper {
    private static final boolean DEBUG = false;
    private static final long PURCHASE_STATE_TIMEOUT = TimeUnit.DAYS.toMillis(14);
    private static final String SKU_FULL_VERSION = "full_version";
    private static final String LICENSE_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjIOpxkDWlUW+/eMjMn7x4fAQGTs7" +
            "Ef5xLEmDeLy3U8cG31rmi0d7YtjYQNrJx4hN8KDDVl0WRsNL5B9Yb+u9K0NiJ8Tf+CZHCoM0" +
            "l4490FSX971PRmLaQ4NZMFSmJITeum0JhNhuB/rMMmT/nbHEaDJcDf3nfQLrngTli+4YbSBi" +
            "BRqmnCzA9NfHOv6qRA090ipAJFAQBnKGpsbm6G29Q0+OWN1lZY64TD7dQuJMkKZzKMDxFE3B" +
            "9FL9a65D5RIZ1bwr7hmxHKsuNPpFIJRjQYHU/2sk2rYd+lfSvYqs+Vc8LdQR6hNlwtFHlm9S" +
            "vkxFwUfKOJg0ROrd6ovpRVV8hQIDAQAB";


    private Context mContext;
    private Handler mHandler;
    private IabHelper mHelper;
    private PurchaseListener mPurchaseListener;
    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener =
            new InventoryListener();


    public PurchaseHelper(Context context, PurchaseListener listener) {
        mContext = context;
        mHandler = new Handler();
        mPurchaseListener = listener;
        mHelper = new IabHelper(context, LICENSE_PUBLIC_KEY);
        mHelper.enableDebugLogging(DEBUG);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess() || mHelper == null) {
                    loadCachedPurchaseState();
                    return;
                }

                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    public void destroy() {
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    public void launchPurchaseFlow(Activity activity, int requestCode,
            PurchaseFinishedListener listener) {
        String payload = "";
        mHelper.launchPurchaseFlow(activity, SKU_FULL_VERSION, requestCode,
                new GamePurchaseFinishedListener(listener), payload);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return (mHelper != null && mHelper.handleActivityResult(requestCode, resultCode, data));
    }

    private void loadCachedPurchaseState() {
        Intent intent = new Intent(mContext, PersistenceService.class);
        intent.setAction(PersistenceService.ACTION_READ_PURCHASE_STATE);
        intent.putExtra(PersistenceService.EXTRA_RESULT_RECEIVER,
                new PurchaseStateResultReceiver(mHandler));
        mContext.startService(intent);
    }

    private void savePurchaseState() {
        Intent intent = new Intent(mContext, PersistenceService.class);
        intent.setAction(PersistenceService.ACTION_SAVE_PURCHASE_STATE);
        intent.putExtra(PersistenceService.EXTRA_FULL_VERSION, true);
        mContext.startService(intent);
    }

    /** Verifies the developer payload of a purchase. */
    private boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    class InventoryListener implements IabHelper.QueryInventoryFinishedListener {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null || result.isFailure()) {
                loadCachedPurchaseState();
                return;
            }

            Purchase premiumPurchase = inventory.getPurchase(SKU_FULL_VERSION);
            boolean fullVersion = (premiumPurchase != null &&
                    verifyDeveloperPayload(premiumPurchase));

            if (fullVersion) {
                savePurchaseState();
            }

            mPurchaseListener.purchaseState(fullVersion);
        }
    }

    class PurchaseStateResultReceiver extends ResultReceiver {
        public PurchaseStateResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            boolean cachedFullVersion =
                    resultData.getBoolean(PersistenceService.EXTRA_FULL_VERSION, false);
            long timeStamp = resultData.getLong(PersistenceService.EXTRA_TIMESTAMP, -1);

            boolean fullVersion = (cachedFullVersion &&
                     System.currentTimeMillis() - timeStamp <= PURCHASE_STATE_TIMEOUT);

            mPurchaseListener.purchaseState(fullVersion);
        }
    }

    class GamePurchaseFinishedListener implements IabHelper.OnIabPurchaseFinishedListener {
        private final PurchaseFinishedListener mListener;

        public GamePurchaseFinishedListener(PurchaseFinishedListener listener) {
            mListener = listener;
        }

        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            boolean success = false;
            boolean userCancelled = false;

            if (mHelper != null) {
                if (result.isFailure()) {
                    userCancelled = (result.getResponse() ==
                            IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED);
                } else if (verifyDeveloperPayload(purchase) &&
                        purchase.getSku().equals(SKU_FULL_VERSION)) {
                    success = true;
                }
            }

            mListener.purchaseFinished(success, userCancelled);
        }
    }

    public static interface PurchaseListener {
        void purchaseState(boolean fullVersion);
    }

    public static interface PurchaseFinishedListener {
        void purchaseFinished(boolean success, boolean userCancelled);
    }
}
