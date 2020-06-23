package org.clas.cross;

public class Constants {
    // FMT Geometry
    private static final double innerRadius = 25;
    private static final double outerRadius = 225;
    private static final int numberOfFMTLayers = 3;
    private static final int numberOfFMTRegions = 4;
    private static final int numberOfDCSectors = 6;
    private static final int[] FMTRegionSeparators = new int[]{-1, 319, 511, 831, 1023};

    public Constants() {
    }

    public static double getInnerRadius() {
        return innerRadius;
    }

    public static double getOuterRadius() {
        return outerRadius;
    }

    public static int getFirstStripNumber() {
        return FMTRegionSeparators[0]+1;
    }

    public static int getLastStripNumber() {
        return FMTRegionSeparators[4];
    }

    public static int getNumberOfFMTLayers() {
        return numberOfFMTLayers;
    }

    public static int getNumberOfFMTRegions() {
        return numberOfFMTRegions;
    }

    public static int getNumberOfDCSectors() {
        return numberOfDCSectors;
    }

    public static int getFMTRegionSeparators(int idx) {
        return FMTRegionSeparators[idx];
    }
}
