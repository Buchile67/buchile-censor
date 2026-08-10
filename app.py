from __future__ import annotations

import hashlib
import zipfile
from io import BytesIO
from pathlib import Path

import numpy as np
import streamlit as st
from PIL import Image

from core import (
    MODEL_SPECS,
    PART_LABELS,
    apply_censor,
    decode_image,
    decode_sticker,
    detect_regions,
    detections_for_parts,
    draw_detection_markers,
    encode_image,
    load_segmentation_models,
)


ROOT = Path(__file__).resolve().parent
MODEL_DIR = ROOT / "models"
BRAND_ICON = ROOT / "assets" / "buchile.jpg"
PIXIV_URL = "https://www.pixiv.net/en/users/118035672"
STICKER_SAMPLES = {
    "dagou": ROOT / "assets" / "dagou.png",
    "maodie": ROOT / "assets" / "maodie.png",
}

DEFAULT_DETECTION_PROFILE = {
    "base_threshold": 0.35,
    "supplement_parts": (),
    "supplement_threshold": 0.12,
    "image_size": 960,
}

PARTS = list(PART_LABELS)
PRESETS = {
    "all": PARTS,
    "genitals": ["anus", "male_genital", "female_genital"],
    "female": ["breasts", "female_genital", "anus"],
    "male": ["male_genital", "anus"],
    "breasts": ["breasts"],
    "custom": PARTS,
}

