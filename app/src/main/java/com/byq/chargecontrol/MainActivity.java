package com.byq.chargecontrol;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ServiceUtils;
import com.byq.applib.FileTools;
import com.byq.chargecontrol.shell.Terminator;
import com.google.android.material.button.MaterialButton;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;
import com.lxj.xpopup.interfaces.OnSelectListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {
    private Terminator terminator;
    private static final String DISABLE_CHARGE_PATH = "disable_charge.sh";
    private static final String RESUME_CHARGE_PATH = "resume_charge.sh";
    private Handler handler = new Handler();
    private final static String TAG = BuildConfig.APPLICATION_ID;
    private MainService mMainService;
    private PowerStatusParser powerStatusParser;

    //Views
    private TextView serviceStatus;
    private MaterialButton updateServiceStatus;
    private MaterialButton startService;
    private MaterialButton settingButton;
    private MaterialButton stopService;
    private MaterialButton pauseCharge;
    private MaterialButton resumeCharge;
    private MaterialButton disableOrEnableAutoCheck;
    private TextView batterayStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏图标和文字颜色为暗色
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        serviceStatus = findViewById(R.id.serviceStatus);
        updateServiceStatus = findViewById(R.id.updateServiceStatus);
        startService = findViewById(R.id.startService);
        settingButton = findViewById(R.id.settingButton);
        stopService = findViewById(R.id.stopService);
        pauseCharge = findViewById(R.id.pauseCharge);
        resumeCharge = findViewById(R.id.resumeCharge);
        disableOrEnableAutoCheck = findViewById(R.id.disableOrEnableAutoCheck);
        batterayStatus = findViewById(R.id.batterayStatus);


        releaseShellFile();

        EventBus.getDefault().post(new MessageEvent(MessageEvent.TRY_CALL_SERVICE));

        updateServiceStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new MessageEvent(MessageEvent.TRY_CALL_SERVICE));
                boolean serviceRunning = ServiceUtils.isServiceRunning(MainService.class);
                new XPopup.Builder(MainActivity.this)
                        .asConfirm("查询结果","服务"+(serviceRunning?"正在运行中":"未运行"),null)
                        .show();
            }
        });

        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasNotifyPermission()) {
                    Toasty.info(MainActivity.this,"请授予通知管理权限以保持服务后台活动").show();
                    requestNotifyPermission();
                }
            }
        });

        startService.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] items = {"设置"};
                new XPopup.Builder(MainActivity.this)
                        .asBottomList("操作", items, new OnSelectListener() {
                            @Override
                            public void onSelect(int position, String text) {
                                startActivity(new Intent(MainActivity.this,SettingActivity.class));
                            }
                        }).show();
                return true;
            }
        });

        powerStatusParser = new PowerStatusParser();
        powerStatusParser.setRefreshListener(new PowerStatusParser.RefreshListener() {
            @Override
            public void onRefresh(PowerStatusParser powerStatusParser) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = findViewById(R.id.batterayStatus);
                        tv.setText(powerStatusParser.toString());
                    }
                });
            }
        });
        powerStatusParser.refresh();
        powerStatusParser.startAutoRefreshThread(1000);

        pauseCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMainService != null) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.REQUEST_DISABLE_CHARGE));
                } else {
                    Toasty.error(MainActivity.this,"Service has not start").show();
                }
            }
        });

        resumeCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMainService != null) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.REQUEST_ENABLE_CHARGE));
                } else {
                    Toasty.error(MainActivity.this,"Service has not start").show();
                }
            }
        });

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,SettingActivity.class));
            }
        });

        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toasty.info(MainActivity.this,"请关闭通知权限以停止Service").show();
                requestNotifyPermission();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        powerStatusParser.stopAutoRefreshThread();
        EventBus.getDefault().unregister(this);
    }

    private void connectedService() {
        serviceStatus.setText("服务已连接");
    }

    @Subscribe
    public void eventbusReceiver(MessageEvent event) {
        switch (event.getEventId()) {
            case MessageEvent.TRY_RESPONSE_ACTIVTY:
                mMainService = (MainService) event.getData();
                connectedService();
                break;
        }
    }

    private void requestNotifyPermission() {
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    private boolean hasNotifyPermission() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void releaseShellFile() {
        File filesDir = getFilesDir();
        if (!filesDir.isDirectory()) filesDir.mkdirs();
        File file = new File(filesDir, "disable_charge.sh");
        if (!file.isFile()) {
            //Release files
            LoadingPopupView initDialog = new XPopup.Builder(this).asLoading("正在初始化");
            initDialog.show();
            new Thread() {
                File f = file;
                @Override
                public void run() {
                    try {
                        f.createNewFile();
                        FileTools.copyAsset(MainActivity.this,"disable_charge.sh",f);
                        f = new File(filesDir,"resume_charge.sh");
                        f.createNewFile();
                        FileTools.copyAsset(MainActivity.this,"resume_charge.sh",f);
                        f = new File(filesDir,"autoCheckConfig.json");
                        f.createNewFile();
                        FileTools.copyAsset(MainActivity.this, "autoCheckConfig.json",f);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initDialog.dismiss();
                                Toasty.success(MainActivity.this,"初始化成功").show();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                initDialog.dismiss();
                                Toasty.error(MainActivity.this,"操作失败").show();
                                Log.e(TAG, "run: failed to initialize" );
                            }
                        });
                        finish();
                    }
                }
            }.start();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,0,0,"检查Root");
        menu.add(0,1,0,"禁止充电");
        menu.add(0,2,0,"启用充电");

        return true;
    }

    private String getShellFilePath(String name) {
        File dir = getFilesDir();
        File f = new File(dir,name);
        return f.getPath();
    }
    
    private void initializeCommand() {
        terminator.command(String.format("chmod 500 %s",getShellFilePath(DISABLE_CHARGE_PATH)));
        terminator.command(String.format("chmod 500 %s",getShellFilePath(RESUME_CHARGE_PATH)));
        Log.i(TAG, "initializeCommand: Permission Initialized.");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0: {
                LoadingPopupView waitDialog = new XPopup.Builder(this).asLoading("请稍后");
                waitDialog.show();
                terminator = new Terminator();
                try {
                    terminator.initializeRootProcess(new Terminator.SuStatusListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    initializeCommand();
                                    waitDialog.setTitle("Success");
                                }
                            });
                        }

                        @Override
                        public void onFailed(String cause) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    waitDialog.setTitle("Failed");
                                }
                            });
                        }
                    },MainActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 1: {
                if (terminator != null) {
                    terminator.command(getShellFilePath(DISABLE_CHARGE_PATH));
                    Toasty.success(MainActivity.this,"已执行").show();
                }
                break;
            }

            case 2: {
                if (terminator != null) {
                    terminator.command(getShellFilePath(RESUME_CHARGE_PATH));
                    Toasty.success(MainActivity.this,"已执行").show();
                }
                break;
            }
        }
        return true;
    }
}