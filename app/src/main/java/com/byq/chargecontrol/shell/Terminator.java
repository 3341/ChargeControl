package com.byq.chargecontrol.shell;

import android.content.Context;
import android.util.Log;

import com.byq.chargecontrol.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * 终端管理
 * 运行命令
 */
public class Terminator {
    private static final String DISABLE_CHARGE_PATH = "disable_charge.sh";
    private static final String RESUME_CHARGE_PATH = "resume_charge.sh";
    private static final String TAG = BuildConfig.APPLICATION_ID;
    private Scanner commandInput;
    private PrintStream commandOutput;
    private Scanner errorInput;
    private Process process;
    private MessageReceiver messageReceiver;
    private ReceiverThread receiverThread;
    private ReceiverThread errorReceiver;
    private Thread startTimer;
    private boolean isEnd;
    private ErrorReceiver mErrorReceiver;

    public interface ErrorReceiver {
        public void onReceived(String msg);
    }

    public ErrorReceiver getmErrorReceiver() {
        return mErrorReceiver;
    }

    public void setmErrorReceiver(ErrorReceiver mErrorReceiver) {
        this.mErrorReceiver = mErrorReceiver;
    }

    /**
     * 用来接收数据
     */
    private class ReceiverThread extends Thread {
        private Scanner targetInput;

        public ReceiverThread(Scanner targetInput) {
            this.targetInput = targetInput;
        }

        public ReceiverThread() {
            targetInput = commandInput;
        }

        @Override
        public void run() {
            while(targetInput.hasNextLine()) {
                if (messageReceiver != null) {
                    messageReceiver.receivedMessage(targetInput.nextLine());
                }
            }
        }
    }


    private String getShellFilePath(String name, Context context) {
        File dir = context.getFilesDir();
        File f = new File(dir,name);
        return f.getPath();
    }

    /**
     * 运行禁止充电
     */
    public void runDisableCharge(Context context) {
        this.command(getShellFilePath(DISABLE_CHARGE_PATH,context));
    }

    /**
     * 运行恢复充电脚本
     */
    public void runResumeCharge(Context context) {
        command(getShellFilePath(RESUME_CHARGE_PATH,context));
    }

    public void command(String command) {
        commandOutput.println(command);
        commandOutput.flush();
    }

    public void setMessageReceiver(MessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    /**
     * 终止所有进程和线程
     */
    public void endAllProcess() {
        isEnd = true;
        if (process != null) {
            process.destroy();
        }
        if (receiverThread != null && receiverThread.isAlive()) receiverThread.interrupt();
    }

    public void exit() {
        endAllProcess();
    }

    /**
     * 判断线程是否应该终止
     * @return
     */
    private boolean isThreadShouldEnd() {
        return isEnd;
    }

    /**
     * 用来接收输入流数据
     */
    public interface MessageReceiver {
        public void receivedMessage(String msg);
    }

    /**
     * 一定要调用这个方法
     * 确保文件具有可执行权限
     */
    public void initializeFilePermission(Context context) {
        command("chmod 500 "+new File(context.getFilesDir(),"disable_charge.sh"));
        command("chmod 500 "+new File(context.getFilesDir(),"resume_charge.sh"));
    }

    public interface SuStatusListener {
        public void onSuccess();
        public void onFailed(String cause);
    }

    public void initializeRootProcess(SuStatusListener listener,Context context) throws IOException {
        Log.i(TAG, "initializeRootProcess...");
        Process exec = Runtime.getRuntime().exec("su");
        process = exec;
        commandInput = new Scanner(exec.getInputStream());
        commandOutput = new PrintStream(exec.getOutputStream());
        errorInput = new Scanner(exec.getErrorStream());

        commandOutput.println("echo done");
        commandOutput.flush();

        messageReceiver = new MessageReceiver() {
            @Override
            public void receivedMessage(String msg) {
                Log.e(TAG, "receivedMessage: "+msg);
                if (msg.equals("done")) {
                    Log.i(TAG, "receivedMessage: Success to get root process.");
                    initializeFilePermission(context);
                    listener.onSuccess();
                } else {
                    if (mErrorReceiver != null) {
                        mErrorReceiver.onReceived(msg);
                    }
                }
                if (startTimer != null) {
                    startTimer.interrupt();
                }
            }
        };
        //WaitThread
        receiverThread = new ReceiverThread();
        receiverThread.start();
        errorReceiver = new ReceiverThread(errorInput);
        errorReceiver.start();

        //Timer
         startTimer = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(7000); //7s wait time
                    receiverThread.interrupt();
                    Log.e(TAG, "run: root failed" );
                    listener.onFailed("Timeout.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        startTimer.start();
    }
}
