package com.cdvdev.subscriptiondemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.vending.billing.IInAppBillingService;

public class MainActivity extends AppCompatActivity {

    private IInAppBillingService mIInAppBillingService;
    private ServiceConnection mServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Binding to IInAppBillingService
        bindInAppBillingServive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIInAppBillingService != null) {
            unbindService(mServiceConnection);
        }
    }

    /**
     *Method for binding to IInAppBillingService
     */
    private void bindInAppBillingServive(){
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mIInAppBillingService = IInAppBillingService.Stub.asInterface(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mIInAppBillingService = null;
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

     }

}
