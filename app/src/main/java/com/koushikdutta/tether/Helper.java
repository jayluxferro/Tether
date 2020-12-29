package com.koushikdutta.tether;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;


public class Helper {
    static long TRIAL_DURATION = 1209600000;
    static long TRIAL_LIMIT = 20000000;
    static long mDailyReset = 0;
    static long mDailyTrialUsed = 0;
    static boolean mExpired = false;
    static boolean mPurchased = true;
    static long mTrialStartDate = 0;

    public static void showAlertDialog(Context context, int stringResource) {
        showAlertDialog(context, context.getString(stringResource), null);
    }

    public static void showAlertDialog(Context context, int stringResource, OnClickListener okCallback) {
        showAlertDialog(context, context.getString(stringResource), okCallback);
    }

    public static void showAlertDialog(Context context, String s) {
        showAlertDialog(context, s, null);
    }

    public static void showAlertDialog(Context context, String s, OnClickListener okCallback) {
        Builder builder = new Builder(context);
        builder.setMessage(s);
        builder.setPositiveButton("", okCallback);
        builder.setCancelable(false);
        builder.create().show();
    }
}
