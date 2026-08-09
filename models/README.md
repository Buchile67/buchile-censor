# Model setup / 模型放置说明

The application expects these two files in this directory:

程序需要在本目录中找到以下两个文件：

| Local filename / 本地文件名 | Upstream source / 上游来源 |
| --- | --- |
| `hachimi_segmentation.pt` | [AutoHajimiMosaic `segmentation_model.pt`](https://github.com/frinkleko/AutoHajimiMosaic/blob/main/models/segmentation_model.pt) |
| `maodie_segmentation.pt` | [Model page linked by Wenaka2004/auto-censor](https://civitai.com/models/1736285?modelVersionId=1965032) |

Download the upstream files, extract archives when necessary, and rename the segmentation weights to the local filenames shown above.

请下载上游文件，在需要时解压，并将分割模型权重重命名为表格所列的本地文件名。

Model weights are intentionally excluded from Git. They may have separate licenses, access requirements, and usage restrictions. Review the upstream terms before downloading or using them.

模型权重有意排除在 Git 仓库之外。它们可能具有独立许可证、访问要求和使用限制；下载或使用前请阅读上游条款。
