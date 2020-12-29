package com.koushikdutta.tether;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;

import java.util.Iterator;

public class TetherActivity extends Activity {
    public static final String CONFIG_URL = "http://www.clockworkmod.com/tether/config.js";
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
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            TetherActivity.this.mConnected = intent.getBooleanExtra("connected", false);
            TetherActivity.this.mReceived = intent.getIntExtra("received", 0);
            TetherActivity.this.mSent = intent.getIntExtra("sent", 0);
            if (TetherActivity.this.mReceived != 0 || TetherActivity.this.mSent != 0) {
                TetherActivity.this.mDataStats.setVisibility(View.VISIBLE);
                ((TextView) TetherActivity.this.findViewById(R.id.data_received)).setText(TetherActivity.this.getBytes(TetherActivity.this.mReceived));
                ((TextView) TetherActivity.this.findViewById(R.id.data_sent)).setText(TetherActivity.this.getBytes(TetherActivity.this.mSent));
                TetherActivity.this.refreshAdb(false);
                if (System.currentTimeMillis() > TetherActivity.this.mVersionExpiration && TetherActivity.this.mVersionExpiration != 0) {
                    TetherActivity.this.mVersionExpiration = 0;
                    int version = intent.getIntExtra("version", 0);
                    if (version < 6) {
                        TetherActivity.this.tetherOutdated();
                    } else if (version > 6) {
                        TetherActivity.this.tetherAndroidOutdated();
                    } else if (!TetherActivity.this.mHasWorked) {
                        TetherActivity.this.mHasWorked = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(TetherActivity.this);
                        builder.setTitle(R.string.tether_successful);
                        builder.setMessage(R.string.tether_successful_info);
                        builder.setPositiveButton(R.string.rate_tether, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent();
                                i.setData(Uri.parse("market://details?id=com.koushikdutta.tether"));
                                TetherActivity.this.startActivity(i);
                            }
                        });
                        builder.setNeutralButton(R.string.support, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                TetherActivity.this.tetherSupport();
                            }
                        });
                        SharedPreferences.Editor editor = TetherActivity.this.mSettings.edit();
                        editor.putBoolean("has_worked", true);
                        editor.commit();
                        builder.create().show();
                    }
                }
            }
        }
    };
    MenuItem mRecoverMenuItem;
    int mSent = 0;
    SharedPreferences mSettings;
    boolean mShowingQuota = false;
    ImageView mUsbIcon;
    TextView mUsbText;
    long mVersionExpiration = Long.MAX_VALUE;

    public void onStart() {
        super.onStart();
    }

    public boolean isConnected() {
        int plugged = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("plugged", 0);
        return plugged == 2 || plugged == 1;
    }

    public void refreshAdb(boolean repost) {
        int percent;
        try {
            if (Settings.Secure.getInt(getContentResolver(), "adb_enabled") == 0) {
                this.mDataStats.setVisibility(View.GONE);
                this.mHelpIcon.setVisibility(View.VISIBLE);
                this.mUsbText.setText(R.string.usb_on_no_adb);
                this.mUsbIcon.setImageResource(R.drawable.usb_broken);
                this.mUsbIcon.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent i = new Intent();
                            i.setClassName("com.android.settings", "com.android.settings.Settings$DevelopmentSettingsActivity");
                            TetherActivity.this.startActivity(i);
                        } catch (Exception e) {
                            try {
                                Intent i2 = new Intent();
                                i2.setClassName("com.android.settings", "com.android.settings.DevelopmentSettingsActivity");
                                TetherActivity.this.startActivity(i2);
                            } catch (Exception e2) {
                            }
                        }
                    }
                });
                stopService(new Intent(this, TetherService.class));
                if (repost) {
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            TetherActivity.this.refreshAdb(true);
                        }
                    }, 1000);
                    return;
                }
                return;
            }
            boolean _enabled = false;
            @SuppressLint("WrongConstant") Iterator<ActivityManager.RunningServiceInfo> it = ((ActivityManager) getSystemService("activity")).getRunningServices(Integer.MAX_VALUE).iterator();
            while (true) {
                if (it.hasNext()) {
                    if (TetherService.class.getName().equals(it.next().service.getClassName())) {
                        _enabled = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            final boolean enabled = _enabled;
            AnimatedView.setOnClickListener(this.mUsbIcon, new View.OnClickListener() {
                public void onClick(View v) {
                    ScaleAnimation scale = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f, 1, 0.5f, 1, 0.5f);
                    scale.setDuration(250);
                    TetherActivity.this.mUsbIcon.setAnimation(scale);
                    if (enabled) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(TetherActivity.this);
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                TetherActivity.this.stopService(new Intent(TetherActivity.this, TetherService.class));
                            }
                        });
                        builder.setNegativeButton("No", (DialogInterface.OnClickListener) null);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(R.string.confirm_tether_off);
                        builder.create().show();
                    } else if (Helper.mExpired) {
                        Helper.mExpired = false;
                    } else {
                        TetherActivity.this.mVersionExpiration = Long.MAX_VALUE;
                        TetherActivity.this.startService(new Intent(TetherActivity.this, TetherService.class));
                    }
                    TetherActivity.this.refreshAdb(false);
                }
            });
            if (!enabled) {
                this.mDataStats.setVisibility(View.GONE);
                this.mUsbText.setText(R.string.tether_disabled);
                this.mUsbIcon.setImageResource(R.drawable.usb_off);
                this.mHelpIcon.setVisibility(View.VISIBLE);
                if (repost) {
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            TetherActivity.this.refreshAdb(true);
                        }
                    }, 1000);
                }
            } else if (!isConnected()) {
                this.mUsbText.setText(R.string.usb_off);
                this.mUsbIcon.setImageResource(R.drawable.usb_broken);
                this.mDataStats.setVisibility(View.GONE);
                this.mHelpIcon.setVisibility(View.VISIBLE);
                this.mReceived = 0;
                this.mSent = 0;
                if (repost) {
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            TetherActivity.this.refreshAdb(true);
                        }
                    }, 1000);
                }
            } else {
                if (!this.mConnected) {
                    this.mDataStats.setVisibility(View.GONE);
                    this.mHelpIcon.setVisibility(View.VISIBLE);
                    this.mUsbText.setText(R.string.usb_on_no_data);
                    this.mUsbIcon.setImageResource(R.drawable.usb_pending);
                } else {
                    this.mDataStats.setVisibility(View.GONE);
                    this.mHelpIcon.setVisibility(View.VISIBLE);
                    this.mUsbIcon.setImageResource(R.drawable.usb_on);
                    this.mUsbText.setText(R.string.usb_on);
                    if (this.mVersionExpiration == Long.MAX_VALUE) {
                        this.mVersionExpiration = System.currentTimeMillis() + 5000;
                    }
                    if (!Helper.mPurchased) {
                        this.mQuotaLayout.setVisibility(View.VISIBLE);
                        if (Helper.mTrialStartDate + Helper.TRIAL_DURATION < System.currentTimeMillis()) {
                            percent = (int) ((100 * (Helper.TRIAL_LIMIT - Helper.mDailyTrialUsed)) / Helper.TRIAL_LIMIT);
                            this.mQuotaStatus.setText(R.string.daily_trial_quota);
                        } else if (Helper.mTrialStartDate != 0) {
                            long timeLeft = (Helper.mTrialStartDate + Helper.TRIAL_DURATION) - System.currentTimeMillis();
                            percent = (int) ((100 * timeLeft) / Helper.TRIAL_DURATION);
                            this.mQuotaStatus.setText(getString(R.string.days_left, (int) (timeLeft / 86400000)));
                        } else {
                            percent = 0;
                            int daysLeft = (int) (Helper.TRIAL_DURATION / 86400000);
                            this.mQuotaStatus.setText(getString(R.string.days_left, daysLeft));
                        }
                        this.mQuotaProgress.setProgress(percent);
                    } else {
                        this.mQuotaLayout.setVisibility(View.GONE);
                    }
                }
                if (repost) {
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            TetherActivity.this.refreshAdb(true);
                        }
                    }, 1000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (repost) {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        TetherActivity.this.refreshAdb(true);
                    }
                }, 1000);
            }
        } catch (Throwable th) {
            if (repost) {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        TetherActivity.this.refreshAdb(true);
                    }
                }, 1000);
            }
            throw th;
        }
    }

    public void tetherIsSlow() {
        AlertDialog.Builder tetherSlow = new AlertDialog.Builder(this);
        tetherSlow.setTitle(R.string.tether_is_slow);
        tetherSlow.setMessage(R.string.tether_is_slow_info);
        tetherSlow.setPositiveButton("Ok",  null);
        tetherSlow.create().show();
    }


    public void tetherWontConnect() {
        AlertDialog.Builder tetherSlow = new AlertDialog.Builder(this);
        tetherSlow.setTitle(R.string.tether_will_not_connect);
        tetherSlow.setMessage(R.string.tether_connect_info);
        tetherSlow.setPositiveButton("Ok", null);
        tetherSlow.create().show();
    }


    public void tetherSupport() {
        AlertDialog.Builder email = new AlertDialog.Builder(this);
        email.setTitle(R.string.support);
        email.setMessage(R.string.support_info);
        email.setNegativeButton("Ok", null);
        email.setPositiveButton(R.string.market, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.setData(Uri.parse("market://details?id=com.koushikdutta.tether"));
                TetherActivity.this.startActivity(i);
            }
        });
        email.create().show();
    }


    public void handleDownloadFinished() {
        AlertDialog.Builder done = new AlertDialog.Builder(this);
        done.setTitle(R.string.download_complete);
        done.setMessage(R.string.download_complete_info);
        done.setPositiveButton("Ok", null);
        done.create().show();
    }

    public void handleDownload(int osId) {
        int resource;
        String _filename;
        this.mCancelledDownload = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View content = getLayoutInflater().inflate(R.layout.downloading, (ViewGroup) null);
        builder.setView(content);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                TetherActivity.this.mCancelledDownload = true;
            }
        });
        final Dialog dialog = builder.create();
        final ProgressBar progress = (ProgressBar) content.findViewById(R.id.progress);
        final Runnable errorHandler = new Runnable() {
            public void run() {
                dialog.dismiss();
                AlertDialog.Builder error = new AlertDialog.Builder(TetherActivity.this);
                error.setTitle(R.string.error_downloading);
                error.setMessage(R.string.error_downloading_info);
                error.setPositiveButton("Ok", (DialogInterface.OnClickListener) null);
                error.create().show();
            }
        };
        ImageView os = content.findViewById(R.id.os);
        final TextView title = content.findViewById(R.id.downloading);
        if (osId == R.id.mac) {
            resource = R.drawable.apple;
            _filename = "tether-mac.zip";
        } else if (osId == R.id.windows) {
            resource = R.drawable.windows;
            _filename = "TetherWindowsSetup.msi";
        } else {
            resource = R.drawable.linux;
            _filename = "tether-linux.tgz";
        }
        final String filename = _filename;
        os.setImageResource(resource);
        dialog.show();
        final String localFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/clockworkmod/tether/" + filename;
        try {
            final int i = osId;
            new Thread() {
                int last = 0;

                public void run() {
                    try {
                        JSONObject config = StreamUtility.downloadUriAsJSONObject(TetherActivity.CONFIG_URL);
                        StreamUtility.downloadUri(String.format(config.getString("tether_url"), filename), localFile, new Callback<Float, Boolean>() {
                            public Boolean onCallback(Float result) {
                                final int current = (int) result.floatValue();
                                if (current != last) {
                                    TetherActivity.this.mHandler.post(new Runnable() {
                                        public void run() {
                                            progress.setProgress(current);
                                        }
                                    });
                                    last = current;
                                }
                                return !TetherActivity.this.mCancelledDownload;
                            }
                        });
                        if (i == R.id.windows) {
                            try {
                                JSONObject manufacturer = config.getJSONObject("drivers").getJSONObject(System.getProperty("ro.product.manufacturer"));
                                String driver = manufacturer.optString(System.getProperty("ro.product.device"), manufacturer.optString("universal", (String) null));
                                if (driver != null) {
                                    String driverUrl = String.format(config.getString("driver_url"), new Object[]{driver});
                                    TetherActivity.this.mHandler.post(new Runnable() {
                                        public void run() {
                                            progress.setProgress(0);
                                            title.setText(R.string.downloading_drivers);
                                        }
                                    });
                                    StreamUtility.downloadUri(driverUrl, Environment.getExternalStorageDirectory().getAbsolutePath() + "/clockworkmod/tether/" + driver, new Callback<Float, Boolean>() {
                                        public Boolean onCallback(Float result) {
                                            final int current = (int) result.floatValue();
                                            if (current != last) {
                                                TetherActivity.this.mHandler.post(new Runnable() {
                                                    public void run() {
                                                        progress.setProgress(current);
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
                        TetherActivity.this.mHandler.post(new Runnable() {
                            public void run() {
                                dialog.dismiss();
                                TetherActivity.this.handleDownloadFinished();
                            }
                        });
                    } catch (Exception e) {
                        TetherActivity.this.mHandler.post(new Runnable() {
                            public void run() {
                                errorHandler.run();
                            }
                        });
                    }
                }
            }.start();
        } catch (Exception e) {
            errorHandler.run();
        }
    }

    public void handleOS(final Dialog dialog, View view, final int osId) {
        AnimatedView.setOnClickListener(view.findViewById(osId), new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                TetherActivity.this.handleDownload(osId);
            }
        });
    }

    public void chooseDownloadOs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View content = getLayoutInflater().inflate(R.layout.os_choice, (ViewGroup) null);
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
        this.mUsbText =  findViewById(R.id.usb_status);
        this.mUsbIcon =  findViewById(R.id.usb_icon);
        this.mDataStats =  findViewById(R.id.data_stats);
        this.mHelpIcon = findViewById(R.id.help);
        this.mQuotaLayout = findViewById(R.id.quota_layout);
        this.mQuotaProgress = findViewById(R.id.quota);
        this.mQuotaStatus = findViewById(R.id.quota_status);
        this.mHasWorked = this.mSettings.getBoolean("has_worked", false);
        this.mQuotaLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // removed purchase trigger
            }
        });
        AnimatedView.setOnClickListener(this.mHelpIcon, new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TetherActivity.this);
                builder.setTitle(R.string.help);
                builder.setItems(new CharSequence[]{TetherActivity.this.getString(R.string.download_pc), TetherActivity.this.getString(R.string.tether_is_slow), TetherActivity.this.getString(R.string.tether_will_not_connect), TetherActivity.this.getString(R.string.support)}, new DialogInterface.OnClickListener() {
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
                        }
                    }
                });
                builder.create().show();
            }
        });
        refreshAdb(true);
        registerReceiver(this.mReceiver, new IntentFilter("com.koushikdutta.tether.TETHER_STATS"));
    }

    public String getBytes(int bytes) {
        if (bytes > 1024) {
            double kb = ((double) bytes) / 1024.0d;
            if (kb > 1024.0d) {
                return getString(R.string.megabytes, kb / 1024.0d);
            }
            return getString(R.string.kilobytes, kb);
        }
        return getString(R.string.bytes, bytes);
    }

    /* access modifiers changed from: private */
    public void tetherAndroidOutdated() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tether_version);
        builder.setMessage(R.string.tether_android_outdated);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.setData(Uri.parse("market://details?id=com.koushikdutta.tether"));
                TetherActivity.this.startActivity(i);
            }
        });
        builder.create().show();
    }


    public void tetherOutdated() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tether_version);
        builder.setMessage(R.string.tether_pc_outdated);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                TetherActivity.this.chooseDownloadOs();
            }
        });
        builder.create().show();
    }


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

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("expired", false) && !this.mShowingQuota) {
            String message = getString(R.string.trial_quota_exceeded_message, Helper.TRIAL_LIMIT / 1000000, (((Helper.mDailyReset - System.currentTimeMillis()) / 60) / 60) / 1000);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message);
            builder.setTitle(R.string.trial_quota_exceeded);
            builder.setIcon(R.drawable.cross_small);
            this.mShowingQuota = true;
            builder.setPositiveButton(R.string.buy_tether, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    TetherActivity.this.mShowingQuota = false;
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    TetherActivity.this.mShowingQuota = false;
                }
            });
            builder.create().show();
        }
    }
}