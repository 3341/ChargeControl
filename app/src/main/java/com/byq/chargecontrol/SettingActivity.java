package com.byq.chargecontrol;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.FileIOUtils;
import com.byq.applib.GsonTools;
import com.byq.chargecontrol.shell.Terminator;
import com.byq.chargecontrol.tools.TextINTool;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

import es.dmoral.toasty.Toasty;

public class SettingActivity extends AppCompatActivity {
    private TextInputLayout mConfigJsonLayout;
    private TextInputEditText mConfigJsonEdit;
    private MaterialButton mResetConfig;
    private MaterialButton mCommitButton;
    private TextInputLayout mTestErrorMessageLayout;
    private TextInputEditText mTestErrorMessageEdit;
    private MaterialButton mStopTest;
    private MaterialButton mStartTest;
    private MaterialButton mLowCharge;
    private MaterialButton mFastCharge;


    private ChargeController mChargeController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏图标和文字颜色为暗色
        setContentView(R.layout.activity_setting);


        mConfigJsonLayout = findViewById(R.id.configJsonLayout);
        mConfigJsonEdit = findViewById(R.id.configJsonEdit);
        mResetConfig = findViewById(R.id.resetConfig);
        mCommitButton = findViewById(R.id.commitButton);
        mTestErrorMessageLayout = findViewById(R.id.testErrorMessageLayout);
        mTestErrorMessageEdit = findViewById(R.id.testErrorMessageEdit);
        mStopTest = findViewById(R.id.stopTest);
        mStartTest = findViewById(R.id.startTest);
        mLowCharge = findViewById(R.id.lowCharge);
        mFastCharge = findViewById(R.id.fastCharge);


        //Read config file
        File file = new File(getFilesDir(),"autoCheckConfig.json");
        String content = FileIOUtils.readFile2String(file);
        mConfigJsonEdit.setText(content);

        TextINTool.setAutoDismissErrorTip(false,mConfigJsonLayout,mConfigJsonEdit);
        mCommitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean b = GsonTools.setGsonConfig(SettingActivity.this,
                        mConfigJsonEdit.getText().toString(),
                        AutoCheckConfig.class, file);
                if (b) {
                    Toasty.success(SettingActivity.this,"提交成功").show();
                } else {
                    mConfigJsonLayout.setErrorEnabled(true);
                    mConfigJsonLayout.setError("无法识别的Json格式");
                }
            }
        });

        mResetConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputStream open = getAssets().open("autoCheckConfig.json");
                    StringBuilder sb = new StringBuilder();
                    Scanner scanner = new Scanner(open);
                    while(scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                    }
                    if (sb.length()!=0) {
                        sb.deleteCharAt(sb.length()-1);
                    }
                    open.close();
                    mConfigJsonEdit.setText(content);
                    mCommitButton.performClick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChargeController == null) {
                    mChargeController = new ChargeController(SettingActivity.this);
                    mChargeController.initialize();
                    mChargeController.getmTerminator().setmErrorReceiver(new Terminator.ErrorReceiver() {
                        @Override
                        public void onReceived(String msg) {
                            mTestErrorMessageLayout.setVisibility(View.VISIBLE);
                            if (!mTestErrorMessageEdit.getText().toString().isEmpty()) {
                                mTestErrorMessageEdit.append("\n");
                            }
                            mTestErrorMessageEdit.append(msg);
                        }
                    });
                    Toasty.success(SettingActivity.this,"测试开始").show();
                } else {
                    Toasty.error(SettingActivity.this,"测试已经开始了").show();
                }
            }
        });

        mStopTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChargeController != null) {
                    mChargeController.terminate();
                    mChargeController = null;
                    Toasty.success(SettingActivity.this,"测试终止").show();
                    mTestErrorMessageLayout.setVisibility(View.GONE);
                    mTestErrorMessageEdit.setText("");
                } else {
                    Toasty.error(SettingActivity.this,"测试已经终止了").show();
                }
            }
        });

        mFastCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChargeController != null) {
                    mChargeController.resumeCharge();
                }
            }
        });

        mLowCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChargeController != null) {
                    mChargeController.disableCharge();
                }
            }
        });
    }
}