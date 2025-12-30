package com.mock.location.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;

public class JsonUtils {
    public static final Gson GSON = new Gson();


    /**
     * 对象 → JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) return null;
        return GSON.toJson(obj);
    }
    public static byte[] toByteArray(Object obj) {
        if (obj == null) {
            return null;
        }
        String json = GSON.toJson(obj);
        return json.getBytes(StandardCharsets.UTF_8);
    }


    /**
     * JSON 字符串 → 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) return null;
        return GSON.fromJson(json, clazz);
    }
}