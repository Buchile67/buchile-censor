# Buchile Censor / Buchile 图像遮挡工具

一款本地运行的轮廓级图像遮挡工具，支持像素马赛克、贴图覆盖、区域选择与批量导出。图片处理在本机完成，程序不会主动上传输入文件。

A local contour-aware image masking tool with pixel mosaics, sticker overlays, region selection, and batch export. Input images are processed on the local machine and are not uploaded by the application.

## 中文

### 主要功能

- 使用两套 YOLO 分割模型进行互补检测。
- 遮挡效果限制在模型输出的像素级轮廓内。
- 支持可调强度的像素马赛克。
- 支持大狗、猫咪两种内置贴图及自定义贴图。
- 支持部位预设、逐区域开关与指定类别的低阈值补检。
- 支持多图上传、批量处理、PNG/JPEG 输出及 ZIP 导出。
- 支持中文和英文界面。

### 安装与启动

Windows 用户可从 [Releases](https://github.com/themedark23-oss/buchile-censor/releases) 下载包含模型的完整包，解压后双击 `start_autoex.bat`。首次启动会创建独立的 `.venv` 环境并安装依赖，需要 Python 3.10 或更高版本及网络连接。

从源码运行：

```powershell
git clone https://github.com/themedark23-oss/buchile-censor.git
cd buchile-censor
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

按照 [`models/README.md`](models/README.md) 放置模型，然后运行 `start_autoex.bat`。

## English

### Features

- Complementary detection using two YOLO segmentation models.
- Masking effects constrained to pixel-level model contours.
- Adjustable pixel-mosaic strength.
- Built-in dog and cat stickers, plus custom sticker uploads.
- Region presets, per-instance selection, and lower-threshold recovery for selected categories.
- Multi-image processing with PNG/JPEG output and ZIP export.
- Chinese and English interface.

### Setup

Windows users can download the model-included package from [Releases](https://github.com/themedark23-oss/buchile-censor/releases), extract it, and run `start_autoex.bat`. The first launch creates an isolated `.venv` and installs dependencies. Python 3.10 or newer and a network connection are required.

To run from source:

```powershell
git clone https://github.com/themedark23-oss/buchile-censor.git
cd buchile-censor
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Place the model files described in [`models/README.md`](models/README.md), then run `start_autoex.bat`.

## Models and references / 模型与参考项目

- [AutoHajimiMosaic segmentation model](https://github.com/frinkleko/AutoHajimiMosaic/blob/main/models/segmentation_model.pt)
- [Wenaka model download page](https://civitai.com/models/1736285?modelVersionId=1965032)
- [frinkleko/AutoHajimiMosaic](https://github.com/frinkleko/AutoHajimiMosaic) — interaction and segmentation workflow.
- [spawner1145/auto-censor](https://github.com/spawner1145/auto-censor) — processing workflow and extensions.
- [Wenaka2004/auto-censor](https://github.com/Wenaka2004/auto-censor) — earlier YOLO masking workflow and model reference.
- [Ultralytics](https://github.com/ultralytics/ultralytics) — YOLO inference framework.

Source code is released under GPL-3.0. Model weights remain subject to their upstream terms. The dog and cat sticker samples are provided by Buchile.

源代码按 GPL-3.0 发布；模型权利与使用条件以上游说明为准。大狗与猫咪贴图样例由 Buchile 提供。

## Responsible use / 使用边界

Only process files you own or are authorized to edit. Do not use this tool for illegal material, non-consensual private material, or any material involving minors.

请只处理自己拥有或获准编辑的文件。请勿用于违法内容、未经同意的私人内容或任何涉及未成年人的内容。

## Buchile

- GitHub: [Buchile](https://github.com/themedark23-oss)
- Pixiv: [Buchile](https://www.pixiv.net/en/users/118035672)

## License / 许可证

[GPL-3.0](LICENSE)
