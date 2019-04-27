package com.koushikdutta.tether;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;

public class TetherActivity extends Activity {
    public static String CONFIG_URL = "http://www.clockworkmod.com/tether/config.js";
    static final String LOGTAG = "Tether";
    MenuItem mBuyMenuItem;
    boolean mCancelledDownload = false;
    boolean mConnected = false;
    LinearLayout mDataStats;
    boolean mDestroyed = false;
    Handler mHandler = new Handler();
    boolean mHasWorked = false;
    View mHelpIcon;
    View mQuotaLayout;
    ProgressBar mQuotaProgress;
    TextView mQuotaStatus;
    int mReceived = 0;
    BroadcastReceiver mReceiver = new C01791();
    MenuItem mRecoverMenuItem;
    int mSent = 0;
    SharedPreferences mSettings;
    boolean mShowingQuota = false;
    ImageView mUsbIcon;
    TextView mUsbText;

    /* renamed from: com.koushikdutta.tether.TetherActivity$10 */
    class C017010 implements OnClickListener {
        C017010() {
        }

        public void onClick(View v) {

        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$11 */
    class C017211 implements OnClickListener {

        /* renamed from: com.koushikdutta.tether.TetherActivity$11$1 */
        class C01711 implements DialogInterface.OnClickListener {
            C01711() {
            }

            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        TetherActivity.this.chooseDownloadOs();
                        return;
                    case 1:
                        TetherActivity.this.tetherIsSlow();
                        return;
                    case 2:
                        TetherActivity.this.tetherWontConnect();
                        return;
                    case 3:
                        TetherActivity.this.tetherSupport();
                        return;
                    default:
                        return;
                }
            }
        }

        C017211() {

        }

