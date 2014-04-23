package sexy.fairly.smartwatch.game2048;

import android.os.Handler;

import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

public class GameExtensionService extends ExtensionService {

    public static final String EXTENSION_KEY = "com.sonymobile.smartconnect.extension.samplecontrol.key";

    public GameExtensionService() {
        super(EXTENSION_KEY);
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
