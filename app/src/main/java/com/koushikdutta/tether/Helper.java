package com.koushikdutta.tether;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Helper {
    static final String LOGTAG = "Tether";
    static final boolean SANDBOX = false;
    static long TRIAL_DURATION = 1209600000;
    static long TRIAL_LIMIT = 20000000;
    static long mDailyReset = 0;
    static long mDailyTrialUsed = 0;
    static boolean mExpired = false;
    static boolean mPurchased = true;
    static long mTrialStartDate = 0;
    static boolean mUpdating = false;

    /*public static void updateDataStats(Context context, int transmitted, final UpdateTrialCallback callback) {
        Log.i(LOGTAG, "Device ID stats: " + ClockworkModBillingClient.getSafeDeviceId(context));
        final SharedPreferences stats = context.getSharedPreferences("settings", 0);
        final int total = stats.getInt("transmitted", 0) + transmitted;
        final Editor editor = stats.edit();
        editor.putInt("transmitted", total);
        editor.commit();
        if (!mUpdating) {
            mUpdating = true;
            ClockworkModBillingClient.getInstance().updateTrial(context, "tether.premium", null, 0, total, new UpdateTrialCallback() {
                public void onFinished(boolean success, long trialStartDate, long trialIncrement, long trialDailyIncrement, long trialDailyWindow) {
                    Helper.mUpdating = false;
                    editor.putInt("transmitted", stats.getInt("transmitted", 0) - total);
                    editor.commit();
                    if (!Helper.mPurchased) {
                        Helper.mTrialStartDate = trialStartDate;
                        if (Helper.TRIAL_DURATION + trialStartDate < System.currentTimeMillis()) {
                            Helper.mDailyTrialUsed = trialDailyIncrement;
                            Helper.mDailyReset = trialDailyWindow;
                            Helper.mExpired = (Helper.mDailyTrialUsed > Helper.TRIAL_LIMIT ? 1 : 0) | Helper.mExpired;
                        }
                    }
                    if (callback != null) {
                        callback.onFinished(success, trialStartDate, trialIncrement, trialDailyIncrement, trialDailyWindow);
                    }
                }
            });
        }
    }*/

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