        public void onClick(View v) {
            Builder builder = new Builder(TetherActivity.this);
            builder.setTitle(R.string.help);
            builder.setItems(new CharSequence[]{TetherActivity.this.getString(R.string.download_pc), TetherActivity.this.getString(R.string.tether_is_slow), TetherActivity.this.getString(R.string.tether_will_not_connect), TetherActivity.this.getString(R.string.support)}, new C01711());
            builder.create().show();
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$12 */
    class C017312 implements DialogInterface.OnClickListener {
        C017312() {
        }

        public void onClick(DialogInterface dialog, int which) {
            Intent i = new Intent();
            i.setData(Uri.parse("market://details?id=com.koushikdutta.tether"));
            TetherActivity.this.startActivity(i);
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$13 */
    class C017413 implements DialogInterface.OnClickListener {
        C017413() {
        }

        public void onClick(DialogInterface dialog, int which) {
            TetherActivity.this.chooseDownloadOs();
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$14 */
    class C017514 implements OnMenuItemClickListener {
        C017514() {
        }

        public boolean onMenuItemClick(MenuItem item) {
            return true;
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$15 */
    class C017615 implements OnMenuItemClickListener {
        C017615() {
        }

        public boolean onMenuItemClick(MenuItem item) {

            return true;
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$16 */
    class C017716 implements DialogInterface.OnClickListener {
        C017716() {
        }

        public void onClick(DialogInterface dialog, int which) {
            TetherActivity.this.mShowingQuota = false;
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$17 */
    class C017817 implements OnCancelListener {
        C017817() {
        }

        public void onCancel(DialogInterface dialog) {
            TetherActivity.this.mShowingQuota = false;
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$1 */
    class C01791 extends BroadcastReceiver {

        /* renamed from: com.koushikdutta.tether.TetherActivity$1$1 */
        class C01681 implements DialogInterface.OnClickListener {
            C01681() {
            }

            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.setData(Uri.parse("market://details?id=com.koushikdutta.tether"));
                TetherActivity.this.startActivity(i);
            }
        }

        /* renamed from: com.koushikdutta.tether.TetherActivity$1$2 */
        class C01692 implements DialogInterface.OnClickListener {
            C01692() {
            }

            public void onClick(DialogInterface dialog, int which) {
                TetherActivity.this.tetherSupport();
            }
        }

        C01791() {
        }

        public void onReceive(Context context, Intent intent) {
            TetherActivity.this.mConnected = intent.getBooleanExtra("connected", false);
            TetherActivity.this.mReceived = intent.getIntExtra("received", 0);
            TetherActivity.this.mSent = intent.getIntExtra("sent", 0);
            if (TetherActivity.this.mReceived != 0 || TetherActivity.this.mSent != 0) {
                TetherActivity.this.mDataStats.setVisibility(View.VISIBLE);
                TextView s = (TextView) TetherActivity.this.findViewById(R.id.data_sent);
                ((TextView) TetherActivity.this.findViewById(R.id.data_received)).setText(TetherActivity.this.getBytes(TetherActivity.this.mReceived));
                s.setText(TetherActivity.this.getBytes(TetherActivity.this.mSent));
                TetherActivity.this.refreshAdb(false);
            }
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$2 */
    class C01802 implements Runnable {
        C01802() {
        }

        public void run() {
            TetherActivity.this.refreshAdb(true);
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$3 */
    class C01813 implements OnClickListener {
        C01813() {
        }

        public void onClick(View v) {
            Intent i;
            try {
                i = new Intent();
                i.setClassName("com.android.settings", "com.android.settings.Settings$DevelopmentSettingsActivity");
                TetherActivity.this.startActivity(i);
            } catch (Exception e) {
                try {
                    i = new Intent();
                    i.setClassName("com.android.settings", "com.android.settings.DevelopmentSettingsActivity");
                    TetherActivity.this.startActivity(i);
                } catch (Exception e2) {
                }
            }
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$5 */
    class C01845 implements DialogInterface.OnClickListener {
        C01845() {
        }

        public void onClick(DialogInterface dialog, int which) {
            Intent i = new Intent();
            i.setData(Uri.parse("market://details?id=com.koushikdutta.tether"));
            TetherActivity.this.startActivity(i);
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherActivity$6 */
    class C01856 implements OnCancelListener {
        C01856() {
        }

        public void onCancel(DialogInterface dialog) {
            TetherActivity.this.mCancelledDownload = true;
        }
    }

    /* Access modifiers changed, original: protected */
    public void onStart() {
        super.onStart();
    }

    public boolean isConnected() {
        int plugged = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("plugged", 0);
        if (plugged == 2 || plugged == 1) {
            return true;
        }
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Failed to extract finally block: empty outs */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void refreshAdb(boolean repost) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (Settings.Global.getInt(getContentResolver(), "adb_enabled") == 0) {
                    this.mDataStats.setVisibility(View.GONE);
                    this.mHelpIcon.setVisibility(View.VISIBLE);
                    this.mUsbText.setText(R.string.usb_on_no_adb);
                    this.mUsbIcon.setImageResource(R.drawable.usb_broken);
                    this.mUsbIcon.setOnClickListener(new C01813());
                    stopService(new Intent(this, TetherService.class));
                    if (repost) {
                        this.mHandler.postDelayed(new C01802(), 1000);
                        return;
                    }
                    return;
                }
            }
            boolean _enabled = false;
            for (RunningServiceInfo service : ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                if (TetherService.class.getName().equals(service.service.getClassName())) {
                    _enabled = true;
                    break;
                }
            }
            final boolean enabled = _enabled;
            AnimatedView.setOnClickListener(this.mUsbIcon, new OnClickListener() {

                /* renamed from: com.koushikdutta.tether.TetherActivity$4$1 */
                class C01821 implements DialogInterface.OnClickListener {
                    C01821() {
                    }

                    public void onClick(DialogInterface dialog, int which) {
                        TetherActivity.this.stopService(new Intent(TetherActivity.this, TetherService.class));
                    }
                }

                public void onClick(View v) {
                    ScaleAnimation scale = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f, 1, 0.5f, 1, 0.5f);
                    scale.setDuration(250);
                    TetherActivity.this.mUsbIcon.setAnimation(scale);
                    if (enabled) {
                        Builder builder = new Builder(TetherActivity.this);
                        builder.setPositiveButton("Yes", new C01821());
                        builder.setNegativeButton("No", null);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(R.string.confirm_tether_off);
                        builder.create().show();
                    } else if (Helper.mExpired) {
                        Helper.mExpired = false;
                    } else {
                        TetherActivity.this.startService(new Intent(TetherActivity.this, TetherService.class));
                    }
                    TetherActivity.this.refreshAdb(true);
                }
            });
            if (!enabled) {
                this.mDataStats.setVisibility(View.GONE);
                this.mUsbText.setText(R.string.tether_disabled);
                this.mUsbIcon.setImageResource(R.drawable.usb_off);
                this.mHelpIcon.setVisibility(View.VISIBLE);
                if (repost) {
                    this.mHandler.postDelayed(new C01802(), 1000);
                }
            } else if (isConnected()) {
                if (this.mConnected) {
                    this.mDataStats.setVisibility(View.VISIBLE);
                    this.mHelpIcon.setVisibility(View.GONE);
                    this.mUsbIcon.setImageResource(R.drawable.usb_on);
                    this.mUsbText.setText(R.string.usb_on);

                    if (Helper.mPurchased) {
                        this.mQuotaLayout.setVisibility(View.GONE);
                    } else {
                        int percent;
                        this.mQuotaLayout.setVisibility(View.VISIBLE);
                        int daysLeft;
                        if (Helper.mTrialStartDate + Helper.TRIAL_DURATION < System.currentTimeMillis()) {
                            percent = (int) ((100 * (Helper.TRIAL_LIMIT - Helper.mDailyTrialUsed)) / Helper.TRIAL_LIMIT);
                            this.mQuotaStatus.setText(R.string.daily_trial_quota);
                        } else if (Helper.mTrialStartDate != 0) {
                            long timeLeft = (Helper.mTrialStartDate + Helper.TRIAL_DURATION) - System.currentTimeMillis();
                            daysLeft = (int) (timeLeft / 86400000);
                            percent = (int) ((100 * timeLeft) / Helper.TRIAL_DURATION);
                            this.mQuotaStatus.setText(getString(R.string.days_left, new Object[]{Integer.valueOf(daysLeft)}));
                        } else {
                            percent = 0;
                            daysLeft = (int) (Helper.TRIAL_DURATION / 86400000);
                            this.mQuotaStatus.setText(getString(R.string.days_left, new Object[]{Integer.valueOf(daysLeft)}));
                        }
                        this.mQuotaProgress.setProgress(percent);
                    }
                } else {
                    this.mDataStats.setVisibility(View.GONE);
                    this.mHelpIcon.setVisibility(View.VISIBLE);
                    this.mUsbText.setText(R.string.usb_on_no_data);
                    this.mUsbIcon.setImageResource(R.drawable.usb_pending);
                }
                if (repost) {
                    this.mHandler.postDelayed(new C01802(), 1000);
                }
            } else {
                this.mUsbText.setText(R.string.usb_off);
                this.mUsbIcon.setImageResource(R.drawable.usb_broken);
                this.mDataStats.setVisibility(View.GONE);
                this.mHelpIcon.setVisibility(View.VISIBLE);
                this.mReceived = 0;
                this.mSent = 0;
                if (repost) {
                    this.mHandler.postDelayed(new C01802(), 1000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (repost) {
                this.mHandler.postDelayed(new C01802(), 1000);
            }
        } catch (Throwable th) {
            if (repost) {
                this.mHandler.postDelayed(new C01802(), 1000);
            }
            throw th;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void tetherIsSlow() {
        Builder tetherSlow = new Builder(this);
        tetherSlow.setTitle(R.string.tether_is_slow);
        tetherSlow.setMessage(R.string.tether_is_slow_info);
        tetherSlow.setPositiveButton("Ok", null);
        tetherSlow.create().show();
    }

    /* Access modifiers changed, original: 0000 */
    public void tetherWontConnect() {
        Builder tetherSlow = new Builder(this);
        tetherSlow.setTitle(R.string.tether_will_not_connect);
        tetherSlow.setMessage(R.string.tether_connect_info);
        tetherSlow.setPositiveButton("Ok", null);
        tetherSlow.create().show();
    }

    /* Access modifiers changed, original: 0000 */
    public void tetherSupport() {
        Builder email = new Builder(this);
        email.setTitle(R.string.support);
        email.setMessage(R.string.support_info);
        email.setNegativeButton("Ok", null);
        email.setPositiveButton(R.string.market, new C01845());
        email.create().show();
    }

    /* Access modifiers changed, original: 0000 */
    public void handleDownloadFinished() {
        Builder done = new Builder(this);
        done.setTitle(R.string.download_complete);
        done.setMessage(R.string.download_complete_info);
        done.setPositiveButton("Ok", null);
        done.create().show();
    }

    /* Access modifiers changed, original: 0000 */
    public void handleDownload(int osId) {
        int resource;
        String _filename;
        this.mCancelledDownload = false;
        Builder builder = new Builder(this);
        View content = getLayoutInflater().inflate(R.layout.downloading, null);
        builder.setView(content);
        builder.setOnCancelListener(new C01856());
        final Dialog dialog = builder.create();
        final ProgressBar progress = (ProgressBar) content.findViewById(R.id.progress);
        final Runnable errorHandler = new Runnable() {
            public void run() {
                dialog.dismiss();
                Builder error = new Builder(TetherActivity.this);
                error.setTitle(R.string.error_downloading);
                error.setMessage(R.string.error_downloading_info);
                error.setPositiveButton("Ok", null);
                error.create().show();
            }
        };
        ImageView os = (ImageView) content.findViewById(R.id.os);
        final TextView title = (TextView) content.findViewById(R.id.downloading);
        if (osId == 2131099669) {
            resource = R.drawable.apple;
            _filename = "tether-mac.zip";
        } else if (osId == 2131099668) {
            resource = R.drawable.windows;
            _filename = "TetherWindowsSetup.msi";
        } else {
            resource = R.drawable.linux;
            _filename = "tether-linux.tgz";
        }
        final String filename = _filename;
        os.setImageResource(resource);
        dialog.show();
        final String localFile = new StringBuilder(String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath())).append("/clockworkmod/tether/").append(filename).toString();
        try {
            final int i = osId;
            new Thread() {
                int last = 0;

                public void run() {
                    Handler handler;
                    try {
                        JSONObject config = StreamUtility.downloadUriAsJSONObject(TetherActivity.CONFIG_URL);
                        String format = String.format(config.getString("tether_url"), new Object[]{filename});
                        String str = localFile;
                        final ProgressBar progressBar = progress;
                        StreamUtility.downloadUri(format, str, new Callback<Float, Boolean>() {
                            public Boolean onCallback(Float result) {
                                final int current = (int) result.floatValue();
                                if (current != last) {
                                    Handler handler = TetherActivity.this.mHandler;
                                    handler.post(new Runnable() {
                                        public void run() {
                                            progressBar.setProgress(current);
                                        }
                                    });
                                    last = current;
                                }
                                return Boolean.valueOf(!TetherActivity.this.mCancelledDownload);
                            }
                        });
                        if (i == R.id.windows) {
                            try {
                                JSONObject manufacturer = config.getJSONObject("drivers").getJSONObject(System.getProperty("ro.product.manufacturer"));
                                String driver = manufacturer.optString(System.getProperty("ro.product.device"), manufacturer.optString("universal", null));
                                if (driver != null) {
                                    String driverUrl = String.format(config.getString("driver_url"), new Object[]{driver});
                                    handler = TetherActivity.this.mHandler;
                                    final ProgressBar progressBar2 = progress;
                                    final TextView textView = title;
                                    handler.post(new Runnable() {
                                        public void run() {
                                            progressBar2.setProgress(0);
                                            textView.setText(R.string.downloading_drivers);
                                        }
                                    });
                                    String localFile = new StringBuilder(String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath())).append("/clockworkmod/tether/").append(driver).toString();
                                    final ProgressBar progressBar3 = progress;
                                    StreamUtility.downloadUri(driverUrl, localFile, new Callback<Float, Boolean>() {
                                        public Boolean onCallback(Float result) {
                                            final int current = (int) result.floatValue();
                                            if (current != last) {
                                                Handler handler = TetherActivity.this.mHandler;
                                                final ProgressBar progressBar = progressBar3;
                                                handler.post(new Runnable() {
                                                    public void run() {
                                                        progressBar.setProgress(current);
                                                    }
                                                });
                                                last = current;
                                            }
                                            return Boolean.valueOf(!TetherActivity.this.mCancelledDownload);
                                        }
                                    });
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        handler = TetherActivity.this.mHandler;
                        handler.post(new Runnable() {
                            public void run() {
                                dialog.dismiss();
                                TetherActivity.this.handleDownloadFinished();
                            }
                        });
                    } catch (Exception e) {
                        handler = TetherActivity.this.mHandler;
                        final Runnable runnable = errorHandler;
                        handler.post(new Runnable() {
                            public void run() {
                                runnable.run();
                            }
                        });
                    }
                }
            }.start();
        } catch (Exception e) {
            errorHandler.run();
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void handleOS(final Dialog dialog, View view, final int osId) {
        AnimatedView.setOnClickListener(view.findViewById(osId), new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                TetherActivity.this.handleDownload(osId);
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void chooseDownloadOs() {
        Builder builder = new Builder(this);
        View content = getLayoutInflater().inflate(R.layout.os_choice, null);
        builder.setView(content);
        Dialog dialog = builder.create();
        handleOS(dialog, content, R.id.mac);
        handleOS(dialog, content, R.id.windows);
        handleOS(dialog, content, R.id.linux);
        dialog.show();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.mSettings = getSharedPreferences("settings", 0);
        this.mUsbText = (TextView) findViewById(R.id.usb_status);
        this.mUsbIcon = (ImageView) findViewById(R.id.usb_icon);
        this.mDataStats = (LinearLayout) findViewById(R.id.data_stats);
        this.mHelpIcon = findViewById(R.id.help);
        this.mQuotaLayout = findViewById(R.id.quota_layout);
        this.mQuotaProgress = (ProgressBar) findViewById(R.id.quota);
        this.mQuotaStatus = (TextView) findViewById(R.id.quota_status);
        this.mHasWorked = this.mSettings.getBoolean("has_worked", false);
        this.mQuotaLayout.setOnClickListener(new C017010());
        AnimatedView.setOnClickListener(this.mHelpIcon, new C017211());
        refreshAdb(true);
        registerReceiver(this.mReceiver, new IntentFilter("com.koushikdutta.tether.TETHER_STATS"));
    }

    /* Access modifiers changed, original: 0000 */
    public String getBytes(int bytes) {
        if (bytes > 1024) {
            if (((double) bytes) / 1024.0d > 1024.0d) {
                return getString(R.string.megabytes, new Object[]{Double.valueOf((((double) bytes) / 1024.0d) / 1024.0d)});
            }
            return getString(R.string.kilobytes, new Object[]{Double.valueOf(((double) bytes) / 1024.0d)});
        }
        return getString(R.string.bytes, new Object[]{Integer.valueOf(bytes)});
    }

    private void tetherAndroidOutdated() {
        Builder builder = new Builder(this);
        builder.setTitle(R.string.tether_version);
        builder.setMessage(R.string.tether_android_outdated);
        builder.setPositiveButton("Ok", new C017312());
        builder.create().show();
    }

    private void tetherOutdated() {
        Builder builder = new Builder(this);
        builder.setTitle(R.string.tether_version);
        builder.setMessage(R.string.tether_pc_outdated);
        builder.setPositiveButton("Ok", new C017413());
        builder.create().show();
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        this.mDestroyed = true;
        unregisterReceiver(this.mReceiver);
    }


    public boolean onPrepareOptionsMenu(Menu menu) {
        if (Helper.mPurchased) {
            this.mBuyMenuItem.setVisible(false);
            this.mRecoverMenuItem.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    /* Access modifiers changed, original: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("expired", false) && !this.mShowingQuota) {
            String message = getString(R.string.trial_quota_exceeded_message, new Object[]{Long.valueOf(Helper.TRIAL_LIMIT / 1000000), Long.valueOf((((Helper.mDailyReset - System.currentTimeMillis()) / 60) / 60) / 1000)});
            Builder builder = new Builder(this);
            builder.setMessage(message);
            builder.setTitle(R.string.trial_quota_exceeded);
            builder.setIcon(R.drawable.cross_small);
            this.mShowingQuota = true;
            builder.setPositiveButton(R.string.buy_tether, new C017716());
            builder.setOnCancelListener(new C017817());
            builder.create().show();
        }
    }
}
