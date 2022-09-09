package com.byq.chargecontrol;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import com.byq.applib.broadcast.CommunicatBroadcastForReplay;
import com.byq.applib.broadcast.CommunicateBroadcast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import es.dmoral.toasty.Toasty;

/**
 * 用于保活的Service
 */
public class MainService extends NotificationListenerService {
    private static final String TAG = BuildConfig.APPLICATION_ID;
    private static final String EVENT_CHECK_REPEAT = "checkRepeat";
    private ChargeController mChargeController;
    private boolean isServiceStarted;

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    /**
     * 完成Service的初始化
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Handler handler = new Handler();
        CommunicateBroadcast.sendBroadcast(this, EVENT_CHECK_REPEAT, new Intent(), new CommunicatBroadcastForReplay(EVENT_CHECK_REPEAT) {
            @Override
            public void onReplayReceived(Context context, Intent intent) {
                Toasty.error(MainService.this,"重复Service，自动注销").show();
            }

            @Override
            public long getReplayMaxDelay() {
                return 1000;
            }

            @Override
            public boolean onReceiveTimeout() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        startService();
                    }
                });

                return false;
            }
        });
    }

    private void startService() {
        isServiceStarted = true;
        Toasty.success(MainService.this,"Service Started.").show();
        Log.i(TAG, "onCreate: MainService created.");
        EventBus.getDefault().register(MainService.this);

        //Initialize controller
        mChargeController = new ChargeController(MainService.this);
        mChargeController.initialize();

        //向Activity发送广播
        EventBus.getDefault().post(new MessageEvent(MessageEvent.TRY_RESPONSE_ACTIVTY));

        CommunicateBroadcast communicateBroadcast = new CommunicateBroadcast() {

            @Override
            public void onReceive(Context context, Intent intent) {
                sendReplay(MainService.this,EVENT_CHECK_REPEAT,new Intent());
            }
        };
        communicateBroadcast.addSupplyEvent(EVENT_CHECK_REPEAT);
        communicateBroadcast.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceStarted) {
            if (mChargeController != null) {
                mChargeController.terminate();
            }
            Log.e(TAG, "onDestroy: Main service will be destroyed");
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe
    public void eventbusReceiver(MessageEvent event) {
        switch (event.getEventId()) {
            case MessageEvent.TRY_CALL_SERVICE: {
                MessageEvent messageEvent = new MessageEvent(MessageEvent.TRY_RESPONSE_ACTIVTY);
                messageEvent.setData(this);
                //响应Activity的消息
                EventBus.getDefault().post(messageEvent);
                break;
            }

            case MessageEvent.UPDATE_CONFIG_FILE: {
                mChargeController.initializeConfig();
                Toasty.success(MainService.this, "已接收到Activity发来的更新消息").show();
                break;
            }

            case MessageEvent.REQUEST_DISABLE_CHARGE:
                mChargeController.disableCharge();
                break;

            case MessageEvent.REQUEST_ENABLE_CHARGE:
                mChargeController.resumeCharge();
                break;
        }
    }
}