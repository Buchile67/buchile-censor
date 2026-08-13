package com.buchile.censor.mobile;

enum Part {
    ANUS("肛门", "Anus"),
    FLUIDS("体液", "Fluids"),
    MALE("男性生殖器", "Male genital region"),
    BREASTS("胸部", "Chest region"),
    FEMALE("女性生殖器", "Female genital region");

    final String zh;
    final String en;

    Part(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    String label(boolean english) {
        return english ? en : zh;
    }
}

