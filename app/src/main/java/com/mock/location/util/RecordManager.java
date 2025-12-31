// com.mock.location.util.RecordManager
package com.mock.location.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mock.location.model.LocationRecord;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecordManager {

    private static final String FILE_NAME = "mock_records.json";
    private static final String PREF_NAME = "mock_prefs";
    private static final String KEY_CURRENT_INDEX = "current_record_index";

    // 保存所有记录到文件
    public static void saveAllRecords(Context context, List<LocationRecord> records) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(records);
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 从文件读取所有记录
    @NonNull
    public static List<LocationRecord> getAllRecords(Context context) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (!file.exists()) return new ArrayList<>();

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            if (sb.length() == 0) return new ArrayList<>();

            Gson gson = new Gson();
            Type listType = new TypeToken<List<LocationRecord>>(){}.getType();
            List<LocationRecord> records = gson.fromJson(sb.toString(), listType);
            return records != null ? records : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 添加一条记录（自动命名：记录1、记录2...）
    public static void addRecord(Context context, LocationRecord record) {
        List<LocationRecord> records = getAllRecords(context);
        int nextId = records.size() + 1;
        record.name = "记录" + nextId;
        record.timestamp = System.currentTimeMillis();
        records.add(record);
        saveAllRecords(context, records);
    }

    // 获取当前选中的记录（根据索引）
    public static LocationRecord getCurrentRecord(Context context) {
        List<LocationRecord> all = getAllRecords(context);
        int index = getCurrentRecordIndex(context);
        if (index >= 0 && index < all.size()) {
            return all.get(index);
        }
        return null;
    }

    // 设置当前选中索引
    public static void setCurrentRecordIndex(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_CURRENT_INDEX, index).apply();
    }

    // 获取当前选中索引
    public static int getCurrentRecordIndex(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CURRENT_INDEX, -1);
    }
}