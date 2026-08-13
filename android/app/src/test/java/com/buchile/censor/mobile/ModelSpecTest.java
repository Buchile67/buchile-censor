package com.buchile.censor.mobile;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModelSpecTest {
    @Test
    public void hachimiClassMapMatchesBundledModel() {
        Part[] classes = ModelSpec.hachimi().classes;
        assertEquals(Part.BREASTS, classes[0]);
        assertEquals(Part.ANUS, classes[1]);
        assertEquals(Part.FEMALE, classes[2]);
        assertEquals(Part.MALE, classes[3]);
    }

    @Test
    public void maodieClassMapMatchesBundledModel() {
        Part[] classes = ModelSpec.maodie().classes;
        assertEquals(Part.ANUS, classes[0]);
        assertEquals(Part.FLUIDS, classes[1]);
        assertEquals(Part.MALE, classes[2]);
        assertEquals(Part.BREASTS, classes[3]);
        assertEquals(Part.FEMALE, classes[4]);
    }
}
