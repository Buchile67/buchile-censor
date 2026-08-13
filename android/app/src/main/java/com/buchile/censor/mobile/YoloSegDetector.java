package com.buchile.censor.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

final class YoloSegDetector implements AutoCloseable {
    static final int INPUT_SIZE = 640;
    private static final int MAX_CANDIDATES = 100;
    private final OrtEnvironment environment;
    private final List<LoadedModel> models = new ArrayList<>();

    YoloSegDetector(Context context) throws IOException, OrtException {
        environment = OrtEnvironment.getEnvironment();
        models.add(load(context, ModelSpec.hachimi()));
        models.add(load(context, ModelSpec.maodie()));
    }

    private LoadedModel load(Context context, ModelSpec spec) throws IOException, OrtException {
        byte[] modelBytes = readAll(context.getAssets().open(spec.assetName));
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1)));
        options.setInterOpNumThreads(1);
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        OrtSession session = environment.createSession(modelBytes, options);
        Iterator<String> iterator = session.getInputNames().iterator();
        if (!iterator.hasNext()) {
            session.close();
            throw new OrtException("Model has no input");
        }
        return new LoadedModel(spec, session, iterator.next());
    }

    List<Detection> detect(Bitmap source, float threshold) throws OrtException {
        Bitmap bitmap = source.getConfig() == Bitmap.Config.ARGB_8888
                ? source : source.copy(Bitmap.Config.ARGB_8888, false);
        float scale = Math.min(INPUT_SIZE / (float) bitmap.getWidth(), INPUT_SIZE / (float) bitmap.getHeight());
        int scaledWidth = Math.max(1, Math.round(bitmap.getWidth() * scale));
        int scaledHeight = Math.max(1, Math.round(bitmap.getHeight() * scale));
        float padX = (INPUT_SIZE - scaledWidth) / 2f;
        float padY = (INPUT_SIZE - scaledHeight) / 2f;
        Bitmap inputBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(inputBitmap);
        canvas.drawColor(Color.rgb(114, 114, 114));
        Paint resizePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, null, new RectF(padX, padY, padX + scaledWidth, padY + scaledHeight), resizePaint);
        float[] input = bitmapToChw(inputBitmap);
        inputBitmap.recycle();

        List<Detection> combined = new ArrayList<>();
        for (LoadedModel model : models) {
            combined.addAll(runModel(model, input, bitmap.getWidth(), bitmap.getHeight(), scale, padX, padY, threshold));
        }
        combined.sort((a, b) -> Float.compare(b.confidence, a.confidence));
        List<Detection> kept = new ArrayList<>();
        for (Detection candidate : combined) {
            boolean duplicate = false;
            for (Detection existing : kept) {
                if (candidate.part == existing.part && maskIou(candidate, existing) >= 0.62f) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                kept.add(candidate);
            }
        }
        kept.sort(Comparator.comparingInt((Detection d) -> d.part.ordinal())
                .thenComparingInt(d -> d.box.top).thenComparingInt(d -> d.box.left));
        for (int i = 0; i < kept.size(); i++) {
            kept.get(i).id = kept.get(i).part.name() + ":" + i;
        }
        return kept;
    }

    private List<Detection> runModel(
            LoadedModel model,
            float[] input,
            int originalWidth,
            int originalHeight,
            float scale,
            float padX,
            float padY,
            float threshold
    ) throws OrtException {
        long[] shape = new long[]{1, 3, INPUT_SIZE, INPUT_SIZE};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape)) {
            Map<String, OnnxTensor> inputs = Collections.singletonMap(model.inputName, tensor);
            try (OrtSession.Result result = model.session.run(inputs)) {
                float[][][] prediction = (float[][][]) result.get(0).getValue();
                float[][][][] prototypes = (float[][][][]) result.get(1).getValue();
                return decode(model.spec, prediction[0], prototypes[0], originalWidth, originalHeight, scale, padX, padY, threshold);
            }
        }
    }

    private List<Detection> decode(
            ModelSpec spec,
            float[][] prediction,
            float[][][] prototypes,
            int originalWidth,
            int originalHeight,
            float scale,
            float padX,
            float padY,
            float threshold
    ) {
        int anchors = prediction[0].length;
        int classCount = spec.classes.length;
        int coefficientStart = 4 + classCount;
        int coefficientCount = prototypes.length;
        List<Candidate> candidates = new ArrayList<>();
        for (int anchor = 0; anchor < anchors; anchor++) {
            int bestClass = -1;
            float bestScore = threshold;
            for (int cls = 0; cls < classCount; cls++) {
                float score = prediction[4 + cls][anchor];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = cls;
                }
            }
            if (bestClass < 0) {
                continue;
            }
            float cx = prediction[0][anchor];
            float cy = prediction[1][anchor];
            float width = prediction[2][anchor];
            float height = prediction[3][anchor];
            RectF inputBox = new RectF(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f);
            float[] coefficients = new float[coefficientCount];
            for (int i = 0; i < coefficientCount; i++) {
                coefficients[i] = prediction[coefficientStart + i][anchor];
            }
            candidates.add(new Candidate(spec.classes[bestClass], bestScore, inputBox, coefficients));
        }
        candidates.sort((a, b) -> Float.compare(b.score, a.score));
        if (candidates.size() > MAX_CANDIDATES) {
            candidates = new ArrayList<>(candidates.subList(0, MAX_CANDIDATES));
        }
        List<Candidate> nms = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean overlaps = false;
            for (Candidate existing : nms) {
                if (boxIou(candidate.inputBox, existing.inputBox) >= 0.5f) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                nms.add(candidate);
            }
        }

        List<Detection> decoded = new ArrayList<>();
        for (Candidate candidate : nms) {
            Detection detection = createMask(candidate, prototypes, originalWidth, originalHeight, scale, padX, padY);
            if (detection != null) {
                decoded.add(detection);
            }
        }
        return decoded;
    }

    private Detection createMask(
            Candidate candidate,
            float[][][] prototypes,
            int originalWidth,
            int originalHeight,
            float scale,
            float padX,
            float padY
    ) {
        int left = clamp((int) Math.floor((candidate.inputBox.left - padX) / scale), 0, originalWidth - 1);
        int top = clamp((int) Math.floor((candidate.inputBox.top - padY) / scale), 0, originalHeight - 1);
        int right = clamp((int) Math.ceil((candidate.inputBox.right - padX) / scale), left + 1, originalWidth);
        int bottom = clamp((int) Math.ceil((candidate.inputBox.bottom - padY) / scale), top + 1, originalHeight);
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) {
            return null;
        }
        int protoHeight = prototypes[0].length;
        int protoWidth = prototypes[0][0].length;
        byte[] mask = new byte[width * height];
        int count = 0;
        for (int y = 0; y < height; y++) {
            float inputY = (top + y + 0.5f) * scale + padY;
            int protoY = clamp((int) (inputY * protoHeight / INPUT_SIZE), 0, protoHeight - 1);
            for (int x = 0; x < width; x++) {
                float inputX = (left + x + 0.5f) * scale + padX;
                int protoX = clamp((int) (inputX * protoWidth / INPUT_SIZE), 0, protoWidth - 1);
                float logit = 0f;
                for (int channel = 0; channel < prototypes.length; channel++) {
                    logit += candidate.coefficients[channel] * prototypes[channel][protoY][protoX];
                }
                if (logit > 0f) {
                    mask[y * width + x] = 1;
                    count++;
                }
            }
        }
        if (count < 12) {
            return null;
        }
        return new Detection("", candidate.part, candidate.score, new Rect(left, top, right, bottom), mask, width, height, count);
    }

    private static float[] bitmapToChw(Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        float[] data = new float[size * 3];
        for (int i = 0; i < size; i++) {
            int color = pixels[i];
            data[i] = Color.red(color) / 255f;
            data[size + i] = Color.green(color) / 255f;
            data[size * 2 + i] = Color.blue(color) / 255f;
        }
        return data;
    }

    private static float boxIou(RectF first, RectF second) {
        float left = Math.max(first.left, second.left);
        float top = Math.max(first.top, second.top);
        float right = Math.min(first.right, second.right);
        float bottom = Math.min(first.bottom, second.bottom);
        float intersection = Math.max(0f, right - left) * Math.max(0f, bottom - top);
        float union = first.width() * first.height() + second.width() * second.height() - intersection;
        return union <= 0f ? 0f : intersection / union;
    }

    private static float maskIou(Detection first, Detection second) {
        int left = Math.max(first.box.left, second.box.left);
        int top = Math.max(first.box.top, second.box.top);
        int right = Math.min(first.box.right, second.box.right);
        int bottom = Math.min(first.box.bottom, second.box.bottom);
        int intersection = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if (first.containsMaskPixel(x, y) && second.containsMaskPixel(x, y)) {
                    intersection++;
                }
            }
        }
        int union = first.maskCount + second.maskCount - intersection;
        return union <= 0 ? 0f : intersection / (float) union;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    @Override
    public void close() throws OrtException {
        for (LoadedModel model : models) {
            model.session.close();
        }
        models.clear();
    }

    private static final class LoadedModel {
        final ModelSpec spec;
        final OrtSession session;
        final String inputName;

        LoadedModel(ModelSpec spec, OrtSession session, String inputName) {
            this.spec = spec;
            this.session = session;
            this.inputName = inputName;
        }
    }

    private static final class Candidate {
        final Part part;
        final float score;
        final RectF inputBox;
        final float[] coefficients;

        Candidate(Part part, float score, RectF inputBox, float[] coefficients) {
            this.part = part;
            this.score = score;
            this.inputBox = inputBox;
            this.coefficients = coefficients;
        }
    }
}
