package com.mintstack.finance.service;

import java.util.Arrays;
import java.util.Random;

final class MonteCarloStatisticsHelper {

    private MonteCarloStatisticsHelper() {
    }

    static double[] generateDefaultReturns() {
        Random random = new Random();
        double[] returns = new double[252];
        double dailyMean = 0.08 / 252;
        double dailyVol = 0.25 / Math.sqrt(252);

        for (int i = 0; i < 252; i++) {
            returns[i] = dailyMean + dailyVol * random.nextGaussian();
        }

        return returns;
    }

    static double calculateMean(double[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    static double calculateStdDev(double[] values) {
        double mean = calculateMean(values);
        double sumSquaredDiff = 0;
        for (double value : values) {
            sumSquaredDiff += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sumSquaredDiff / values.length);
    }

    static double[] createHistogram(double[] values, int bins) {
        double min = values[0];
        double max = values[values.length - 1];
        double binWidth = (max - min) / bins;

        double[] histogram = new double[bins];

        for (double value : values) {
            int binIndex = (int) ((value - min) / binWidth);
            if (binIndex >= bins) {
                binIndex = bins - 1;
            }
            if (binIndex < 0) {
                binIndex = 0;
            }
            histogram[binIndex]++;
        }

        for (int i = 0; i < bins; i++) {
            histogram[i] = (histogram[i] / values.length) * 100;
        }

        return histogram;
    }
}
