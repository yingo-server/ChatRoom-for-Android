package com.motan.chat;

import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GiteeApi {
    private static final String ACCESS_TOKEN = "36079a70cc1fbac56f6846d99653405a";
    private static final String OWNER = "yingo-server";
    private static final String REPO = "yingo";
    private static final String FILE_PATH = "message.json";
    private static final String IMAGE_DIR = "chat_images";
    private static final String AUDIO_DIR = "rec";
    private static final String API_BASE = "https://gitee.com/api/v5";
    private static final String RAW_BASE = "https://gitee.com/" + OWNER + "/" + REPO + "/raw/master/";
    // 消息文件 raw 地址
    private static final String MESSAGES_RAW_URL = RAW_BASE + FILE_PATH;

    private final OkHttpClient client;
    private final Gson gson;

    public GiteeApi() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    // ✅ 消息下载：直接请求 raw 文件，无 CORS 限制，无需 Base64 解码
    public List<Message> getMessages() throws IOException {
        Request request = new Request.Builder().url(MESSAGES_RAW_URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());

            String json = response.body().string();
            Type listType = new TypeToken<List<Message>>(){}.getType();
            return gson.fromJson(json, listType);
        }
    }

    // 获取文件 SHA（仍需要 API，用于更新）
    public String getFileSha() throws IOException {
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + FILE_PATH + "?access_token=" + ACCESS_TOKEN;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());

            String json = response.body().string();
            JSONObject root = new JSONObject(json);
            return root.getString("sha");
        } catch (JSONException e) {
            throw new IOException("SHA 解析失败", e);
        }
    }

    // 上传/更新消息 JSON（使用 API，POST 创建，PUT 更新）
    public boolean uploadMessageJson(String jsonContent, String sha) throws IOException {
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + FILE_PATH + "?access_token=" + ACCESS_TOKEN;
        String encoded = Base64.encodeToString(jsonContent.getBytes("UTF-8"), Base64.DEFAULT);

        try {
            JSONObject body = new JSONObject();
            body.put("content", encoded);
            body.put("message", "更新消息 - " + System.currentTimeMillis());

            Request.Builder requestBuilder = new Request.Builder().url(url);
            if (sha == null) {
                requestBuilder.post(
                        RequestBody.create(body.toString(), MediaType.parse("application/json"))
                );
            } else {
                body.put("sha", sha);
                requestBuilder.put(
                        RequestBody.create(body.toString(), MediaType.parse("application/json"))
                );
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                return response.isSuccessful();
            }
        } catch (JSONException e) {
            throw new IOException("构建请求体失败", e);
        }
    }

    // 上传图片（API）
    public String uploadImage(byte[] imageData, String filename) throws IOException {
        String filePath = IMAGE_DIR + "/" + filename;
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + filePath + "?access_token=" + ACCESS_TOKEN;
        String encoded = Base64.encodeToString(imageData, Base64.DEFAULT);

        try {
            JSONObject body = new JSONObject();
            body.put("content", encoded);
            body.put("message", "上传图片 " + filename);
            body.put("branch", "master");

            RequestBody requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder().url(url).post(requestBody).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful())
                    throw new IOException("Upload image failed: " + response.code());
            }
        } catch (JSONException e) {
            throw new IOException("构建图片上传请求失败", e);
        }
        return RAW_BASE + filePath;
    }

    // 上传语音（API）
    public String uploadAudio(byte[] audioData, String filename) throws IOException {
        String filePath = AUDIO_DIR + "/" + filename;
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + filePath + "?access_token=" + ACCESS_TOKEN;
        String encoded = Base64.encodeToString(audioData, Base64.DEFAULT);

        try {
            JSONObject body = new JSONObject();
            body.put("content", encoded);
            body.put("message", "上传语音 " + filename);
            body.put("branch", "master");

            RequestBody requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder().url(url).post(requestBody).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful())
                    throw new IOException("Upload audio failed: " + response.code());
            }
        } catch (JSONException e) {
            throw new IOException("构建语音上传请求失败", e);
        }
        return RAW_BASE + filePath;
    }
}
