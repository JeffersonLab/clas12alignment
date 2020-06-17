package org.clas.cross;

public class Constants {
    // FMT Geometry
    private static final double innerRadius = 25;
    private static final double outerRadius = 225;
    private static final int firstStripNumber = 0;
    private static final int lastStripNumber = 1023;
    //
    private static final int numberOfFMTLayers = 3; // Number of FMT layers.
    private static final int numberOfFMTRegions = 4; // Number of FMT regions.
    private static final int numberOfDCSectors = 6; // Number of DC sectors.
    private static final int[] FMTRegionSeparators = new int[]{-1, 319, 511, 831, 1023}; // FMT region separators.

    public Constants() {
    }

    public static double getInnerRadius() {
        return innerRadius;
    }

    public static double getOuterRadius() {
        return outerRadius;
    }

    public static int getFirstStripNumber() {
        return firstStripNumber;
    }

    public static int getLastStripNumber() {
        return lastStripNumber;
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
