package sexy.fairly.smartwatch.game2048;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

public class GameExtensionService extends ExtensionService {
    public static final String ACTION_PURCHASE_COMPLETE = "purchase_complete";
    public static final String ACTION_PURCHASE_CANCELLED = "purchase_cancelled";

    public static final String EXTENSION_KEY = "com.sonymobile.smartconnect.extension.samplecontrol.key";


    public GameExtensionService() {
        super(EXTENSION_KEY);
    }

    public static void purchaseComplete(Context context) {
        Intent intent = new Intent(ACTION_PURCHASE_COMPLETE, null, context,
                GameExtensionService.class);
        context.startService(intent);
    }

    public static void purchaseCancelled(Context context) {
        Intent intent = new Intent(ACTION_PURCHASE_CANCELLED, null, context,
                GameExtensionService.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_PURCHASE_COMPLETE.equals(action)) {
            doActionOnAllControls(GameControlSmartWatch2.ACTION_PURCHASE_COMPLETE, null);
        } else if (ACTION_PURCHASE_CANCELLED.equals(action)) {
            doActionOnAllControls(GameControlSmartWatch2.ACTION_PURCHASE_CANCELLED, null);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new GameRegistrationInformation(this);
    }

    @Override
    protected boolean keepRunningWhenConnected() {
        return true;
    }

    @Override
    public ControlExtension createControlExtension(String hostAppPackageName) {
        boolean advancedFeaturesSupported = DeviceInfoHelper.isSmartWatch2ApiAndScreenDetected(
                this, hostAppPackageName);

        if (!advancedFeaturesSupported) {
            throw new IllegalArgumentException("No control for: " + hostAppPackageName);
        }

        return new GameControlSmartWatch2(hostAppPackageName, this, new Handler());
    }
}
