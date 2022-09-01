package com.byq.chargecontrol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Test {
    private static String logFilePath = "/sdcard/test.txt";

    public static void logToFile(String log) {
        File file = new File(logFilePath);
        if (!file.isFile()) {
            try {
                file.createNewFile();
                FileWriter fw = new FileWriter(file, true);
                fw.write(log + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void test() {
        logToFile("");
    }
}
