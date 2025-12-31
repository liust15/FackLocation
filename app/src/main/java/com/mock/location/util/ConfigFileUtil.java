package com.mock.location.util;


import android.content.Context;
import android.util.Log;

import com.mock.location.Constant;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;


public class ConfigFileUtil {

    private static final String TARGET_PATH = "/data/local/tmp/mock_location.loc";

    /**
     * 将任意 Serializable 对象通过 Root 权限写入到 /data/local/tmp/mock_location.loc
     *
     * @param content 要写入的对象（必须实现 java.io.Serializable）
     * @return true 表示成功写入并设置权限，false 表示失败
     */
    public static boolean writeString(Context context, byte[] content) {
        File localFile = new File(context.getCacheDir(), "mock.tmp");
        try {
            // 1. 安全写入 App 自己目录
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                fos.write(content);
            }

            // 2. 用 su 复制并设权限
            Process p = Runtime.getRuntime().exec("su");
            try (DataOutputStream os = new DataOutputStream(p.getOutputStream())) {
                os.writeBytes("cp '" + localFile.getAbsolutePath() + "' /data/local/tmp/mock_location.loc\n");
                os.writeBytes("chmod 644 /data/local/tmp/mock_location.loc\n");
                os.writeBytes("exit\n");
                os.flush();
                return p.waitFor() == 0;
            }
        } catch (Exception e) {
            Log.e(Constant.TAG, "Failed", e);
            return false;
        } finally {
            if (localFile.exists()) {
                 localFile.delete();
            }
        }
    }
    // ConfigFileUtil.java
    public static boolean deleteMockFile(Context context) {
        try {
            String path = "/data/local/tmp/mock_location.loc";
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "rm " + path});
            int result = p.waitFor();
            return result == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean writeObject(Context context, Object obj) {
        if (obj == null) {
            Log.e(Constant.TAG, "Object is null");
            return false;
        }
        return writeString(context, JsonUtils.toByteArray(obj));
    }

    public static String readString() {
        File file = new File(TARGET_PATH);
        if (!file.exists()) {
            Log.w(Constant.TAG, "File not found: " + TARGET_PATH);
            return null;
        }
        if (!file.canRead()) {
            Log.e(Constant.TAG, "File not readable. Check permissions.");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            // 移除最后一个换行符（如果有的话）
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }

            return sb.toString();

        } catch (IOException e) {
            Log.e(Constant.TAG, "Failed to read file", e);
            return null;
        }
    }

}