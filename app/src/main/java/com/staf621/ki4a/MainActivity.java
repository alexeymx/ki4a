package com.staf621.ki4a;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.adclient.android.sdk.listeners.ClientAdListener;
import com.adclient.android.sdk.view.AbstractAdClientView;
import com.adclient.android.sdk.view.AdClientView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    protected static ImageButton button;
    protected static TextView text_status;
    protected static MainActivity myMainActivity;
    protected DataUpdateReceiver dataUpdateReceiver;
    protected static AdClientView adClientView;
    protected static SharedPreferences preferences;
    public EndpointListAdaptor customAdapter;

    // This Class is called from ki4aService to notify a status change
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ki4aService.REFRESH_STATUS_INTENT)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.refresh_status_img(ki4aService.current_status);
                    }
                });
            }
            else if(intent.getAction().equals(ki4aService.ASK_FOR_PASS_INTENT)) {
                MyLog.d(Util.TAG,"Got intent for pass request!");
                ask_for_pass(context);
            }
        }
    }

    // Menu Creator
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handler for Config Icon Menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_config:
                Intent settingsIntent = new Intent().setClass(
                        MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        myMainActivity = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String server_text = preferences.getString("server_text", "");
        List<EndpointItem> servers = new ArrayList<>();

        if (server_text.contains(",")) {
            String[] hosts = server_text.split(",");
            for (String host : hosts) {
                host = host.trim();
                EndpointItem ep = new EndpointItem();
                ep.host = host;
                try {
                    InetAddress address = InetAddress.getByName(host);
                    ep.ip = address.getHostAddress();
                } catch (UnknownHostException e) {
                    ep.ip = "0.0.0.0";
                } catch (SecurityException e) {
                    ep.ip = "0.0.0.0";
                }
                servers.add(ep);
            }
        }

        ListView listView=(ListView)findViewById(R.id.list);
        if (preferences.getBoolean("ping_switch", true)) {

            listView.setVisibility(View.VISIBLE);
            listView.setClickable(true);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    EndpointItem item = (EndpointItem) parent.getItemAtPosition(position);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+item.host));
                    startActivity(browserIntent);
                }

            });

            customAdapter = new EndpointListAdaptor(this, R.layout.endpoint_host_item, servers);

            listView.setAdapter(customAdapter);

            CheckAvailability m = new CheckAvailability(servers);
            m.execute();
        } else {
            listView.setVisibility(View.GONE);
        }




        if(ki4aService.toState == Util.STATUS_DISCONNECT) // First time we open the app (or disconnected)
        {
            // Let's start service at the beginning of the app
            ki4aService.toState = Util.STATUS_INIT; // Init
            Intent intent = new Intent(myMainActivity, ki4aService.class);
            myMainActivity.startService(intent);
        }

        button = (ImageButton) findViewById(R.id.imageButton_status);
        refresh_status_img(ki4aService.current_status);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean startKi4a = false;
                boolean iptables_switch = preferences.getBoolean("iptables_switch", false);

                if (!iptables_switch) {
                    // Ask for VPN permission
                    MyLog.i(Util.TAG, "Asking for permission to use VPN");
                    Intent intentVpn = VpnService.prepare(myMainActivity);
                    if (intentVpn != null) {
                        MyLog.i(Util.TAG, "First time VPN permission, asking...");
                        startActivityForResult(intentVpn, 0);
                    }
                    else
                        startKi4a = true;
                }
                else
                    startKi4a = true;

                if(startKi4a) {
                    Intent intent = new Intent(myMainActivity, ki4aService.class);
                    if (ki4aService.current_status == Util.STATUS_DISCONNECT) {
                        ki4aService.current_status = Util.STATUS_CONNECTING;
                        refresh_status_img(ki4aService.current_status);
                        ki4aService.toState = Util.STATUS_SOCKS;
                    } else if (ki4aService.current_status == Util.STATUS_CONNECTING ||
                            ki4aService.current_status == Util.STATUS_SOCKS
                            )
                        ki4aService.toState = Util.STATUS_DISCONNECT;

                    // Notify Service about the button being pushed
                    startService(intent);
                }
            }
        });

        adClientView = (AdClientView) findViewById(R.id.adClientView);
        /* adClientView.addClientAdListener(new ClientAdListener() {
            @Override
            public void onReceivedAd(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad received callback.");
            }
            @Override
            public void onFailedToReceiveAd(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad failed to be received callback.");
            }
            @Override
            public void onShowAdScreen(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad show ad screen callback.");
            }
            @Override
            public void onLoadingAd(AbstractAdClientView adClientView, String
                    message) {
                MyLog.d(Util.TAG, "Ad loaded callback.");
            }
            @Override
            public void onClosedAd(AbstractAdClientView adClientView) {
                MyLog.d(Util.TAG, "Ad closed callback.");
            }
        });
        adClientView.load(); */
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the current status
        refresh_status_img(ki4aService.current_status);

        // Re-register to Service Updates
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ki4aService.REFRESH_STATUS_INTENT);
        intentFilter.addAction(ki4aService.ASK_FOR_PASS_INTENT);
        registerReceiver(dataUpdateReceiver, intentFilter);

        if (adClientView != null) {
            adClientView.resume();
        }
    }

    @Override
    public void onPause() {
        if (adClientView != null) {
            adClientView.pause();
        }
        super.onPause();
        // Unregister Service Updates
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    }

    /** Called when the activity is closed. */
    @Override
    public void onDestroy() {
        if (adClientView != null) {
            adClientView.destroy();
        }
        super.onDestroy();
    }

    protected static void refresh_status_img(int status)
    {
        button = (ImageButton) myMainActivity.findViewById(R.id.imageButton_status);
        text_status = (TextView) myMainActivity.findViewById(R.id.textView_status);
        if(status==Util.STATUS_DISCONNECT)
        {
            text_status.setText(R.string.text_status_empty);
            button.setImageResource(R.drawable.status_red);
        }
        else if(status==Util.STATUS_INIT)
        {
            text_status.setText(R.string.text_status_initializing);
            button.setImageResource(R.drawable.status_gray);
        }
        else if(status==Util.STATUS_CONNECTING)
        {
            text_status.setText(R.string.text_status_connecting);
            button.setImageResource(R.drawable.status_orange);
        }
        else if(status==Util.STATUS_SOCKS)
        {
            text_status.setText(R.string.text_status_connected);
            button.setImageResource(R.drawable.status_blue);
            if (adClientView != null) {
                // adClientView.load();
            }
        }
    }

    protected static void ask_for_pass(Context context)
    {
        final EditText pass = new EditText(context);
        int title = PreferenceManager.
                getDefaultSharedPreferences(context).
                getBoolean("key_switch", false) ?
                R.string.str_key_passphrase : R.string.pref_title_ssh_password;
        pass.setSingleLine(true);
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(pass)
                .setCancelable(false)
                .setPositiveButton(R.string.str_connect, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ki4aService.current_ssh_pass = pass.getText().toString();
                        ki4aService.got_ssh_pass = true;
                    }
                })
                .show();

        pass.setOnKeyListener(new TextView.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                }
                return false;
            }
        });
        pass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                }
                return true;
            }
        });

        pass.requestFocus();
    }

    public class CheckAvailability extends AsyncTask<Void, List<EndpointItem>, String> {
        public List<EndpointItem> n_e;
        public String mip_address = "0.0.0.0";


        CheckAvailability(List<EndpointItem> it) {
           this.n_e = it;
           Log.d("constrcutor", this.n_e.toString());
        }
        //List<EndpointItem>
        @Override
        protected String doInBackground(Void... params) {
           // for (String location : params) {
              //  String tmp = getTemp(location);
                //publishProgress(tmp);    /** Use Result **/

           // }
            while (true) {
                try {
                    Log.e("pr",this.n_e.toString());

                    for (int i = 0; i < this.n_e.size(); i++)
                    {
                        EndpointItem item = this.n_e.get(i);
                        InetAddress address = InetAddress.getByName(item.host);
                        item.ip = address.getHostAddress();
                        item.reachable = InetAddress.getByName(item.host).isReachable(500);
                        if (item.reachable) {
                            Log.e("IP reachablke", item.host );

                        } else {
                            Log.e("IP un reachablke", item.host );

                        }
                        this.n_e.set(i,item);
                        //do something with i


                    }

                    String ipAddress = null;
                    try {
                        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                            NetworkInterface intf = en.nextElement();
                            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                                InetAddress inetAddress = enumIpAddr.nextElement();
                                if (!inetAddress.isLoopbackAddress()) {
                                    ipAddress = inetAddress.getHostAddress().toString();
                                    Log.e("Here is the Address",ipAddress);
                                    this.mip_address = ipAddress;
                                }
                            }
                        }
                    } catch (SocketException ex) {
                        this.mip_address = "0.0.0.0";
                    }


                    publishProgress(this.n_e);
                    Thread.sleep(15000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
           //
            // return null;
        }

        @Override
        protected void onProgressUpdate(List<EndpointItem>... val) {
            super.onProgressUpdate(val);

            //customAdapter.clear();

            //customAdapter.addAll(val[0]);
            //Log.e("new vals", val[0].toString());

            customAdapter.notifyDataSetChanged();
            TextView ipView = (TextView)findViewById(R.id.ipViewTv);
            ipView.setText(this.mip_address);

        }

    }

    public void reloadData(View view) {
        Util.refreshMobileData();
    }

}