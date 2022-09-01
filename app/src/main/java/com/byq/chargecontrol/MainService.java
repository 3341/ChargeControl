package com.byq.chargecontrol;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import es.dmoral.toasty.Toasty;

/**
 * 用于保活的Service
 */
public class MainService extends NotificationListenerService {
    private static final String TAG = BuildConfig.APPLICATION_ID;
    private ChargeController mChargeController;

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
        Toasty.success(this,"Service Started.").show();
        Log.i(TAG, "onCreate: MainService created.");
        EventBus.getDefault().register(this);

        //Initialize controller
        mChargeController = new ChargeController(MainService.this);
        mChargeController.initialize();

        //向Activity发送广播
        EventBus.getDefault().post(new MessageEvent(MessageEvent.TRY_RESPONSE_ACTIVTY));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChargeController != null) {
            mChargeController.terminate();
        }
        Log.e(TAG, "onDestroy: Main service will be destroyed");
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void eventbusReceiver(MessageEvent event) {
        switch (event.getEventId()) {
            case MessageEvent.TRY_CALL_SERVICE:
                MessageEvent messageEvent = new MessageEvent(MessageEvent.TRY_RESPONSE_ACTIVTY);
                messageEvent.setData(this);
                //响应Activity的消息
                EventBus.getDefault().post(messageEvent);
                break;

                case MessageEvent.UPDATE_CONFIG_FILE:
                    mChargeController.initializeConfig();
                    break;
        }
    }
}