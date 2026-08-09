# Buchile Censor / Buchile 图像遮挡工具

给图片里指定区域盖上一床像素小被子，或者贴一张狗头/猫脸——活都在本地电脑里完成，图片不会被工具上传。

A tiny local sidekick that puts pixel blankets or dog/cat stickers over selected image regions. Everything runs on your computer; the app does not upload your pictures.

## 中文

### 它会做什么

- 两套分割模型搭档干活，按轮廓寻找目标区域。
- 普通马赛克可以调像素块大小：从“依稀可见”到“这是谁的乐高”。
- 贴图马赛克自带大狗和猫咪两位值班员，也能上传自己的贴图。
- 可选择部位预设、单独开关每个检测区域，也能降低指定类别的阈值来捞回漏网之鱼。
- 一次上传多张图，批量处理后打包下载。
- 中文、English 随时切换。

### 最省事的用法

到 [Releases](https://github.com/themedark23-oss/buchile-censor/releases) 下载带模型的一键包，解压后双击 `start_autoex.bat`。第一次启动会准备独立运行环境，需要一点时间和网络；以后再开就快多了。

需要电脑已安装 Python 3.10 或更高版本。Windows 如果询问是否允许联网，请允许它下载运行依赖。

### 从源码启动

```powershell
git clone https://github.com/themedark23-oss/buchile-censor.git
cd buchile-censor
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

按照 [`models/README.md`](models/README.md) 放好两个模型，然后双击 `start_autoex.bat`，或运行：

```powershell
.\.venv\Scripts\python.exe -m streamlit run app.py
```

## English

### What it does

- Two segmentation models team up to find target regions by contour.
- Adjustable pixel blocks, ranging from “soft disguise” to “who spilled the LEGO?”
- Two built-in sticker helpers—a dog and a cat—plus your own custom upload.
- Presets, per-detection switches, and lower-threshold recovery for stubborn misses.
- Multi-image processing with one ZIP download at the end.
- Switch between Chinese and English whenever you like.

### The easy route

Grab the model-included bundle from [Releases](https://github.com/themedark23-oss/buchile-censor/releases), extract it, and double-click `start_autoex.bat`. The first launch builds its own little runtime and downloads dependencies, so it needs a network connection and a bit of patience. Later launches are much quicker.

Python 3.10 or newer must already be installed on Windows.

### Run from source

```powershell
git clone https://github.com/themedark23-oss/buchile-censor.git
cd buchile-censor
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Place both models as explained in [`models/README.md`](models/README.md), then double-click `start_autoex.bat` or run:

```powershell
.\.venv\Scripts\python.exe -m streamlit run app.py
```

## Models / 模型

The source repository keeps model weights outside Git so it stays pleasantly small. The downloadable one-click package includes them. Upstream pages and their terms still apply.

源码仓库没有塞进模型权重，克隆时不会拖着两个大行李箱；Release 中的一键包会带上它们。模型仍以上游页面及其条款为准。

- [AutoHajimiMosaic segmentation model](https://github.com/frinkleko/AutoHajimiMosaic/blob/main/models/segmentation_model.pt)
- [Wenaka model download page](https://civitai.com/models/1736285?modelVersionId=1965032)（第三方页面，可能需要登录或调整内容偏好）

## Thanks / 感谢名单

这只小工具没有凭空长出来。以下项目提供了灵感、工作流程或底层能力：

This little app did not spring from a mysterious pixel cloud. Hats off to:

- [frinkleko/AutoHajimiMosaic](https://github.com/frinkleko/AutoHajimiMosaic) — interaction and segmentation workflow.
- [spawner1145/auto-censor](https://github.com/spawner1145/auto-censor) — processing workflow and follow-up ideas.
- [Wenaka2004/auto-censor](https://github.com/Wenaka2004/auto-censor) — the earlier YOLO masking workflow and model trail.
- [Ultralytics](https://github.com/ultralytics/ultralytics) — YOLO inference framework.

The code is released under GPL-3.0. Model weights keep their respective upstream terms. The bundled dog and cat sticker samples are provided by Buchile.

代码按 GPL-3.0 发布；模型权利与使用条件仍以上游说明为准。一键包中的大狗与猫咪贴图样例由 Buchile 提供。

## Keep it sensible / 请文明使用

Only process files you own or are allowed to edit. Do not use the tool for illegal material, non-consensual private material, or anything involving minors. The repository contains no sample media of that kind.

请只处理自己拥有或获准编辑的文件。请勿用于违法内容、未经同意的私人内容或任何涉及未成年人的内容。仓库不会放置此类示例媒体。

## Buchile

- GitHub: [Buchile](https://github.com/themedark23-oss)
- Pixiv: [Buchile](https://www.pixiv.net/en/users/118035672)

## License / 许可证

[GPL-3.0](LICENSE)
