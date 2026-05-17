package com.motan.chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageCompressor {
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final int QUALITY = 70;

    public static byte[] compress(InputStream inputStream) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        byte[] inputBytes = readBytes(inputStream);
        BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.length, options);
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            int halfWidth = width / 2;
            int halfHeight = height / 2;
            while ((halfWidth / inSampleSize) >= MAX_WIDTH && (halfHeight / inSampleSize) >= MAX_HEIGHT) {
                inSampleSize *= 2;
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.length, options);
        if (bitmap == null) throw new IOException("Failed to decode bitmap");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, baos);
        bitmap.recycle();
        return baos.toByteArray();
    }

    private static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
