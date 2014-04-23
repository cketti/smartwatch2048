package sexy.fairly.smartwatch.game2048;

import android.content.ContentValues;
import android.content.Context;

import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;


public class GameRegistrationInformation extends RegistrationInformation {

    final Context mContext;

    protected GameRegistrationInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return 2;
    }

    @Override
    public int getTargetControlApiVersion() {
        return 2;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredNotificationApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return 0;
    }

    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String iconHostapp = ExtensionUtils.getUriString(mContext, R.drawable.ic_2048);
        String iconExtension = ExtensionUtils
                .getUriString(mContext, R.drawable.ic_2048_36x36);
        String iconExtension48 = ExtensionUtils
                .getUriString(mContext, R.drawable.ic_2048_48x48);

        ContentValues values = new ContentValues();

        values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY,
                GamePreferenceActivity.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT,
                mContext.getString(R.string.configuration_text));
        values.put(Registration.ExtensionColumns.NAME, mContext.getString(R.string.extension_name));
        values.put(Registration.ExtensionColumns.EXTENSION_KEY,
                GameExtensionService.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostapp);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, iconExtension);
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, iconExtension48);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION,
                getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());

        return values;
    }

    @Override
    public boolean isDisplaySizeSupported(int width, int height) {
        return (width == GameControlSmartWatch2.getSupportedControlWidth(mContext) &&
                height == GameControlSmartWatch2 .getSupportedControlHeight(mContext));
    }
}
