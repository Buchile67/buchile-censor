package com.buchile.censor.mobile;

import android.graphics.Rect;

final class Detection {
    String id;
    final Part part;
    final float confidence;
    final Rect box;
    final byte[] mask;
    final int maskWidth;
    final int maskHeight;
    final int maskCount;

    Detection(String id, Part part, float confidence, Rect box, byte[] mask, int maskWidth, int maskHeight, int maskCount) {
        this.id = id;
        this.part = part;
        this.confidence = confidence;
        this.box = box;
        this.mask = mask;
        this.maskWidth = maskWidth;
        this.maskHeight = maskHeight;
        this.maskCount = maskCount;
    }

    boolean containsMaskPixel(int imageX, int imageY) {
        int x = imageX - box.left;
        int y = imageY - box.top;
        return x >= 0 && y >= 0 && x < maskWidth && y < maskHeight && mask[y * maskWidth + x] != 0;
    }
}