I18N = {
    "zh": {
        "page_title": "Buchile 精细自动打码",
        "subtitle": "双模型轮廓分割 · 可选部位 · 低阈值补检 · 贴图/像素马赛克 · 多图导出",
        "creator_link": "作者 Pixiv 主页",
        "language": "界面语言 / Language",
        "settings": "打码设置",
        "preset": "部位预设",
        "preset_all": "全部敏感部位",
        "preset_genitals": "生殖器重点",
        "preset_female": "女性重点",
        "preset_male": "男性重点",
        "preset_breasts": "仅胸部",
        "preset_custom": "自定义",
        "selected_parts": "需要打码的部位",
        "selected_parts_help": "预设之后仍可手动增删。",
        "part_anus": "肛门",
        "part_fluids": "体液",
        "part_male_genital": "男性生殖器",
        "part_breasts": "胸部",
        "part_female_genital": "女性生殖器",
        "sensitivity": "检测灵敏度",
        "base_threshold": "常规识别阈值",
        "base_threshold_help": "越低越容易找到部位，也越可能误判。",
        "supplement_parts": "补检部位",
        "supplement_parts_help": "只对这些部位启用更低阈值，用于增添常规检测遗漏的区域。",
        "supplement_threshold": "补检阈值",
        "accuracy": "识别精度",
        "accuracy_help": "推理尺寸范围为 320–1920，按模型步长 32 调整。更高可能提升小目标细节，但会增加显存、内存和处理时间。",
        "detection_profiles": "识别阈值与精度",
        "detection_profiles_help": "设置常规阈值、补检参数和推理尺寸，再选择仅应用到当前图片或应用到全部图片。",
        "detection_profile_current": "当前图片正在使用独立识别参数。",
        "detection_profile_global": "当前图片正在使用全局识别参数。",
        "detection_apply_hint": "修改后请点击应用按钮；预览和批量导出仅使用已保存的识别参数。",
        "apply_current": "仅应用到当前图片",
        "apply_all_images": "应用到所有图片",
        "detection_applied_current": "已保存当前图片的独立识别参数。",
        "detection_applied_all": "已更新全部图片的识别参数，并清除旧的单图覆盖。",
        "mode": "打码方式",
        "mode_pixel": "普通马赛克",
        "mode_sticker": "贴图马赛克",
        "block_size": "马赛克强度（像素块大小）",
        "feather": "轮廓内侧柔化",
        "feather_help": "仅在蒙版内部柔化边缘，不会越出识别轮廓。",
        "custom_sticker": "自定义贴图（可选）",
        "custom_sticker_help": "选择自定义时上传贴图；若未上传则回退为大狗叫样例。透明区域会使用贴图自身颜色填充，不会混入普通马赛克。",
        "sticker_source": "贴图来源",
        "sticker_dagou": "大狗叫（大狗样例）",
        "sticker_maodie": "耄耋（猫咪样例）",
        "sticker_custom": "自行上传",
        "output_format": "导出格式",
        "model_license": "模型与许可",
        "model_description": "使用 Hachimi 与 Maodie 两套 YOLO 分割模型进行互补检测。",
        "license_description": "本工具按 GPL-3.0 发布，不提供任何担保；模型权利归各自作者所有。",
        "upload": "上传一张或多张图片",
        "upload_help": "可一次选择多张图片。单图区域选择只影响当前预览；批量导出按左侧部位预设自动处理全部图片。",
        "select_part_warning": "请至少选择一个需要打码的部位。",
        "preview_file": "当前预览图片",
        "detecting": "正在进行轮廓级检测…",
        "select_regions": "选择当前图片中需要遮挡的具体区域",
        "select_regions_help": "与 Hachimi 的部位选择相似，但这里每个实例都可单独开关。",
        "original": "原图",
        "preview": "精细打码预览",
        "show_region_numbers": "显示识别区域序号",
        "show_region_numbers_help": "红底白字序号仅显示在页面预览中，不会写入下载图片或批量导出结果。",
        "no_detection": "没有找到所选部位。可在左侧把对应部位加入“补检部位”，并降低补检阈值。",
        "download_preview": "下载当前预览",
        "batch_title": "批量处理与导出",
        "batch_caption": "批量模式会对每张图片重新执行精细检测，并自动遮挡左侧预设中的全部候选区域。",
        "process_all": "处理全部图片",
        "download_all": "下载全部结果（ZIP）",
        "batch_messages": "批量处理提示",
        "batch_prepare": "准备批量处理…",
        "batch_processing": "正在处理 {name}",
        "batch_done": "批量处理完成",
        "batch_not_found": "{name}：未找到所选部位，已原样导出",
        "batch_failed": "{name}：处理失败（{error}）",
    },
    "en": {
        "page_title": "Buchile Precision Auto Censor",
        "subtitle": "Dual-model contour segmentation · Selectable regions · Low-threshold recovery · Sticker/pixel mosaic · Batch export",
        "creator_link": "Creator on Pixiv",
        "language": "Language / 界面语言",
        "settings": "Censor Settings",
        "preset": "Region Preset",
        "preset_all": "All Sensitive Regions",
        "preset_genitals": "Genitals Focus",
        "preset_female": "Female Focus",
        "preset_male": "Male Focus",
        "preset_breasts": "Breasts Only",
        "preset_custom": "Custom",
        "selected_parts": "Regions to Censor",
        "selected_parts_help": "You can add or remove regions after choosing a preset.",
        "part_anus": "Anus",
        "part_fluids": "Fluids",
        "part_male_genital": "Male Genitalia",
        "part_breasts": "Breasts",
        "part_female_genital": "Female Genitalia",
        "sensitivity": "Detection Sensitivity",
        "base_threshold": "Standard Detection Threshold",
        "base_threshold_help": "Lower values find more regions but may add false positives.",
        "supplement_parts": "Recovery Regions",
        "supplement_parts_help": "Apply a lower threshold only to these regions to recover possible misses.",
        "supplement_threshold": "Recovery Threshold",
        "accuracy": "Detection Resolution",
        "accuracy_help": "Inference size ranges from 320 to 1920 in model-stride steps of 32. Higher values may preserve small details but use more memory and processing time.",
        "detection_profiles": "Detection Threshold and Resolution",
        "detection_profiles_help": "Set standard/recovery thresholds and inference size, then apply them to the current image or every image.",
        "detection_profile_current": "This image is using independent detection parameters.",
        "detection_profile_global": "This image is using the global detection parameters.",
        "detection_apply_hint": "Click an apply button after editing. Preview and batch export use saved detection parameters only.",
        "apply_current": "Apply to Current Image Only",
        "apply_all_images": "Apply to All Images",
        "detection_applied_current": "Saved independent detection parameters for the current image.",
        "detection_applied_all": "Updated detection parameters for all images and cleared previous per-image overrides.",
        "mode": "Censor Mode",
        "mode_pixel": "Pixel Mosaic",
        "mode_sticker": "Sticker Mosaic",
        "block_size": "Mosaic Strength (Pixel Block Size)",
        "feather": "Inner Contour Feathering",
        "feather_help": "Softens only inside the mask and never paints beyond its contour.",
        "custom_sticker": "Custom Sticker (Optional)",
        "custom_sticker_help": "Upload an image when Custom is selected. Without one, the Dog sample is used. Transparent margins are filled from the sticker itself and never mixed with pixel mosaic.",
        "sticker_source": "Sticker Source",
        "sticker_dagou": "Dog Sample",
        "sticker_maodie": "Cat Sample",
        "sticker_custom": "Custom Upload",
        "output_format": "Export Format",
        "model_license": "Models and License",
        "model_description": "Uses the Hachimi and Maodie YOLO segmentation models for complementary detection.",
        "license_description": "Released under GPL-3.0 without warranty. Model rights remain with their respective authors.",
        "upload": "Upload One or More Images",
        "upload_help": "Upload multiple images at once. Per-instance choices affect only the current preview; batch export uses the region preset for every image.",
        "select_part_warning": "Select at least one region to censor.",
        "preview_file": "Preview Image",
        "detecting": "Detecting precise contours…",
        "select_regions": "Select Specific Regions to Censor in This Image",
        "select_regions_help": "Similar to Hachimi region selection, with an independent switch for every detected instance.",
        "original": "Original",
        "preview": "Precision Censor Preview",
        "show_region_numbers": "Show Detected Region Numbers",
        "show_region_numbers_help": "Red numbered markers appear only in the page preview and are never written to downloaded or batch-exported images.",
        "no_detection": "No selected regions were found. Add the region under Recovery Regions and lower the recovery threshold.",
        "download_preview": "Download Current Preview",
        "batch_title": "Batch Processing and Export",
        "batch_caption": "Batch mode runs precise detection on every image and automatically censors all candidates in the selected preset.",
        "process_all": "Process All Images",
        "download_all": "Download All Results (ZIP)",
        "batch_messages": "Batch Processing Messages",
        "batch_prepare": "Preparing batch processing…",
        "batch_processing": "Processing {name}",
        "batch_done": "Batch processing complete",
        "batch_not_found": "{name}: no selected region found; exported unchanged",
        "batch_failed": "{name}: processing failed ({error})",
    },
}


