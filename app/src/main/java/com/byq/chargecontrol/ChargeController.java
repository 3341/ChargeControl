package com.byq.chargecontrol;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.MainThread;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.byq.applib.ExceptionUtil;
import com.byq.chargecontrol.shell.Terminator;

import java.io.File;

import es.dmoral.toasty.Toasty;

/**
 * 充电控制调用
 * 必须在主线程调用
 */
@MainThread
public class ChargeController implements PowerStatusParser.RefreshListener {
    private static final String TAG = BuildConfig.APPLICATION_ID;
    private boolean autoCheck = true;
    private Handler handler = new Handler();
    private boolean originUsbDeviceConnect; //上一次更新时是否插入充电器
    private PowerStatusParser mPowerStatusParser;
    private Terminator mTerminator;
    private boolean waitingToReduceTemp;
    private boolean hasRootPermission;
    private AutoCheckConfig mConfig;
    private Context context;
    private ErrorMessageReceiver errorMessageReceiver;

    public ErrorMessageReceiver getErrorMessageReceiver() {
        return errorMessageReceiver;
    }

    public void setErrorMessageReceiver(ErrorMessageReceiver errorMessageReceiver) {
        this.errorMessageReceiver = errorMessageReceiver;
    }

    public ChargeController(Context context) {
        this.context = context;
    }

    public Terminator getmTerminator() {
        return mTerminator;
    }

    public void disableCharge() {
        Toasty.success(context,"慢速充电").show();
        mTerminator.runDisableCharge(context);
    }

    public void resumeCharge() {
        Toasty.success(context,"快速充电").show();
        mTerminator.runResumeCharge(context);
    }

    /**
     * 初始化方法
     */
    public void initialize() {
        Log.i(TAG, "initializing charge controller...");
        initializeConfig();
        achieveRootPermission();
    }

    /**
     * 读取Config文件并初始化
     */
    public void initializeConfig() {
        //Initialize config
        File file = new File(context.getFilesDir(),"autoCheckConfig.json");
        String content = FileIOUtils.readFile2String(file);
        mConfig = GsonUtils.fromJson(content,AutoCheckConfig.class);
        Log.i(TAG, "onCreate: initialized config.");
    }

    @Override
    public void onRefresh(PowerStatusParser powerStatusParser) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (autoCheck && hasRootPermission) {
                    if (powerStatusParser.isUsbConnected() && !originUsbDeviceConnect) {
                        Toasty.info(context,"检测到充电线插入").show();
                        resumeCharge();
                    }
                    if (powerStatusParser.isUsbConnected()) {
                        int temp = Math.round(powerStatusParser.getPowerTemp());
                        if (temp >= mConfig.stopTemp) { //超过温度限制
                            waitingToReduceTemp = true;
                            Toasty.warning(context,"温度过高，禁用快充\n当前温度："+temp).show();
                            disableCharge();
                        } else if (waitingToReduceTemp && temp <= mConfig.reduceToTemp) {
                            waitingToReduceTemp = false;
                            Toasty.info(context,"温度下降，恢复快充").show();
                            resumeCharge();
                        }
                    }
                    if (!powerStatusParser.isUsbConnected() && originUsbDeviceConnect){
                        Toasty.info(context,"检测到充电线拔出").show();
                        //Reset all variable
                        waitingToReduceTemp = false;
                    }

                    originUsbDeviceConnect = powerStatusParser.isUsbConnected();
                }
            }
        });
    }

    public interface ErrorMessageReceiver {
        public void onReceivedError(String message);
    }

    /**
     * 获取ROOT权限
     */
    private void achieveRootPermission() {
        //Initialize Root Process
        mTerminator = new Terminator();
        try {
            mTerminator.initializeRootProcess(new Terminator.SuStatusListener() {
                @Override
                public void onSuccess() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "run: success to request root permission");
                            Toasty.success(context, "Acquire Root Permission Succeed.").show();
                            onAchievedRootPermission();
                        }
                    });
                }

                @Override
                public void onFailed(String cause) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "run: failed to request root permission");
                            Toasty.error(context, "获取ROOT权限失败").show();
                        }
                    });
                }
            },context);

            Log.i(TAG, "achieveRootPermission: request root permission...");
        } catch (Exception e) {
            e.printStackTrace();
            printError("Request root permission error: "+ ExceptionUtil.getExceptionTraceContent(e));
        }
    }

    private void printError(String content) {
        if (errorMessageReceiver != null) {
            errorMessageReceiver.onReceivedError(content);
        }
    }

    /**
     * 终止控制器
     */
    public void terminate() {
        if (mPowerStatusParser != null) {
            mPowerStatusParser.stopAutoRefreshThread();
        }
        mTerminator.exit();
    }

    private void onAchievedRootPermission() {
        hasRootPermission = true;
        //Initialize the power status check thread
        mPowerStatusParser = new PowerStatusParser();
        mPowerStatusParser.setRefreshListener(this);
        mPowerStatusParser.refresh();
        mPowerStatusParser.startAutoRefreshThread(mConfig.dateRefreshDelay);
        Log.i(TAG, "onCreate: initialize power status succeed.");
    }
}
