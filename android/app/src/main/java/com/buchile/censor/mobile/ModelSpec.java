package com.buchile.censor.mobile;

final class ModelSpec {
    final String assetName;
    final Part[] classes;

    ModelSpec(String assetName, Part[] classes) {
        this.assetName = assetName;
        this.classes = classes;
    }

    static ModelSpec hachimi() {
        return new ModelSpec(
                "models/hachimi_segmentation.onnx",
                new Part[]{Part.BREASTS, Part.ANUS, Part.FEMALE, Part.MALE}
        );
    }

    static ModelSpec maodie() {
        return new ModelSpec(
                "models/maodie_segmentation.onnx",
                new Part[]{Part.ANUS, Part.FLUIDS, Part.MALE, Part.BREASTS, Part.FEMALE}
        );
    }
}

