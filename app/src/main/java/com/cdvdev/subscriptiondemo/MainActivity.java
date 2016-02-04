package com.cdvdev.subscriptiondemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.cdvdev.subscriptiondemo.helpers.IabHelper;
import com.cdvdev.subscriptiondemo.utils.iab.IabBroadcastReceiver;
import com.cdvdev.subscriptiondemo.utils.iab.IabResult;
import com.cdvdev.subscriptiondemo.utils.iab.Inventory;
import com.cdvdev.subscriptiondemo.utils.iab.Purchase;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    // SKU for our subscription
    static final String SKU_SUB_MONTHLY = "monthly_subscribe_demo";
    static final String SKU_SUB_SIX_MONTHLY = "halfyearly_subscribe_demo";
    static final String SKU_SUB_YEARLY = "yearly_subscribe_demo";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    boolean mAlreadySubscribed = false;
    String mFirstChoiceSku = "", mSecondChoiceSku = "", mThirdChoiceSku = "";
    String mAutoRenewingSku = "";
    String mSelectedSubscription = "";

    IabHelper mIabHelper;
    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;
    // Will the subscription auto-renew?
    boolean mAutoRenewEnabled = false;

    Button mSubsButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSubsButton = (Button) findViewById(R.id.button_subscribe);
        mSubsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubscriptionDialog();
            }
        });

        setupInAppBilling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        destroyInAppBilling();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIabHelper.logDebug("onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mIabHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            mIabHelper.logDebug("onActivityResult handled by IABUtil.");
        }
    }

    /**
     * Show subscription chooser dialog
     */
    private void showSubscriptionDialog(){
        if (!mIabHelper.isSubscriptionsSupported()) {
            mIabHelper.showAlertDialog("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        CharSequence[] options;
        if (!mAlreadySubscribed || !mAutoRenewEnabled) {
            options = new CharSequence[3];
            options[0] = getString(R.string.subscription_item_monthly);
            options[1] = getString(R.string.subscription_item_six_monthly);
            options[2] = getString(R.string.subscription_item_yearly);

            mFirstChoiceSku = SKU_SUB_MONTHLY;
            mSecondChoiceSku = SKU_SUB_SIX_MONTHLY;
            mThirdChoiceSku = SKU_SUB_YEARLY;
        } else {
            // This is the subscription upgrade/downgrade path, so only one option is valid
            options = new CharSequence[1];
            if (mAutoRenewingSku.equals(SKU_SUB_MONTHLY)) {
                options = new CharSequence[2];
                // Give the option to upgrade to six-monthly or  yearly
                options[0] = getString(R.string.subscription_item_six_monthly);
                options[1] = getString(R.string.subscription_item_yearly);
                mFirstChoiceSku = SKU_SUB_SIX_MONTHLY;
                mSecondChoiceSku = SKU_SUB_YEARLY;
            } else if (mAutoRenewingSku.equals(SKU_SUB_SIX_MONTHLY)){
                options = new CharSequence[2];
                // Give the option to downgrade to monthly or yearly
                options[0] = getString(R.string.subscription_item_monthly);
                options[1] = getString(R.string.subscription_item_yearly);
                mFirstChoiceSku = SKU_SUB_MONTHLY;
                mSecondChoiceSku = SKU_SUB_YEARLY;
            } else {
                options = new CharSequence[2];
                // Give the option to downgrade to monthly or six-monthly
                options[0] = getString(R.string.subscription_item_monthly);
                options[1] = getString(R.string.subscription_item_six_monthly);
                mFirstChoiceSku = SKU_SUB_MONTHLY;
                mSecondChoiceSku = SKU_SUB_SIX_MONTHLY;
            }
            mThirdChoiceSku = "";
        }

        int titleResId;
        if (!mAlreadySubscribed) {
            titleResId = R.string.subscription_period_prompt;
        } else if (!mAutoRenewEnabled) {
            titleResId = R.string.subscription_resignup_prompt;
        } else {
            titleResId = R.string.subscription_update_prompt;
        }

        //create chooser dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleResId)
                .setSingleChoiceItems(options, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mSelectedSubscription = mFirstChoiceSku;
                                break;
                            case 1:
                                mSelectedSubscription = mSecondChoiceSku;
                                break;
                            case 2:
                                mSelectedSubscription = mThirdChoiceSku;
                                break;
                        }
                    }
                })
                .setPositiveButton(R.string.subscription_prompt_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* TODO: for security, generate your payload here for verification. See the comments on
                         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
                         *        an empty string, but on a production app you should carefully generate
                         *        this.
                          */
                        String payload = "";

                        if (TextUtils.isEmpty(mSelectedSubscription)) {
                            // The user has not changed from the default selection
                            mSelectedSubscription = mFirstChoiceSku;
                        }

                        List<String> oldSkus = null;
                        //if our selected purchase has  auto renewing
                        if (!TextUtils.isEmpty(mAutoRenewingSku) && !mAutoRenewingSku.equals(mSelectedSubscription)) {
                            // The user currently has a valid subscription, any purchase action is going to
                            // replace that subscription
                            oldSkus = new ArrayList<String>();
                            oldSkus.add(mAutoRenewingSku);
                        }

                        mIabHelper.logDebug("Launching purchase flow for subscription.");
                        mIabHelper.launchPurchaseFlow(MainActivity.this, mSelectedSubscription, IabHelper.ITEM_TYPE_SUBS,
                                oldSkus, RC_REQUEST, mPurchaseFinishedListener, payload);
                        // Reset the dialog options
                        mSelectedSubscription = "";
                        mFirstChoiceSku = "";
                        mSecondChoiceSku = "";
                        mThirdChoiceSku = "";

                    }
                })
                .setNegativeButton(R.string.subscription_prompt_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         //do nothing
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }



    private void doOnQueryInventoryFinished(IabResult result, Inventory inv){
        //TODO: change this for your needs
        // First find out which subscription is auto renewing
        Purchase subMonthly = inv.getPurchase(SKU_SUB_MONTHLY);
        Purchase subSixMonthly = inv.getPurchase(SKU_SUB_SIX_MONTHLY);
        Purchase subYearly = inv.getPurchase(SKU_SUB_YEARLY);

        if (subMonthly != null && subMonthly.isAutoRenewing()) {
            mAutoRenewingSku = SKU_SUB_MONTHLY;
            mAutoRenewEnabled = true;
        } else if (subSixMonthly != null && subSixMonthly.isAutoRenewing()) {
            mAutoRenewingSku = SKU_SUB_SIX_MONTHLY;
            mAutoRenewEnabled = true;
        } else if (subYearly != null && subYearly.isAutoRenewing()) {
            mAutoRenewingSku = SKU_SUB_YEARLY;
            mAutoRenewEnabled = true;
        } else {
            mAutoRenewingSku = "";
            mAutoRenewEnabled = false;
        }

        mAlreadySubscribed = (subMonthly != null && mIabHelper.verifyDeveloperPayload(subMonthly))
                || (subSixMonthly != null && mIabHelper.verifyDeveloperPayload(subSixMonthly))
                || (subYearly != null && mIabHelper.verifyDeveloperPayload(subYearly));
    }

    /**
     * Setup in-app billing
     */
    private void setupInAppBilling() {

        String licenseKey = getResources().getString(R.string.base64_app_license_key);

        mIabHelper = new IabHelper(this, licenseKey);
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                mIabHelper.logDebug("Setup finished.");
                if (!result.isSuccess()) {
                    mIabHelper.showAlertDialog("Problem setting up in-app billing: " + result);
                    return;
                }
                if (mIabHelper == null) {
                    return;
                }

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(new IabBroadcastReceiver.IabBroadcastListener() {
                    @Override
                    public void receivedBroadcast() {
                        // Received a broadcast notification that the inventory of items has changed
                        mIabHelper.logDebug("Received broadcast notification. Querying inventory.");
                        mIabHelper.queryInventoryAsync(mGotInventoryListener);
                    }
                });
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                mIabHelper.logDebug("Setup successful. Querying inventory.");
                mIabHelper.queryInventoryAsync(mGotInventoryListener);

            }
        });
    }

    /**
     * Destroy in-app billing
     */
    private void destroyInAppBilling() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        mIabHelper.logDebug("Destroying helper.");
        if (mIabHelper != null) {
            mIabHelper.dispose();
            mIabHelper = null;
        }
    }


    // IN-APP BILLING CALLBACKS

    // Listener that's called when we finish querying the items and subscriptions we own
    final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            mIabHelper.logDebug("Query inventory finished.");
            // Have we been disposed of in the meantime? If so, quit.
            if (mIabHelper == null) return;
            // Is it a failure?
            if (result.isFailure()) {
                mIabHelper.showAlertDialog("Failed to query inventory: " + result);
                return;
            }
            mIabHelper.logDebug("Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */
            doOnQueryInventoryFinished(result, inv);
            mIabHelper.logDebug("Initial inventory query finished; enabling main UI.");
        }
    };

    // Callback when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener(){
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            mIabHelper.logDebug("Purchase finished: " + result + ", purchase: " + info);
            // if we were disposed of in the meantime, quit.
            if (mIabHelper == null) return;

            if (result.isFailure()) {
                mIabHelper.showAlertDialog("Error purchasing: " + result);
                return;
            }
            if (!mIabHelper.verifyDeveloperPayload(info)) {
                mIabHelper.showAlertDialog("Error purchasing. Authenticity verification failed.");
                return;
            }
            mIabHelper.logDebug("Purchase successful.");

            if (info.getSku().equals(SKU_SUB_MONTHLY) ||
                    info.getSku().equals(SKU_SUB_SIX_MONTHLY) ||
                    info.getSku().equals(SKU_SUB_YEARLY)) {

                mIabHelper.showAlertDialog("Thank you for subscribing!");

                mAlreadySubscribed = true;
                mAutoRenewEnabled = info.isAutoRenewing();
                mAutoRenewingSku = info.getSku();

                //TODO: Update UI or do other actions after purchase
            }

        }
    };



}
