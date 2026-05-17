package com.motan.chat;

import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecorder {
    private MediaRecorder recorder;
    private File outputFile;
    private boolean isRecording = false;
    private long startTime;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecordingCallback callback;

    public interface RecordingCallback {
        void onTimerUpdate(int seconds);
        void onMaxDurationReached();
    }

    public void start(File outputDir, RecordingCallback callback) throws IOException {
        this.callback = callback;
        outputFile = File.createTempFile("audio_", ".webm", outputDir);
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.VORBIS);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setOutputFile(outputFile.getAbsolutePath());
        recorder.prepare();
        recorder.start();
        isRecording = true;
        startTime = System.currentTimeMillis();
        startTimer();
    }

    private void startTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isRecording) return;
                int elapsed = (int) ((System.currentTimeMillis() - startTime) / 1000);
                callback.onTimerUpdate(elapsed);
                if (elapsed >= 15) {
                    callback.onMaxDurationReached();
                    stop();
                } else {
                    handler.postDelayed(this, 200);
                }
            }
        });
    }

    public File stop() {
        if (!isRecording) return null;
        try {
            recorder.stop();
        } catch (RuntimeException e) {
            // 录音可能已停止
        }
        recorder.release();
        recorder = null;
        isRecording = false;
        handler.removeCallbacksAndMessages(null);
        return outputFile.exists() ? outputFile : null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void cancel() {
        if (!isRecording) return;
        try {
            recorder.stop();
        } catch (Exception ignored) {}
        recorder.release();
        recorder = null;
        isRecording = false;
        handler.removeCallbacksAndMessages(null);
        if (outputFile != null && outputFile.exists()) outputFile.delete();
    }
}
