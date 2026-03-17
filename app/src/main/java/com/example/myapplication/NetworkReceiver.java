package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkReceiver extends BroadcastReceiver {

    private NetworkListener listener;

    public interface NetworkListener {
        void onNetworkChanged(boolean isConnected);
    }

    public NetworkReceiver(NetworkListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isConnected = false;
        if (cm != null) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        if (listener != null) {
            listener.onNetworkChanged(isConnected);
        }
    }
}