language = st.session_state.get("language", "zh")


def tr(key: str, **values) -> str:
    return I18N[language][key].format(**values)


def part_label(part: str) -> str:
    return tr(f"part_{part}")


st.set_page_config(page_title=tr("page_title"), page_icon=Image.open(BRAND_ICON), layout="wide")
st.markdown(
    """
    <style>
      .block-container {max-width: 1240px; padding-top: 2rem;}
      [data-testid="stImage"] img {border-radius: 12px;}
    </style>
    """,
    unsafe_allow_html=True,
)


@st.cache_resource(show_spinner=False)
def get_models():
    return load_segmentation_models(MODEL_DIR)


@st.cache_data(show_spinner=False, max_entries=32)
def cached_decode(data: bytes) -> np.ndarray:
    return decode_image(data)


@st.cache_data(show_spinner=False, max_entries=64)
def run_detection(
    data: bytes,
    base_threshold: float,
    supplement_parts: tuple[str, ...],
    supplement_threshold: float,
    image_size: int,
):
    image = cached_decode(data)
    return detect_regions(
        image,
        get_models(),
        base_threshold=base_threshold,
        supplement_parts=supplement_parts,
        supplement_threshold=supplement_threshold,
        image_size=image_size,
    )


def safe_stem(filename: str) -> str:
    stem = Path(filename).stem.strip() or "image"
    return "".join(character if character.isalnum() or character in "-_." else "_" for character in stem)


def region_label(index, detection) -> str:
    return f"{index + 1}. {part_label(detection.part)}"


def upload_key(index: int, filename: str, data: bytes) -> str:
    identity = f"{index}:{filename}:".encode("utf-8") + data
    return hashlib.sha1(identity).hexdigest()[:20]


