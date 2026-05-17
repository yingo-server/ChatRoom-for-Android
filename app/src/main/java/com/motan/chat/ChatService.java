package com.motan.chat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.List;

public class ChatService extends Service {
    private static final String CHANNEL_ID = "chat_keep_alive";
    private static final int NOTIFY_ID = 1;
    private static final long POLL_INTERVAL = 3000;

    public static final String ACTION_NEW_MESSAGE = "com.motan.chat.NEW_MESSAGE";
    public static final String EXTRA_COUNT = "extra_count";

    private GiteeApi api;
    private Handler handler;
    private int lastMessageCount = 0;
    private boolean isPolling = false;

    @Override
    public void onCreate() {
        super.onCreate();
        api = new GiteeApi();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        startForeground(NOTIFY_ID, buildNotification("连接中..."));
        startPolling();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "保活通知", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持聊天服务运行");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("墨谭")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void startPolling() {
        isPolling = true;
        handler.post(pollingRunnable);
    }

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPolling) return;
            new Thread(() -> {
                try {
                    List<Message> messages = api.getMessages();
                    if (messages != null) {
                        int newCount = messages.size() - lastMessageCount;
                        if (newCount > 0 && lastMessageCount > 0) {
                            // 发送本地广播
                            Intent broadcast = new Intent(ACTION_NEW_MESSAGE);
                            broadcast.putExtra(EXTRA_COUNT, newCount);
                            LocalBroadcastManager.getInstance(ChatService.this).sendBroadcast(broadcast);
                            // 发送通知
                            sendNewMessageNotification(newCount);
                        }
                        lastMessageCount = messages.size();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            handler.postDelayed(this, POLL_INTERVAL);
        }
    };

    private void sendNewMessageNotification(int count) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("新消息")
                .setContentText("收到 " + count + " 条新消息")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        nm.notify(100, notif);
    }

    @Override
    public void onDestroy() {
        isPolling = false;
        handler.removeCallbacks(pollingRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
