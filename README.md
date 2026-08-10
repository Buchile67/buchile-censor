# Buchile Censor / Buchile 图像遮挡工具

一款本地运行的轮廓级图像遮挡工具，支持像素马赛克、贴图覆盖、区域选择与批量导出。图片处理在本机完成，程序不会主动上传输入文件。

A local contour-aware image masking tool with pixel mosaics, sticker overlays, region selection, and batch export. Input images are processed on the local machine and are not uploaded by the application.

## 版本选择 / Edition guide

[基础版 / Base Edition](https://github.com/Buchile67/buchile-censor) · [先锋版 / Vanguard Beta](https://github.com/Buchile67/buchile-censor-vanguard-beta)

| 版本 / Edition | 主要优势 / Advantages | 适用场景 / Best for |
| --- | --- | --- |
| **基础版 / Base Edition（当前 / Current）** | 环境更轻、启动与批量处理更快；保留轮廓级遮挡、贴图和低阈值补检等核心功能。<br>Lighter runtime and faster startup/batch processing while retaining contour masking, stickers, and low-threshold recovery. | 优先考虑易部署、处理速度和批量工作流。<br>Prioritizing simpler deployment, speed, and batch workflows. |
| **[先锋版 / Vanguard Beta](https://github.com/Buchile67/buchile-censor-vanguard-beta)** | 增加 SAM 2.1 自动与交互式精修、包含/排除点、CPU/GPU 选择及按图片/区域保存参数。<br>Adds SAM 2.1 automatic and interactive refinement, include/exclude points, CPU/GPU selection, and per-image/per-region profiles. | 对轮廓控制要求更高，并可接受更大的环境和更长的处理时间。<br>Higher contour-control requirements where a larger runtime and longer processing time are acceptable. |

## 中文

### 主要功能

- 使用两套 YOLO 分割模型进行互补检测。
- 遮挡效果限制在模型输出的像素级轮廓内。
- 识别阈值、补检参数和推理尺寸可应用到当前图片或全部图片。
- 预览图可显示区域序号；序号不会写入导出文件。
- 支持可调强度的像素马赛克。
- 支持大狗叫、耄耋两种内置贴图及自定义贴图；贴图模式不会叠加像素马赛克。
- 支持部位预设、逐区域开关与指定类别的低阈值补检。
- 支持多图上传、批量处理、PNG/JPEG 输出及 ZIP 导出。
- 支持中文和英文界面。
- 页面底部包含离线礼盒小游戏，可随机领取一张内置猫咪图片。

### 安装与启动

方式一：
一键包：Windows 用户可从 [Releases](https://github.com/Buchile67/buchile-censor/releases) 下载包含模型与 Miniconda 安装包的完整包，解压后双击 `start_autoex.bat`。无需预先安装 Python 或 Miniconda；首次启动按提示输入 `Y` 后，程序会安装一套不影响系统 Python 的私有运行环境，再自动安装所需依赖。请保持网络畅通和窗口开启，并预留数 GB 磁盘空间；之后启动会直接复用已经准备好的环境。

基础版和先锋版共用 `%LOCALAPPDATA%\BuchileRuntime\miniconda`，但使用彼此独立的 Python 环境，不会互相覆盖。旧版已经创建的 `.venv` 也会继续优先使用。

方式二：
从源码运行（无代码基础不推荐）：

```powershell
git clone https://github.com/Buchile67/buchile-censor.git
cd buchile-censor
```

按照 [`models/README.md`](models/README.md) 放置模型，然后运行 `start_autoex.bat`。源码仓库不包含体积较大的 Miniconda 安装程序，首次启动会从官方地址下载并校验它。

如果安装中断，请重新运行启动脚本。安装器不会覆盖已有的完整环境；如仍失败，请复制命令窗口中的完整错误信息。Miniconda 文件来源、校验值和许可说明见 [`installer/README.md`](installer/README.md)。

## English

### Features

- Complementary detection using two YOLO segmentation models.
- Masking effects constrained to pixel-level model contours.
- Detection thresholds, recovery settings, and inference size can apply to the current image or all images.
- Optional numbered region markers in previews; markers are never included in exports.
- Adjustable pixel-mosaic strength.
- Built-in dog and cat stickers plus custom uploads; sticker mode does not mix in pixel mosaics.
- Region presets, per-instance selection, and lower-threshold recovery for selected categories.
- Multi-image processing with PNG/JPEG output and ZIP export.
- Chinese and English interface.
- Includes an offline gift-box mini-game with a randomly selected built-in kitty image.

### Setup

**Method 1 — release package:** Download the complete package with models and the Miniconda installer from [Releases](https://github.com/Buchile67/buchile-censor/releases), extract it, and run `start_autoex.bat`. No prior Python or Miniconda installation is required. On first launch, type `Y` when prompted; the setup creates a private runtime without changing the system Python, then installs the dependencies automatically. Keep the network connected and the window open, and allow several GB of free disk space. Later launches reuse the prepared environment.

The base and Vanguard editions share `%LOCALAPPDATA%\BuchileRuntime\miniconda` but use separate Python environments. An existing legacy `.venv` remains supported and takes priority.

**Method 2 — source (not recommended for users without coding experience):**

```powershell
git clone https://github.com/Buchile67/buchile-censor.git
cd buchile-censor
```

Place the model files described in [`models/README.md`](models/README.md), then run `start_autoex.bat`. A source checkout does not include the large Miniconda installer, so the first launch downloads and verifies it from the official source.

If setup is interrupted, run the launcher again. Existing complete environments are not overwritten. If it still fails, copy the full console output when requesting help. See [`installer/README.md`](installer/README.md) for the installer source, checksum, and license notice.

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

- GitHub: [Buchile](https://github.com/Buchile67)
- Pixiv: [Buchile](https://www.pixiv.net/en/users/118035672)

## License / 许可证

[GPL-3.0](LICENSE)