def detection_store() -> dict:
    if "detection_profiles" not in st.session_state:
        st.session_state["detection_profiles"] = {
            "global": dict(DEFAULT_DETECTION_PROFILE),
            "images": {},
        }
    store = st.session_state["detection_profiles"]
    store["global"].setdefault("supplement_parts", ())
    return store


def effective_detection_profile(image_id: str) -> dict:
    store = detection_store()
    profile = store["images"].get(image_id, store["global"])
    result = dict(profile)
    result["supplement_parts"] = tuple(result.get("supplement_parts", ()))
    return result


def detection_state_signature() -> tuple:
    store = detection_store()

    def value(profile: dict) -> tuple:
        return (
            float(profile["base_threshold"]),
            tuple(profile.get("supplement_parts", ())),
            float(profile["supplement_threshold"]),
            int(profile["image_size"]),
        )

    return (
        value(store["global"]),
        tuple((image_id, value(profile)) for image_id, profile in sorted(store["images"].items())),
    )


def build_zip(
    uploaded_files,
    selected_parts,
    effect_settings,
    sticker_rgba,
    output_format,
) -> tuple[bytes, list[str]]:
    buffer = BytesIO()
    warnings: list[str] = []
    with zipfile.ZipFile(buffer, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        progress = st.progress(0, text=tr("batch_prepare"))
        for index, uploaded in enumerate(uploaded_files):
            progress.progress(index / len(uploaded_files), text=tr("batch_processing", name=uploaded.name))
            try:
                data = uploaded.getvalue()
                image = cached_decode(data)
                image_id = upload_key(index, uploaded.name, data)
                detection_settings = effective_detection_profile(image_id)
                detections = run_detection(data, **detection_settings)
                chosen = detections_for_parts(detections, selected_parts)
                if not chosen:
                    warnings.append(tr("batch_not_found", name=uploaded.name))
                processed = apply_censor(
                    image,
                    chosen,
                    selected_ids=None,
                    sticker_rgba=sticker_rgba,
                    **effect_settings,
                )
                extension = "jpg" if output_format == "JPEG" else "png"
                filename = f"{index + 1:03d}_{safe_stem(uploaded.name)}_censored.{extension}"
                archive.writestr(filename, encode_image(processed, output_format))
            except Exception as error:
                warnings.append(tr("batch_failed", name=uploaded.name, error=error))
        progress.progress(1.0, text=tr("batch_done"))
    return buffer.getvalue(), warnings


brand_icon_column, brand_title_column = st.columns([0.07, 0.93], gap="small", vertical_alignment="center")
with brand_icon_column:
    st.image(BRAND_ICON, width=70)
with brand_title_column:
    st.title(tr("page_title"))
st.caption(tr("subtitle"))
st.markdown(f"{tr('creator_link')}: [Buchile]({PIXIV_URL})")

with st.sidebar:
    st.header(tr("settings"))
    preset = st.selectbox(
        tr("preset"),
        list(PRESETS),
        index=0,
        format_func=lambda value: tr(f"preset_{value}"),
    )
    selected_parts = st.multiselect(
        tr("selected_parts"),
        options=PARTS,
        default=PRESETS[preset],
        key=f"parts_{language}_{preset}",
        format_func=part_label,
        help=tr("selected_parts_help"),
    )

    st.divider()
    mode_key = st.radio(
        tr("mode"),
        ["pixel", "sticker"],
        horizontal=True,
        format_func=lambda value: tr(f"mode_{value}"),
    )
    mode = "贴图马赛克" if mode_key == "sticker" else "普通马赛克"
    block_size = 18
    if mode_key == "pixel":
        block_size = st.slider(
            tr("block_size"),
            min_value=2,
            max_value=64,
            value=18,
            step=1,
        )
    feather = 0
    if mode_key == "pixel":
        feather = st.slider(
            tr("feather"),
            min_value=0,
            max_value=5,
            value=1,
            help=tr("feather_help"),
        )
    sticker_file = None
    sticker_source = "dagou"
    if mode_key == "sticker":
        sticker_source = st.selectbox(
            tr("sticker_source"),
            ["dagou", "maodie", "custom"],
            format_func=lambda value: tr(f"sticker_{value}"),
        )
        if sticker_source == "custom":
            sticker_file = st.file_uploader(
                tr("custom_sticker"),
                type=["png", "jpg", "jpeg", "webp"],
                help=tr("custom_sticker_help"),
            )

    output_format = st.radio(tr("output_format"), ["PNG", "JPEG"], horizontal=True)
    with st.expander(tr("model_license")):
        st.write(tr("model_description"))
        st.caption(tr("license_description"))


uploaded_files = st.file_uploader(
    tr("upload"),
    type=["png", "jpg", "jpeg", "webp", "bmp", "tif", "tiff"],
    accept_multiple_files=True,
    help=tr("upload_help"),
)

st.radio(
    tr("language"),
    options=["zh", "en"],
    format_func=lambda value: "中文" if value == "zh" else "English",
    horizontal=True,
    key="language",
)

if not uploaded_files:
    st.stop()

if not selected_parts:
    st.warning(tr("select_part_warning"))
    st.stop()

preview_index = st.selectbox(
    tr("preview_file"),
    options=list(range(len(uploaded_files))),
    format_func=lambda index: f"{index + 1}. {uploaded_files[index].name}",
)
current_file = uploaded_files[preview_index]
current_data = current_file.getvalue()
current_image = cached_decode(current_data)
current_image_id = upload_key(preview_index, current_file.name, current_data)

with st.expander(tr("detection_profiles"), expanded=True):
    st.caption(tr("detection_profiles_help"))
    detection_message = st.session_state.pop("detection_profile_message", None)
    if detection_message:
        st.success(detection_message)
    detection_source = effective_detection_profile(current_image_id)
    has_detection_override = current_image_id in detection_store()["images"]
    st.caption(
        tr("detection_profile_current")
        if has_detection_override
        else tr("detection_profile_global")
    )
    detection_revision = st.session_state.get("detection_editor_revision", 0)
    detection_key = f"{language}_{current_image_id}_{detection_revision}"
    draft_base_threshold = st.slider(
        tr("base_threshold"),
        min_value=0.15,
        max_value=0.80,
        value=float(detection_source["base_threshold"]),
        step=0.01,
        help=tr("base_threshold_help"),
        key=f"detection_base_{detection_key}",
    )
    draft_supplement_parts = tuple(
        st.multiselect(
            tr("supplement_parts"),
            options=PARTS,
            default=list(detection_source.get("supplement_parts", ())),
            format_func=part_label,
            help=tr("supplement_parts_help"),
            key=f"detection_parts_{detection_key}",
        )
    )
    draft_supplement_threshold = st.slider(
        tr("supplement_threshold"),
        min_value=0.03,
        max_value=0.35,
        value=float(detection_source["supplement_threshold"]),
        step=0.01,
        disabled=not draft_supplement_parts,
        key=f"detection_supplement_{detection_key}",
    )
    draft_image_size = st.slider(
        tr("accuracy"),
        min_value=320,
        max_value=1920,
        value=int(detection_source["image_size"]),
        step=32,
        help=tr("accuracy_help"),
        key=f"detection_size_{detection_key}",
    )
    st.caption(tr("detection_apply_hint"))
    draft_detection_profile = {
        "base_threshold": float(draft_base_threshold),
        "supplement_parts": draft_supplement_parts,
        "supplement_threshold": float(draft_supplement_threshold),
        "image_size": int(draft_image_size),
    }
    detection_current_column, detection_all_column = st.columns(2)
    with detection_current_column:
        apply_detection_current = st.button(
            tr("apply_current"),
            key="detection_apply_current",
            use_container_width=True,
        )
    with detection_all_column:
        apply_detection_all = st.button(
            tr("apply_all_images"),
            key="detection_apply_all",
            type="primary",
            use_container_width=True,
        )
    if apply_detection_current:
        detection_store()["images"][current_image_id] = dict(draft_detection_profile)
        st.session_state["detection_editor_revision"] = detection_revision + 1
        st.session_state["detection_profile_message"] = tr("detection_applied_current")
        st.session_state.pop("batch_result", None)
        st.rerun()
    if apply_detection_all:
        store = detection_store()
        store["global"] = dict(draft_detection_profile)
        store["images"].clear()
        st.session_state["detection_editor_revision"] = detection_revision + 1
        st.session_state["detection_profile_message"] = tr("detection_applied_all")
        st.session_state.pop("batch_result", None)
        st.rerun()

detection_settings = effective_detection_profile(current_image_id)
effect_settings = {
    "mode": mode,
    "block_size": int(block_size),
    "feather": int(feather),
}

sticker_bytes = sticker_file.getvalue() if sticker_file is not None else None
sticker_path = STICKER_SAMPLES.get(sticker_source, STICKER_SAMPLES["dagou"])
sticker_rgba = decode_sticker(sticker_bytes, sticker_path) if mode_key == "sticker" else None

with st.spinner(tr("detecting")):
    detections = run_detection(current_data, **detection_settings)

candidate_detections = detections_for_parts(detections, selected_parts)
label_to_id = {region_label(index, item): item.uid for index, item in enumerate(candidate_detections)}
region_labels = list(label_to_id)
preview_key = hashlib.sha1(
    current_data
    + repr((selected_parts, detection_settings)).encode("utf-8")
).hexdigest()[:12]
selected_region_labels = st.multiselect(
    tr("select_regions"),
    options=region_labels,
    default=region_labels,
    key=f"regions_{language}_{preview_key}",
    help=tr("select_regions_help"),
)
selected_ids = {label_to_id[label] for label in selected_region_labels}

processed_preview = apply_censor(
    current_image,
    candidate_detections,
    selected_ids=selected_ids,
    sticker_rgba=sticker_rgba,
    **effect_settings,
)

show_region_numbers = st.toggle(
    tr("show_region_numbers"),
    value=True,
    help=tr("show_region_numbers_help"),
)
display_original = (
    draw_detection_markers(current_image, candidate_detections)
    if show_region_numbers
    else current_image
)
display_preview = (
    draw_detection_markers(processed_preview, candidate_detections)
    if show_region_numbers
    else processed_preview
)

left, right = st.columns(2, gap="medium")
with left:
    st.image(display_original, caption=tr("original"), use_container_width=True)
with right:
    st.image(display_preview, caption=tr("preview"), use_container_width=True)

if not candidate_detections:
    st.warning(tr("no_detection"))

extension = "jpg" if output_format == "JPEG" else "png"
preview_bytes = encode_image(processed_preview, output_format)
st.download_button(
    tr("download_preview"),
    data=preview_bytes,
    file_name=f"{safe_stem(current_file.name)}_censored.{extension}",
    mime=f"image/{'jpeg' if output_format == 'JPEG' else 'png'}",
    use_container_width=True,
)

st.divider()
st.subheader(tr("batch_title"))
st.caption(tr("batch_caption"))

batch_signature = hashlib.sha1(
    repr(
        (
            [(item.name, len(item.getvalue())) for item in uploaded_files],
            selected_parts,
            detection_state_signature(),
            effect_settings,
            output_format,
            sticker_source,
            hashlib.sha1(sticker_bytes or b"").hexdigest(),
        )
    ).encode("utf-8")
).hexdigest()

if st.button(tr("process_all"), type="primary", use_container_width=True):
    zip_bytes, batch_warnings = build_zip(
        uploaded_files,
        selected_parts,
        effect_settings,
        sticker_rgba,
        output_format,
    )
    st.session_state["batch_result"] = (batch_signature, zip_bytes, batch_warnings)

batch_result = st.session_state.get("batch_result")
if batch_result and batch_result[0] == batch_signature:
    _, zip_bytes, batch_warnings = batch_result
    st.download_button(
        tr("download_all"),
        data=zip_bytes,
        file_name="buchile_censored_images.zip",
        mime="application/zip",
        use_container_width=True,
    )
    if batch_warnings:
        with st.expander(f"{tr('batch_messages')} ({len(batch_warnings)})"):
            for warning in batch_warnings:
                st.write(f"- {warning}")
