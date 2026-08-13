package com.buchile.censor.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_IMAGES = 101;
    private static final int REQUEST_STICKER = 102;
    private static final int REQUEST_EXPORT_ONE = 103;
    private static final int REQUEST_EXPORT_ALL = 104;
    private static final int MAX_IMAGE_EDGE = 4096;
    private static final int BACKGROUND = Color.rgb(13, 17, 23);
    private static final int PANEL = Color.rgb(32, 37, 48);
    private static final int TEXT = Color.rgb(244, 247, 251);
    private static final int MUTED = Color.rgb(167, 174, 189);
    private static final int ACCENT = Color.rgb(255, 75, 75);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<ImageRecord> images = new ArrayList<>();
    private final EnumSet<Part> selectedParts = EnumSet.allOf(Part.class);
    private final EnumMap<Part, CheckBox> partChecks = new EnumMap<>(Part.class);
    private final Random random = new Random();

    private LinearLayout root;
    private ImageView preview;
    private TextView status;
    private TextView counter;
    private TextView thresholdValue;
    private TextView strengthValue;
    private ProgressBar progress;
    private Button detectButton;
    private Button previousButton;
    private Button nextButton;
    private Spinner modeSpinner;

    private YoloSegDetector detector;
    private Bitmap currentBitmap;
    private Bitmap currentPreview;
    private Bitmap maodieSticker;
    private Bitmap dagouSticker;
    private Bitmap customSticker;
    private Bitmap pendingExport;
    private String pendingExportName;
    private int currentIndex;
    private float threshold = 0.35f;
    private int blockSize = 18;
    private CensorEngine.Mode mode = CensorEngine.Mode.PIXEL;
    private boolean showMarkers = true;
    private boolean english;
    private boolean busy;
    private boolean detectorReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        maodieSticker = loadAssetBitmap("maodie.png");
        dagouSticker = loadAssetBitmap("dagou.png");
        buildUi();
        loadModels();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(com.buchile.censor.mobile.R.drawable.buchile);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable logoBackground = new GradientDrawable();
        logoBackground.setCornerRadius(dp(14));
        logo.setBackground(logoBackground);
        header.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        TextView title = text(english ? "Buchile Android Beta" : "Buchile 安卓测试版", 25, TEXT, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(14), 0, dp(8), 0);
        header.addView(title, titleParams);
        Button language = button(english ? "中文" : "English", false);
        language.setOnClickListener(v -> {
            english = !english;
            buildUi();
            updateCurrentPreview();
        });
        header.addView(language);
        root.addView(header);

        TextView subtitle = text(
                tr("双模型轮廓检测 · 本地离线处理 · 普通/贴图马赛克 · 多图导出",
                        "Dual-model contours · fully offline · pixel/sticker masking · batch export"),
                14, MUTED, false);
        subtitle.setPadding(0, dp(12), 0, dp(10));
        root.addView(subtitle);

        TextView notice = text(tr(
                "本工具为免费开源工具，如果您是通过任何付费方式获得本工具，均为盗版！！😭",
                "This is free, open-source software. Any copy sold to you is unauthorized. 😭"), 15, Color.rgb(255, 222, 112), true);
        notice.setPadding(dp(14), dp(12), dp(14), dp(12));
        notice.setBackground(panelDrawable(Color.rgb(66, 64, 19), 12));
        addWithBottom(notice, 14);

        TextView guide = text(tr(
                "使用指引：① 先在下方选择需要处理的部位；② 选择一张或多张图片；③ 检测后调整马赛克并导出。所有处理均在手机本地完成。",
                "Quick start: ① choose region types; ② select one or more images; ③ detect, adjust masking, and export. Processing stays on this device."), 15, TEXT, false);
        guide.setPadding(dp(14), dp(14), dp(14), dp(14));
        guide.setBackground(panelDrawable(Color.rgb(25, 54, 77), 12));
        addWithBottom(guide, 16);

        Button choose = button(tr("选择一张或多张图片", "Select one or more images"), true);
        choose.setOnClickListener(v -> chooseImages());
        addWithBottom(choose, 12);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        addWithBottom(progress, 8);
        status = text("", 14, MUTED, false);
        addWithBottom(status, 14);

        addSection(tr("需要处理的部位", "Region types to mask"));
        TextView partHint = text(tr(
                "只有勾选的部位会应用马赛克；模型仍会一次检测全部类别。",
                "Only checked region types are masked; the models still detect all classes in one pass."), 13, MUTED, false);
        addWithBottom(partHint, 6);
        partChecks.clear();
        for (Part part : Part.values()) {
            CheckBox box = new CheckBox(this);
            box.setText(part.label(english));
            box.setTextColor(TEXT);
            box.setTextSize(15);
            box.setChecked(selectedParts.contains(part));
            box.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
            box.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedParts.add(part);
                } else {
                    selectedParts.remove(part);
                }
                updateCurrentPreview();
            });
            partChecks.put(part, box);
            root.addView(box);
        }

        addSection(tr("识别灵敏度", "Detection sensitivity"));
        thresholdValue = text(String.format(Locale.US, "%.2f", threshold), 14, TEXT, true);
        root.addView(thresholdValue);
        SeekBar thresholdBar = new SeekBar(this);
        thresholdBar.setMax(75);
        thresholdBar.setProgress(Math.round((threshold - 0.05f) * 100));
        thresholdBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        thresholdBar.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        thresholdBar.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold = 0.05f + progress / 100f;
                thresholdValue.setText(String.format(Locale.US, "%.2f", threshold));
            }
        });
        root.addView(thresholdBar);
        TextView thresholdHint = text(tr(
                "降低数值可补检遗漏区域，但也可能增加误检。修改后点击“重新检测当前图片”。首个测试版固定使用 640 推理尺寸。",
                "Lower values can recover missed regions but may add false positives. Tap Redetect current image after changing it. This first beta uses a fixed 640 inference size."), 13, MUTED, false);
        addWithBottom(thresholdHint, 10);

        detectButton = button(tr("重新检测当前图片", "Redetect current image"), false);
        detectButton.setOnClickListener(v -> detectCurrent(true));
        addWithBottom(detectButton, 16);

        addSection(tr("马赛克方式", "Masking method"));
        modeSpinner = new Spinner(this);
        String[] modeNames = new String[]{
                tr("普通马赛克", "Pixel mosaic"),
                tr("耄耋（猫咪样例）", "Kitty sample"),
                tr("大狗叫（大狗样例）", "Dog sample"),
                tr("自定义贴图", "Custom sticker")
        };
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modeNames);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setSelection(mode.ordinal(), false);
        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                mode = CensorEngine.Mode.values()[position];
                if (mode == CensorEngine.Mode.CUSTOM && customSticker == null) {
                    chooseSticker();
                } else {
                    updateCurrentPreview();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        addWithBottom(modeSpinner, 8);

        strengthValue = text(String.format(
                java.util.Locale.getDefault(),
                tr("像素块大小：%d", "Pixel block size: %d"),
                blockSize), 14, TEXT, true);
        root.addView(strengthValue);
        SeekBar strength = new SeekBar(this);
        strength.setMax(62);
        strength.setProgress(blockSize - 2);
        strength.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        strength.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        strength.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                blockSize = progress + 2;
                strengthValue.setText(String.format(
                        java.util.Locale.getDefault(),
                        tr("像素块大小：%d", "Pixel block size: %d"),
                        blockSize));
                if (!fromUser || !busy) {
                    updateCurrentPreview();
                }
            }
        });
        addWithBottom(strength, 8);

        Switch markerSwitch = new Switch(this);
        markerSwitch.setText(tr("在预览中显示识别区域序号", "Show detected-region numbers in preview"));
        markerSwitch.setTextColor(TEXT);
        markerSwitch.setTextSize(14);
        markerSwitch.setChecked(showMarkers);
        markerSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(showMarkers ? ACCENT : MUTED));
        markerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showMarkers = isChecked;
            ((Switch) buttonView).setThumbTintList(android.content.res.ColorStateList.valueOf(isChecked ? ACCENT : MUTED));
            updateCurrentPreview();
        });
        addWithBottom(markerSwitch, 12);
        TextView markerHint = text(tr(
                "序号只显示在预览中，永远不会写入导出图片。",
                "Numbers are preview-only and are never written to exported images."), 13, MUTED, false);
        addWithBottom(markerHint, 10);

        addSection(tr("图片预览", "Image preview"));
        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackground(panelDrawable(PANEL, 12));
        preview.setMinimumHeight(dp(240));
        addWithBottom(preview, 10);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        previousButton = button(tr("← 上一张", "← Previous"), false);
        nextButton = button(tr("下一张 →", "Next →"), false);
        counter = text("0 / 0", 15, TEXT, true);
        counter.setGravity(Gravity.CENTER);
        previousButton.setOnClickListener(v -> move(-1));
        nextButton.setOnClickListener(v -> move(1));
        navigation.addView(previousButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        navigation.addView(counter, new LinearLayout.LayoutParams(0, dp(50), 1f));
        navigation.addView(nextButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        addWithBottom(navigation, 10);

        LinearLayout exportRow = new LinearLayout(this);
        exportRow.setOrientation(LinearLayout.HORIZONTAL);
        Button exportOne = button(tr("导出当前图片", "Export current"), false);
        Button exportAll = button(tr("批量导出全部", "Batch export all"), true);
        exportOne.setOnClickListener(v -> exportCurrent());
        exportAll.setOnClickListener(v -> chooseExportFolder());
        exportRow.addView(exportOne, new LinearLayout.LayoutParams(0, dp(52), 1f));
        LinearLayout.LayoutParams allParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        allParams.setMargins(dp(8), 0, 0, 0);
        exportRow.addView(exportAll, allParams);
        addWithBottom(exportRow, 18);

        addSection(tr("小礼物", "A small gift"));
        Button gift = button(tr("🎁 送你一只小猫咪，点击即送！", "🎁 Tap to receive a kitty!"), false);
        gift.setOnClickListener(v -> showKitty());
        addWithBottom(gift, 18);

        TextView links = text("", 13, MUTED, false);
        String html = tr(
                "作者：<a href='https://www.pixiv.net/en/users/118035672'>Buchile</a><br>" +
                        "项目：<a href='https://github.com/Buchile67/buchile-censor'>基础版</a> · " +
                        "<a href='https://github.com/Buchile67/buchile-censor-vanguard-beta'>先锋版</a><br>" +
                        "参考：<a href='https://github.com/frinkleko/AutoHajimiMosaic'>AutoHajimiMosaic</a> · " +
                        "<a href='https://github.com/spawner1145/auto-censor'>auto-censor</a>",
                "Author: <a href='https://www.pixiv.net/en/users/118035672'>Buchile</a><br>" +
                        "Projects: <a href='https://github.com/Buchile67/buchile-censor'>Base Edition</a> · " +
                        "<a href='https://github.com/Buchile67/buchile-censor-vanguard-beta'>Vanguard Beta</a><br>" +
                        "References: <a href='https://github.com/frinkleko/AutoHajimiMosaic'>AutoHajimiMosaic</a> · " +
                        "<a href='https://github.com/spawner1145/auto-censor'>auto-censor</a>");
        links.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        links.setMovementMethod(LinkMovementMethod.getInstance());
        links.setLinkTextColor(Color.rgb(110, 177, 255));
        root.addView(links);
        updateControls();
        if (busy) {
            setStatus(tr("正在处理…", "Processing…"));
        } else if (!detectorReady) {
            setStatus(tr("正在载入本地模型…首次启动可能需要几秒。", "Loading local models… the first launch may take a few seconds."));
        } else if (images.isEmpty()) {
            setStatus(tr("模型已就绪。请选择图片。", "Models ready. Select images to begin."));
        }
    }

    private void loadModels() {
        setBusy(true, tr("正在载入本地模型…", "Loading local models…"));
        worker.execute(() -> {
            try {
                YoloSegDetector loaded = new YoloSegDetector(getApplicationContext());
                runOnUiThread(() -> {
                    detector = loaded;
                    detectorReady = true;
                    setBusy(false, images.isEmpty()
                            ? tr("模型已就绪。请选择图片。", "Models ready. Select images to begin.")
                            : tr("模型已就绪。", "Models ready."));
                    if (!images.isEmpty()) {
                        if (currentBitmap == null) {
                            showCurrent(true);
                        } else if (images.get(currentIndex).detections == null) {
                            detectCurrent(false);
                        }
                    }
                });
            } catch (Throwable error) {
                runOnUiThread(() -> setBusy(false, tr("模型载入失败：", "Model loading failed: ") + readable(error)));
            }
        });
    }

    private void chooseImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMAGES);
    }

    private void chooseSticker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_STICKER);
    }

    private void chooseExportFolder() {
        if (images.isEmpty()) {
            toast(tr("请先选择图片。", "Select images first."));
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_EXPORT_ALL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            if (requestCode == REQUEST_STICKER && customSticker == null && mode == CensorEngine.Mode.CUSTOM) {
                mode = CensorEngine.Mode.PIXEL;
                if (modeSpinner != null) {
                    modeSpinner.setSelection(mode.ordinal());
                }
            }
            return;
        }
        if (requestCode == REQUEST_IMAGES) {
            List<Uri> selected = collectUris(data);
            if (selected.isEmpty()) {
                return;
            }
            images.clear();
            for (Uri uri : selected) {
                persistReadPermission(uri, data.getFlags());
                images.add(new ImageRecord(uri, displayName(uri)));
            }
            currentIndex = 0;
            showCurrent(true);
        } else if (requestCode == REQUEST_STICKER) {
            Uri uri = data.getData();
            if (uri != null) {
                persistReadPermission(uri, data.getFlags());
                setBusy(true, tr("正在读取贴图…", "Loading sticker…"));
                worker.execute(() -> {
                    try {
                        Bitmap bitmap = decodeUri(uri);
                        runOnUiThread(() -> {
                            recycle(customSticker);
                            customSticker = bitmap;
                            mode = CensorEngine.Mode.CUSTOM;
                            setBusy(false, tr("已载入自定义贴图。", "Custom sticker loaded."));
                            updateCurrentPreview();
                        });
                    } catch (Throwable error) {
                        runOnUiThread(() -> {
                            mode = CensorEngine.Mode.PIXEL;
                            if (modeSpinner != null) {
                                modeSpinner.setSelection(mode.ordinal());
                            }
                            setBusy(false, tr("贴图读取失败：", "Sticker loading failed: ") + readable(error));
                        });
                    }
                });
            }
        } else if (requestCode == REQUEST_EXPORT_ONE) {
            Uri uri = data.getData();
            if (uri != null && pendingExport != null) {
                Bitmap export = pendingExport;
                pendingExport = null;
                writeBitmap(uri, export, tr("图片已导出。", "Image exported."));
            }
        } else if (requestCode == REQUEST_EXPORT_ALL) {
            Uri tree = data.getData();
            if (tree != null) {
                try {
                    getContentResolver().takePersistableUriPermission(tree,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                exportAll(tree);
            }
        }
    }

    private void showCurrent(boolean detectIfMissing) {
        if (images.isEmpty() || busy) {
            updateControls();
            return;
        }
        int targetIndex = currentIndex;
        ImageRecord record = images.get(targetIndex);
        setBusy(true, tr("正在读取图片…", "Loading image…"));
        worker.execute(() -> {
            try {
                Bitmap bitmap = decodeUri(record.uri);
                List<Detection> found = record.detections;
                if (detectIfMissing && found == null && detectorReady) {
                    runOnUiThread(() -> setStatus(tr("正在检测当前图片…", "Detecting current image…")));
                    found = detector.detect(bitmap, threshold);
                    record.detections = found;
                    record.threshold = threshold;
                }
                List<Detection> finalFound = found;
                runOnUiThread(() -> {
                    if (targetIndex != currentIndex) {
                        bitmap.recycle();
                        return;
                    }
                    recycle(currentBitmap);
                    currentBitmap = bitmap;
                    setBusy(false, detectionSummary(record, finalFound));
                    updateCurrentPreview();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> setBusy(false, tr("图片读取或检测失败：", "Image loading or detection failed: ") + readable(error)));
            }
        });
    }

    private void detectCurrent(boolean force) {
        if (!detectorReady || detector == null) {
            toast(tr("模型仍在载入，请稍候。", "Models are still loading."));
            return;
        }
        if (images.isEmpty() || currentBitmap == null || busy) {
            toast(tr("请先选择图片。", "Select an image first."));
            return;
        }
        int targetIndex = currentIndex;
        ImageRecord record = images.get(targetIndex);
        Bitmap bitmap = currentBitmap;
        float requestedThreshold = threshold;
        setBusy(true, tr("正在检测当前图片…", "Detecting current image…"));
        worker.execute(() -> {
            try {
                List<Detection> found = detector.detect(bitmap, requestedThreshold);
                record.detections = found;
                record.threshold = requestedThreshold;
                runOnUiThread(() -> {
                    if (targetIndex == currentIndex) {
                        setBusy(false, detectionSummary(record, found));
                        updateCurrentPreview();
                    }
                });
            } catch (Throwable error) {
                runOnUiThread(() -> setBusy(false, tr("检测失败：", "Detection failed: ") + readable(error)));
            }
        });
    }

    private void move(int delta) {
        if (busy || images.isEmpty()) {
            return;
        }
        int next = Math.max(0, Math.min(images.size() - 1, currentIndex + delta));
        if (next != currentIndex) {
            currentIndex = next;
            showCurrent(true);
        }
    }

    private void updateCurrentPreview() {
        if (preview == null) {
            return;
        }
        if (images.isEmpty() || currentBitmap == null) {
            preview.setImageDrawable(null);
            updateControls();
            return;
        }
        ImageRecord record = images.get(currentIndex);
        List<Detection> detections = record.detections == null ? new ArrayList<>() : record.detections;
        Bitmap rendered = CensorEngine.render(currentBitmap, detections, selectedParts, mode, blockSize, selectedSticker(), showMarkers);
        recycle(currentPreview);
        currentPreview = rendered;
        preview.setImageBitmap(currentPreview);
        updateControls();
    }

    private Bitmap selectedSticker() {
        if (mode == CensorEngine.Mode.MAODIE) {
            return maodieSticker;
        }
        if (mode == CensorEngine.Mode.DAGOU) {
            return dagouSticker;
        }
        if (mode == CensorEngine.Mode.CUSTOM) {
            return customSticker;
        }
        return null;
    }

    private void exportCurrent() {
        if (images.isEmpty() || currentBitmap == null || busy) {
            toast(tr("请先选择并检测图片。", "Select and detect an image first."));
            return;
        }
        ImageRecord record = images.get(currentIndex);
        if (record.detections == null) {
            toast(tr("请先检测当前图片。", "Detect the current image first."));
            return;
        }
        pendingExport = CensorEngine.render(currentBitmap, record.detections, selectedParts, mode, blockSize, selectedSticker(), false);
        pendingExportName = outputName(record.name);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, pendingExportName);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_EXPORT_ONE);
    }

    private void writeBitmap(Uri uri, Bitmap bitmap, String successMessage) {
        setBusy(true, tr("正在导出…", "Exporting…"));
        worker.execute(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
                if (output == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw new IOException("PNG encoding failed");
                }
                bitmap.recycle();
                runOnUiThread(() -> setBusy(false, successMessage));
            } catch (Throwable error) {
                bitmap.recycle();
                runOnUiThread(() -> setBusy(false, tr("导出失败：", "Export failed: ") + readable(error)));
            }
        });
    }

    private void exportAll(Uri treeUri) {
        if (!detectorReady || detector == null || busy) {
            toast(tr("模型仍在载入或正在执行其他任务。", "Models are loading or another task is running."));
            return;
        }
        Set<Part> parts = EnumSet.copyOf(selectedParts);
        CensorEngine.Mode requestedMode = mode;
        int requestedBlock = blockSize;
        Bitmap sticker = selectedSticker();
        float requestedThreshold = threshold;
        setBusy(true, tr("正在批量处理…", "Batch processing…"));
        worker.execute(() -> {
            int completed = 0;
            try {
                DocumentFile folder = DocumentFile.fromTreeUri(this, treeUri);
                if (folder == null || !folder.canWrite()) {
                    throw new IOException("The selected folder is not writable");
                }
                for (int index = 0; index < images.size(); index++) {
                    ImageRecord record = images.get(index);
                    int progressIndex = index + 1;
                    runOnUiThread(() -> setStatus(tr("正在处理第 ", "Processing image ") + progressIndex + " / " + images.size()));
                    Bitmap bitmap = decodeUri(record.uri);
                    List<Detection> detections = record.detections;
                    if (detections == null || Math.abs(record.threshold - requestedThreshold) > 0.0001f) {
                        detections = detector.detect(bitmap, requestedThreshold);
                        record.detections = detections;
                        record.threshold = requestedThreshold;
                    }
                    Bitmap output = CensorEngine.render(bitmap, detections, parts, requestedMode, requestedBlock, sticker, false);
                    bitmap.recycle();
                    String name = uniqueName(folder, outputName(record.name));
                    DocumentFile target = folder.createFile("image/png", name);
                    if (target == null) {
                        output.recycle();
                        throw new IOException("Could not create " + name);
                    }
                    try (OutputStream stream = getContentResolver().openOutputStream(target.getUri(), "w")) {
                        if (stream == null || !output.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                            throw new IOException("Could not encode " + name);
                        }
                    } finally {
                        output.recycle();
                    }
                    completed++;
                }
                int count = completed;
                runOnUiThread(() -> {
                    setBusy(false, tr("批量导出完成：", "Batch export complete: ") + count + tr(" 张图片。", " images."));
                    updateCurrentPreview();
                });
            } catch (Throwable error) {
                int count = completed;
                runOnUiThread(() -> setBusy(false,
                        tr("批量导出中断，已完成 ", "Batch export stopped after ") + count + tr(" 张：", " images: ") + readable(error)));
            }
        });
    }

    private void showKitty() {
        int number = 1 + random.nextInt(16);
        String name = String.format(Locale.US, "kitty_gallery/kitty_%02d.png", number);
        Bitmap kitty = loadAssetBitmap(name);
        if (kitty == null) {
            toast(tr("小猫咪暂时躲起来了。", "The kitty is hiding right now."));
            return;
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(12));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageBitmap(kitty);
        int imageHeight = Math.min(dp(520), (int) (getResources().getDisplayMetrics().heightPixels * 0.58f));
        content.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight));
        TextView message = text(tr("✨ 收下属于你的可爱耄耋吧！✨", "✨ Accept your adorable kitty! ✨"), 18, Color.rgb(255, 112, 142), true);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dp(12), 0, 0);
        content.addView(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton(tr("收下", "Keep it"), null)
                .create();
        dialog.setOnDismissListener(ignored -> kitty.recycle());
        dialog.show();
    }

    private Bitmap decodeUri(Uri uri) throws IOException {
        ContentResolver resolver = getContentResolver();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_EDGE) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream stream = resolver.openInputStream(uri)) {
            decoded = BitmapFactory.decodeStream(stream, null, options);
        }
        if (decoded == null) {
            throw new IOException("Unsupported image");
        }
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream stream = resolver.openInputStream(uri)) {
            if (stream != null) {
                orientation = new ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Throwable ignored) {
        }
        Matrix matrix = new Matrix();
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            matrix.postRotate(90);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            matrix.postRotate(180);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            matrix.postRotate(270);
        } else if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) {
            matrix.postScale(-1, 1);
        } else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) {
            matrix.postScale(1, -1);
        }
        if (!matrix.isIdentity()) {
            Bitmap rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
            if (rotated != decoded) {
                decoded.recycle();
            }
            decoded = rotated;
        }
        return decoded;
    }

    private Bitmap loadAssetBitmap(String name) {
        try (InputStream stream = getAssets().open(name)) {
            return BitmapFactory.decodeStream(stream);
        } catch (IOException error) {
            return null;
        }
    }

    private List<Uri> collectUris(Intent data) {
        List<Uri> result = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null && !result.contains(uri)) {
                    result.add(uri);
                }
            }
        } else if (data.getData() != null) {
            result.add(data.getData());
        }
        return result;
    }

    private void persistReadPermission(Uri uri, int returnedFlags) {
        int flags = returnedFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    return cursor.getString(column);
                }
            }
        } catch (Throwable ignored) {
        }
        String segment = uri.getLastPathSegment();
        return segment == null ? "image.png" : segment;
    }

    private String outputName(String input) {
        String base = input == null ? "image" : input.replaceAll("(?i)\\.(png|jpe?g|webp|bmp|tiff?)$", "");
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (base.trim().isEmpty()) {
            base = "image";
        }
        return base + "_buchile.png";
    }

    private String uniqueName(DocumentFile folder, String requested) {
        if (folder.findFile(requested) == null) {
            return requested;
        }
        String stem = requested.replaceFirst("(?i)\\.png$", "");
        int index = 2;
        while (folder.findFile(stem + "_" + index + ".png") != null) {
            index++;
        }
        return stem + "_" + index + ".png";
    }

    private String detectionSummary(ImageRecord record, List<Detection> detections) {
        if (detections == null) {
            return detectorReady ? tr("等待检测。", "Waiting for detection.") : tr("模型仍在载入。", "Models are loading.");
        }
        if (detections.isEmpty()) {
            return tr("没有找到区域。可降低识别灵敏度数值后重新检测。",
                    "No regions found. Lower the sensitivity value and redetect if needed.");
        }
        return record.name + " · " + tr("检测到 ", "Detected ") + detections.size() + tr(" 个区域。", " regions.");
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        if (progress != null) {
            progress.setVisibility(value ? View.VISIBLE : View.GONE);
        }
        setStatus(message);
        updateControls();
    }

    private void setStatus(String message) {
        if (status != null) {
            status.setText(message == null ? "" : message);
        }
    }

    private void updateControls() {
        if (counter != null) {
            counter.setText(images.isEmpty() ? "0 / 0" : (currentIndex + 1) + " / " + images.size());
        }
        if (previousButton != null) {
            previousButton.setEnabled(!busy && currentIndex > 0);
        }
        if (nextButton != null) {
            nextButton.setEnabled(!busy && !images.isEmpty() && currentIndex < images.size() - 1);
        }
        if (detectButton != null) {
            detectButton.setEnabled(!busy && detectorReady && !images.isEmpty() && currentBitmap != null);
        }
    }

    private String tr(String zh, String en) {
        return english ? en : zh;
    }

    private void addSection(String value) {
        TextView heading = text(value, 18, TEXT, true);
        heading.setPadding(0, dp(14), 0, dp(8));
        root.addView(heading);
    }

    private void addWithBottom(View view, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(bottomDp));
        root.addView(view, params);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        view.setLineSpacing(0, 1.12f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button view = new Button(this);
        view.setText(value);
        view.setTextColor(TEXT);
        view.setTextSize(14);
        view.setAllCaps(false);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setBackground(panelDrawable(primary ? ACCENT : PANEL, 10));
        return view;
    }

    private GradientDrawable panelDrawable(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (color == PANEL) {
            drawable.setStroke(dp(1), Color.rgb(61, 68, 81));
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static String readable(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
        recycle(currentBitmap);
        recycle(currentPreview);
        recycle(maodieSticker);
        recycle(dagouSticker);
        recycle(customSticker);
        recycle(pendingExport);
        if (detector != null) {
            try {
                detector.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static final class ImageRecord {
        final Uri uri;
        final String name;
        List<Detection> detections;
        float threshold = -1f;

        ImageRecord(Uri uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
