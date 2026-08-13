"""Smoke-test the two exported Android ONNX models with safe bundled images."""

from __future__ import annotations

from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"


def letterbox(path: Path) -> np.ndarray:
    with Image.open(path) as image:
        rgb = image.convert("RGB")
        scale = min(640 / rgb.width, 640 / rgb.height)
        resized = rgb.resize((round(rgb.width * scale), round(rgb.height * scale)), Image.Resampling.BILINEAR)
    canvas = Image.new("RGB", (640, 640), (114, 114, 114))
    canvas.paste(resized, ((640 - resized.width) // 2, (640 - resized.height) // 2))
    array = np.asarray(canvas, dtype=np.float32) / 255.0
    return np.transpose(array, (2, 0, 1))[None]


def validate(model_name: str, class_count: int) -> None:
    model_path = ASSETS / "models" / model_name
    onnx.checker.check_model(onnx.load(model_path))
    session = ort.InferenceSession(model_path, providers=["CPUExecutionProvider"])
    input_name = session.get_inputs()[0].name
    for image_name in ("dagou.png", "maodie.png"):
        prediction, prototypes = session.run(None, {input_name: letterbox(ASSETS / image_name)})
        assert prediction.shape == (1, 4 + class_count + 32, 8400), prediction.shape
        assert prototypes.shape == (1, 32, 160, 160), prototypes.shape
        assert np.isfinite(prediction).all()
        assert np.isfinite(prototypes).all()
    print(f"OK: {model_name}")


if __name__ == "__main__":
    validate("hachimi_segmentation.onnx", 4)
    validate("maodie_segmentation.onnx", 5)

