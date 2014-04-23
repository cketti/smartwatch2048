package sexy.fairly.smartwatch.game2048;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExtensionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        intent.setClass(context, GameExtensionService.class);
        context.startService(intent);
    }
}
