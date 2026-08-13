package com.buchile.censor.mobile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.List;
import java.util.Set;

final class CensorEngine {
    enum Mode {
        PIXEL,
        MAODIE,
        DAGOU,
        CUSTOM
    }

    static Bitmap render(
            Bitmap original,
            List<Detection> detections,
            Set<Part> selectedParts,
            Mode mode,
            int blockSize,
            Bitmap sticker,
            boolean markers
    ) {
        Bitmap output = original.copy(Bitmap.Config.ARGB_8888, true);
        for (Detection detection : detections) {
            if (!selectedParts.contains(detection.part)) {
                continue;
            }
            applyOne(output, detection, mode, blockSize, sticker);
        }
        if (markers) {
            drawMarkers(output, detections, selectedParts);
        }
        return output;
    }

    private static void applyOne(Bitmap output, Detection detection, Mode mode, int blockSize, Bitmap sticker) {
        Rect box = detection.box;
        int width = box.width();
        int height = box.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        Bitmap crop = Bitmap.createBitmap(output, box.left, box.top, width, height);
        Bitmap effect;
        if (mode == Mode.PIXEL || sticker == null) {
            int smallWidth = Math.max(1, (int) Math.ceil(width / (double) Math.max(2, blockSize)));
            int smallHeight = Math.max(1, (int) Math.ceil(height / (double) Math.max(2, blockSize)));
            Bitmap small = Bitmap.createScaledBitmap(crop, smallWidth, smallHeight, false);
            effect = Bitmap.createScaledBitmap(small, width, height, false);
            if (small != crop) {
                small.recycle();
            }
        } else {
            effect = opaqueSticker(sticker, width, height);
        }
        int[] originalPixels = new int[width * height];
        int[] effectPixels = new int[width * height];
        crop.getPixels(originalPixels, 0, width, 0, 0, width, height);
        effect.getPixels(effectPixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < originalPixels.length; i++) {
            if (detection.mask[i] != 0) {
                originalPixels[i] = effectPixels[i];
            }
        }
        output.setPixels(originalPixels, 0, width, box.left, box.top, width, height);
        crop.recycle();
        if (effect != crop) {
            effect.recycle();
        }
    }

    private static Bitmap opaqueSticker(Bitmap source, int width, int height) {
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(averageVisibleColor(source));
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, null, new Rect(0, 0, width, height), paint);
        return output;
    }

    private static int averageVisibleColor(Bitmap bitmap) {
        int sampleWidth = Math.min(96, bitmap.getWidth());
        int sampleHeight = Math.min(96, bitmap.getHeight());
        Bitmap sample = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true);
        int[] pixels = new int[sampleWidth * sampleHeight];
        sample.getPixels(pixels, 0, sampleWidth, 0, 0, sampleWidth, sampleHeight);
        long red = 0, green = 0, blue = 0, count = 0;
        for (int color : pixels) {
            if (Color.alpha(color) > 12) {
                red += Color.red(color);
                green += Color.green(color);
                blue += Color.blue(color);
                count++;
            }
        }
        if (sample != bitmap) {
            sample.recycle();
        }
        if (count == 0) {
            return Color.rgb(32, 32, 32);
        }
        return Color.rgb((int) (red / count), (int) (green / count), (int) (blue / count));
    }

    private static void drawMarkers(Bitmap bitmap, List<Detection> detections, Set<Part> selectedParts) {
        Canvas canvas = new Canvas(bitmap);
        float radius = Math.max(13f, Math.min(28f, Math.min(bitmap.getWidth(), bitmap.getHeight()) * 0.018f));
        Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        text.setTextSize(radius * 1.15f);
        int number = 1;
        for (Detection detection : detections) {
            if (!selectedParts.contains(detection.part)) {
                continue;
            }
            long sumX = 0, sumY = 0, count = 0;
            for (int y = 0; y < detection.maskHeight; y++) {
                for (int x = 0; x < detection.maskWidth; x++) {
                    if (detection.mask[y * detection.maskWidth + x] != 0) {
                        sumX += detection.box.left + x;
                        sumY += detection.box.top + y;
                        count++;
                    }
                }
            }
            float x = count == 0 ? detection.box.centerX() : sumX / (float) count;
            float y = count == 0 ? detection.box.centerY() : sumY / (float) count;
            x = Math.max(radius + 2, Math.min(bitmap.getWidth() - radius - 2, x));
            y = Math.max(radius + 2, Math.min(bitmap.getHeight() - radius - 2, y));
            circle.setColor(Color.WHITE);
            canvas.drawCircle(x, y, radius + 2, circle);
            circle.setColor(Color.rgb(220, 35, 45));
            canvas.drawCircle(x, y, radius, circle);
            Paint.FontMetrics metrics = text.getFontMetrics();
            float baseline = y - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(String.valueOf(number++), x, baseline, text);
        }
    }

    private CensorEngine() {
    }
}

