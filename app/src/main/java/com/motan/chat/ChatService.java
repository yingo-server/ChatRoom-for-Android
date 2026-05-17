package com.motan.chat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatService extends Service {
    private static final String CHANNEL_KEEP_ALIVE = "chat_keep_alive";
    private static final String CHANNEL_NEW_MSG = "chat_new_message";
    private static final int NOTIFY_KEEP = 1;

    private GiteeApi api;
    private ScheduledExecutorService scheduler;
    private int lastMessageCount = 0;
    private boolean firstLoad = true;

    @Override
    public void onCreate() {
        super.onCreate();
        api = new GiteeApi();
        createNotificationChannels();
        startForeground(NOTIFY_KEEP, buildStatusNotification("在线", "等待新消息..."));
        startPolling();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel keep = new NotificationChannel(
                    CHANNEL_KEEP_ALIVE, "服务保活", NotificationManager.IMPORTANCE_LOW);
            keep.setDescription("保持后台连接");
            nm.createNotificationChannel(keep);

            NotificationChannel newMsg = new NotificationChannel(
                    CHANNEL_NEW_MSG, "新消息", NotificationManager.IMPORTANCE_HIGH);
            newMsg.setDescription("收到新消息提醒");
            newMsg.enableVibration(true);
            nm.createNotificationChannel(newMsg);
        }
    }

    private Notification buildStatusNotification(String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_KEEP_ALIVE)
                .setContentTitle("墨谭 · " + title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                List<Message> messages = api.getMessages();
                if (messages == null) return;

                int currentCount = messages.size();

                // 更新状态通知
                updateStatusNotification(currentCount);

                // 检测新消息（忽略首次加载）
                if (!firstLoad && lastMessageCount > 0 && currentCount > lastMessageCount) {
                    int newCount = currentCount - lastMessageCount;
                    showNewMessageNotification(messages);
                }
                lastMessageCount = currentCount;
                firstLoad = false;
            } catch (IOException e) {
                updateStatusNotification(-1);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void updateStatusNotification(int messageCount) {
        String title, text;
        if (messageCount < 0) {
            title = "离线";
            text = "连接失败，正在重试...";
        } else {
            title = "在线";
            text = messageCount + " 条消息 · 刚刚更新";
        }
        Notification notification = buildStatusNotification(title, text);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFY_KEEP, notification);
    }

    private void showNewMessageNotification(List<Message> messages) {
        // 取最后一条消息作为预览
        String preview = "收到新消息";
        if (messages != null && !messages.isEmpty()) {
            Message last = messages.get(messages.size() - 1);
            String from = last.username;
            String content = last.text;
            if (content == null && last.images != null && !last.images.isEmpty()) {
                content = "[图片]";
            } else if (content == null && last.audio != null) {
                content = "[语音]";
            } else if (content == null) {
                content = "";
            }
            preview = from + ": " + content;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_NEW_MSG)
                .setContentTitle("墨谭 · 新消息")
                .setContentText(preview)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .build();

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(100 + (int)(System.currentTimeMillis() % 1000), notification);
        }
    }

    @Override
    public void onDestroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
