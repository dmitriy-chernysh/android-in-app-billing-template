
### Android In-app purchases template.

<a href="https://play.google.com/apps/testing/com.cdvdev.subscriptiondemo"><img alt="Get it on Google Play" width="200px" src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" /></a>

#### Settings

* Copy AIDL file to your project and build project:

    ![aidl](https://cloud.githubusercontent.com/assets/5750211/12844998/830ed368-cc0b-11e5-8762-4485d476ca22.png)

* Copy helper classes to you project:

    ![iab2](https://cloud.githubusercontent.com/assets/5750211/12844719/d83a169c-cc09-11e5-998c-75a56457ba4f.png)

* Add permission to AndroidManifest.xml :
  
  ```xml
  <uses-permission android:name="com.android.vending.BILLING" />
  ```

* Add license key to strings.xml :

  ```xml
   <resources>
    
    .....
    
      <string name="base64_app_license_key">your key here</string>
   </resources>
  ```

  ![lic](https://cloud.githubusercontent.com/assets/5750211/12845445/9670c58a-cc0e-11e5-9ba6-15e13ca46165.png)


* Add subscription periods to Activity: 

  ```java
    // SKU for our subscription
    static final String SKU_SUB_MONTHLY = "monthly_subscribe_demo";
    static final String SKU_SUB_SIX_MONTHLY = "halfyearly_subscribe_demo";
    static final String SKU_SUB_YEARLY = "yearly_subscribe_demo";
  ```

  ![inapp](https://cloud.githubusercontent.com/assets/5750211/12825189/af0e16ec-cb7d-11e5-8d50-65f61a407db2.png)

* Copy helper methods to Activity: 
 
  ```java
    private void setupInAppBilling() {
       .......
       .......
    }
  ```
   
   ```java
     private void destroyInAppBilling() {
       .......
       .......
     }
   ```
   
   ```java
      private void showSubscriptionDialog(){
        .......
        .......
      }
   ```
   
   and call their in Activity methods:
   
   ```java
      @Override
      protected void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           setContentView(R.layout.activity_main);
           mSubsButton = (Button) findViewById(R.id.button_subscribe);
           mSubsButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   //for example calling chooser dialog here
                   showSubscriptionDialog();
               }
           });
           setupInAppBilling();
       }
    ```
   
    ```java
       @Override
       protected void onDestroy() {
           super.onDestroy();
           destroyInAppBilling();
       }
       ```
   
* Process the response code of purchase result in onActivityResult: 

       
    ```java
       @Override
       protected void onActivityResult(int requestCode, int resultCode, Intent data) {
           if (mIabHelper == null) return;
           // Pass on the activity result to the helper for handling
           if (!mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
              // not handled, so handle it ourselves (here's where you'd
              // perform any handling of activity results not related to in-app
              // billing...
              super.onActivityResult(requestCode, resultCode, data);
           }
       }
      ```
       
