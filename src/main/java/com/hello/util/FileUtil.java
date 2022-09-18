package com.hello.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hello.Examples;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileUtil {

    public static JsonObject json(String filename) {
        Gson gson = new Gson();
        InputStream jsonStream = Examples.class.getClassLoader().getResourceAsStream(filename);
        return gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
    }
}
