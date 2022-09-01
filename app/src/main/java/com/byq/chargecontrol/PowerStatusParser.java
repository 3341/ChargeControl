package com.byq.chargecontrol;

import android.util.Log;

import com.blankj.utilcode.util.FileIOUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PowerStatusParser {
    private static final String POWER_LESS_PATH = "/sys/class/power_supply/battery/capacity"; //剩余电量
    private static final String CURRENT_NOW = "/sys/class/power_supply/battery/current_now"; //当前电量
    private static final String POWER_TEMP = "/sys/class/power_supply/battery/temp"; //电池温度
    private static final String USB_DEVICE_CHARGE_STATUS = "/sys/class/power_supply/battery/subsystem/usb/uevent";
    private static final String TAG = BuildConfig.APPLICATION_ID;

    /**
     * 剩余电量
     */
    private float lessPower;
    /**
     * 当前电流 单位毫安
     */
    private float currentNow; //记得除以1000
    /**
     * 当前温度
     */
    private float powerTemp; //除以10
    /**
     * 是否连接充电线
     */
    private boolean isUsbConnected;
    
    private Thread refreshThread;
    private long refreshDelay;
    private String errorMessage;
    private RefreshListener refreshListener;
    
    public void startAutoRefreshThread(long refreshDelay) {
        this.refreshDelay = refreshDelay;
        refreshThread = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while(true) {
                        sleep(refreshDelay);
                        refresh();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        refreshThread.start();
    }

    private void printError(String errorMsg) {
        errorMessage = errorMsg;
        Log.e(TAG, "printError: "+errorMsg );
    }

    public boolean isUsbConnected() {
        return isUsbConnected;
    }

    public long getRefreshDelay() {
        return refreshDelay;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String readError() {
        String result = errorMessage;
        errorMessage = null;
        return result;
    }

    public void setRefreshListener(RefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    public interface RefreshListener {
        public void onRefresh(PowerStatusParser powerStatusParser);
    }
    
    public void stopAutoRefreshThread() {
        if (refreshThread != null && refreshThread.isAlive()) {
            refreshThread.interrupt();
        }
    }

    @Override
    public String toString() {
        return "PowerStatusParser{" +
                "lessPower=" + lessPower +
                "\n currentNow=" + currentNow +
                "\n powerTemp=" + powerTemp +
                "\n isUsbConnected=" + isUsbConnected +
                "\n refreshThread=" + refreshThread +
                "\n refreshDelay=" + refreshDelay +
                "\n errorMessage='" + errorMessage + '\'' +
                "\n refreshListener=" + refreshListener +
                '}';
    }

    public float getLessPower() {
        return lessPower;
    }

    public float getCurrentNow() {
        return currentNow;
    }

    public float getPowerTemp() {
        return powerTemp;
    }

    /**
     * 刷新电池数据
     */
    public void refresh() {
        String s = readFile(POWER_LESS_PATH);
        lessPower = Float.parseFloat(s);
        s = readFile(CURRENT_NOW);
        currentNow = Float.parseFloat(s) / 1000; 
        s = readFile(POWER_TEMP);
        powerTemp = Float.parseFloat(s) /10;
        if (refreshListener != null) {
            refreshListener.onRefresh(PowerStatusParser.this);
        }

        //Read usb file
        s = readFile(USB_DEVICE_CHARGE_STATUS);
        Pattern compile = Pattern.compile("(?<=POWER_SUPPLY_VOLTAGE_NOW=)\\d+");
        Matcher matcher = compile.matcher(s);
        if (matcher.find()) {
            String group = matcher.group();
            try {
                int i = Integer.parseInt(group);
                isUsbConnected = i != 0;
            } catch (NumberFormatException e) {
                printError("number format error:"+group);
            }
        } else {
            printError("Not found key word for file content:\n"+s);
        }
    }
    
    private String readFile(String path) {
        File file = new File(path);
        return FileIOUtils.readFile2String(file);
    }
    
}
