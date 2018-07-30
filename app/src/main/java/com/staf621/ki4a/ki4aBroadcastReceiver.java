package com.staf621.ki4a;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class ki4aBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Broadcast",intent.getAction().toString());
        if(intent.getAction().equals(ki4aService.START_KI4A_INTENT) ||
                (intent.getAction().equals(ki4aService.TOGGLE_KI4A_INTENT) &&
                        ki4aService.current_status == Util.STATUS_DISCONNECT
                )) {
            if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
                Intent intentService = new Intent(context, ki4aService.class);
                ki4aService.current_status = Util.STATUS_CONNECTING;
                ki4aService.notifyStatusChange(context);
                ki4aService.toState = Util.STATUS_SOCKS;
                // Notify Service about the button being pushed
                context.startService(intentService);
            }
        } else if(intent.getAction().equals(ki4aService.STOP_KI4A_INTENT) ||
                (intent.getAction().equals(ki4aService.TOGGLE_KI4A_INTENT) &&
                        (ki4aService.current_status == Util.STATUS_CONNECTING ||
                                ki4aService.current_status == Util.STATUS_SOCKS)
                )) {
            if ( ki4aService.current_status == Util.STATUS_CONNECTING ||
                    ki4aService.current_status == Util.STATUS_SOCKS
                    ) {
                Intent intentService = new Intent(context, ki4aService.class);
                ki4aService.toState = Util.STATUS_DISCONNECT;
                // Notify Service about the button being pushed
                context.startService(intentService);
            }
        } else if(intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autoconnect = preferences.getBoolean("autoconnect_switch", false);
            if(autoconnect) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.e("networt",info.getExtraInfo().toString());
                if (info.isConnected()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
                        Intent intentService = new Intent(context, ki4aService.class);
                        ki4aService.current_status = Util.STATUS_CONNECTING;
                        ki4aService.notifyStatusChange(context);
                        ki4aService.toState = Util.STATUS_SOCKS;
                        // Notify Service about the button being pushed
                        context.startService(intentService);
                    }
                }
            }
        } else if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON")) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean start_on_boot = preferences.getBoolean("boot_switch", false);
            if (start_on_boot) {
                if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
                    Intent intentService = new Intent(context, ki4aService.class);
                    ki4aService.current_status = Util.STATUS_CONNECTING;
                    ki4aService.notifyStatusChange(context);
                    ki4aService.toState = Util.STATUS_SOCKS;
                    // Notify Service about the button being pushed
                    context.startService(intentService);
                }
            }

            //boot_switch

        }

    }
}
