package com.hello.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hello.Examples;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileUtil {

    public static JsonObject json(String filename) {
        Gson gson = new Gson();
        InputStream jsonStream = FileUtil.class.getClassLoader().getResourceAsStream(filename);
        return gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
    }

    public static String contents(String filename) throws IOException {
        InputStream stream = FileUtil.class.getClassLoader().getResourceAsStream(filename);
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }
}
