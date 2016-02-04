package com.cdvdev.subscriptiondemo.utils.iab;

import com.cdvdev.subscriptiondemo.helpers.IabHelper;

/**
 * Class for represents result of an in-app billing operation.
 *
 * @author Dmitriy V. Chernysh (dmitriy.chernysh@gmail.com)
 *         Created on 03.02.16.
 */
public class IabResult {

    int mResponse;
    String mMessage;

    public IabResult(int response, String message) {
        mResponse = response;
        if (message == null || message.trim().length() == 0) {
            mMessage = IabHelper.getResponseDesc(response);
        } else {
            mMessage = message + " (response: " + IabHelper.getResponseDesc(response) + ")";
        }
    }

    public String getMessage() {
        return mMessage;
    }

    public boolean isSuccess() {
        return mResponse == IabHelper.BILLING_RESPONSE_RESULT_OK;
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public String toString() {
        return "IabResult: " + getMessage();
    }
}
