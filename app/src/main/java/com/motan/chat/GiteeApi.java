package com.motan.chat;

import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
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

    private final OkHttpClient client;
    private final Gson gson;

    public GiteeApi() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public List<Message> getMessages() throws IOException {
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + FILE_PATH + "?access_token=" + ACCESS_TOKEN;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            String json = response.body().string();
            JSONObject root = new JSONObject(json);
            String content = root.getString("content");
            byte[] decoded = Base64.decode(content, Base64.DEFAULT);
            String messagesJson = new String(decoded, "UTF-8");
            Type listType = new TypeToken<List<Message>>(){}.getType();
            return gson.fromJson(messagesJson, listType);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String getFileSha() throws IOException {
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + FILE_PATH + "?access_token=" + ACCESS_TOKEN;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            String json = response.body().string();
            JSONObject root = new JSONObject(json);
            return root.getString("sha");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public boolean uploadMessageJson(String jsonContent, String sha) throws IOException {
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + FILE_PATH + "?access_token=" + ACCESS_TOKEN;
        String encoded = Base64.encodeToString(jsonContent.getBytes("UTF-8"), Base64.DEFAULT);
        JSONObject body = new JSONObject();
        body.put("content", encoded);
        body.put("message", "更新消息 - " + System.currentTimeMillis());
        if (sha != null) body.put("sha", sha);
        RequestBody requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(url).put(requestBody).build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String uploadImage(byte[] imageData, String filename) throws IOException {
        String filePath = IMAGE_DIR + "/" + filename;
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + filePath + "?access_token=" + ACCESS_TOKEN;
        String encoded = Base64.encodeToString(imageData, Base64.DEFAULT);
        JSONObject body = new JSONObject();
        body.put("content", encoded);
        body.put("message", "上传图片 " + filename);
        body.put("branch", "master");
        RequestBody requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(url).post(requestBody).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Upload image failed: " + response.code());
        } catch (Exception e) {
            throw new IOException(e);
        }
        return RAW_BASE + filePath;
    }

    public String uploadAudio(byte[] audioData, String filename) throws IOException {
        String filePath = AUDIO_DIR + "/" + filename;
        String url = API_BASE + "/repos/" + OWNER + "/" + REPO + "/contents/" + filePath + "?access_token=" + ACCESS_TOKEN;
        String encoded = Base64.encodeToString(audioData, Base64.DEFAULT);
        JSONObject body = new JSONObject();
        body.put("content", encoded);
        body.put("message", "上传语音 " + filename);
        body.put("branch", "master");
        RequestBody requestBody = RequestBody.create(body.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(url).post(requestBody).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Upload audio failed: " + response.code());
        } catch (Exception e) {
            throw new IOException(e);
        }
        return RAW_BASE + filePath;
    }
}
