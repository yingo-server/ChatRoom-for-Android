package com.motan.chat;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.IOException;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private final List<Message> messages;
    private final String currentUserId;
    private final Context context;
    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;

    public ChatAdapter(Context context, List<Message> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);
        boolean isOwn = msg.userId != null && msg.userId.equals(currentUserId);

        holder.ownIndicator.setVisibility(isOwn ? View.VISIBLE : View.GONE);
        holder.username.setText(msg.username);
        holder.time.setText(msg.time != null ? msg.time : Utils.formatTime(msg.timestamp));

        // 文本
        if (msg.text != null && !msg.text.isEmpty()) {
            holder.messageText.setVisibility(View.VISIBLE);
            holder.messageText.setText(msg.text);
        } else {
            holder.messageText.setVisibility(View.GONE);
        }

        // 图片
        if (msg.images != null && !msg.images.isEmpty()) {
            holder.imagesContainer.setVisibility(View.VISIBLE);
            holder.imagesContainer.removeAllViews();
            for (String url : msg.images) {
                ImageView imageView = new ImageView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 300);
                params.setMargins(0, 4, 0, 4);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(context).load(url).placeholder(R.drawable.ic_placeholder).into(imageView);
                holder.imagesContainer.addView(imageView);
            }
        } else {
            holder.imagesContainer.setVisibility(View.GONE);
        }

        // 音频
        if (msg.audio != null) {
            holder.audioContainer.setVisibility(View.VISIBLE);
            holder.audioProgress.setProgress(0);
            holder.audioTime.setText("00:00");
            holder.playBtn.setImageResource(R.drawable.ic_play);
            holder.playBtn.setOnClickListener(v -> {
                if (playingPosition == position && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    holder.playBtn.setImageResource(R.drawable.ic_play);
                } else {
                    startAudio(msg.audio, position, holder);
                }
            });
        } else {
            holder.audioContainer.setVisibility(View.GONE);
        }
    }

    private void startAudio(String url, int position, ViewHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            holder.playBtn.setImageResource(R.drawable.ic_pause);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                playingPosition = position;
                updateProgress(holder);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                holder.playBtn.setImageResource(R.drawable.ic_play);
                holder.audioProgress.setProgress(0);
                holder.audioTime.setText("00:00");
                playingPosition = -1;
                mp.release();
                mediaPlayer = null;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateProgress(ViewHolder holder) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int progress = (int) (mediaPlayer.getCurrentPosition() * 100.0 / mediaPlayer.getDuration());
                    holder.audioProgress.setProgress(progress);
                    int sec = mediaPlayer.getCurrentPosition() / 1000;
                    holder.audioTime.setText(String.format("%02d:%02d", sec / 60, sec % 60));
                    new Handler(Looper.getMainLooper()).postDelayed(this, 200);
                }
            }
        }, 200);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            playingPosition = -1;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View ownIndicator;
        TextView username, time, messageText, audioTime;
        LinearLayout imagesContainer, audioContainer;
        ImageButton playBtn;
        ProgressBar audioProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ownIndicator = itemView.findViewById(R.id.ownIndicator);
            username = itemView.findViewById(R.id.username);
            time = itemView.findViewById(R.id.time);
            messageText = itemView.findViewById(R.id.messageText);
            imagesContainer = itemView.findViewById(R.id.imagesContainer);
            audioContainer = itemView.findViewById(R.id.audioContainer);
            playBtn = itemView.findViewById(R.id.playBtn);
            audioProgress = itemView.findViewById(R.id.audioProgress);
            audioTime = itemView.findViewById(R.id.audioTime);
        }
    }
}
