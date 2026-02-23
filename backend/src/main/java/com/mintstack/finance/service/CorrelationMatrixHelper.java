package com.mintstack.finance.service;

import java.util.List;
import java.util.Map;
import java.util.Random;

final class CorrelationMatrixHelper {

    private CorrelationMatrixHelper() {
    }

    static double[][] choleskyDecomposition(double[][] matrix) {
        int n = matrix.length;
        double[][] l = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0;
                for (int k = 0; k < j; k++) {
                    sum += l[i][k] * l[j][k];
                }

                if (i == j) {
                    double val = matrix[i][i] - sum;
                    l[i][j] = val > 0 ? Math.sqrt(val) : 0;
                } else {
                    l[i][j] = l[j][j] > 0 ? (matrix[i][j] - sum) / l[j][j] : 0;
                }
            }
        }

        return l;
    }

    static double[][] calculateCorrelationMatrix(List<String> symbols, Map<String, String> sectorMap) {
        int n = symbols.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 1.0;
                } else {
                    String sector1 = sectorMap.getOrDefault(symbols.get(i), "OTHER");
                    String sector2 = sectorMap.getOrDefault(symbols.get(j), "OTHER");
                    matrix[i][j] = sector1.equals(sector2) ? 0.7 : 0.3;
                }
            }
        }

        return matrix;
    }

    static double[] generateCorrelatedRandoms(double[][] choleskyL, Random random) {
        int n = choleskyL.length;
        double[] uncorrelated = new double[n];
        double[] correlated = new double[n];

        for (int i = 0; i < n; i++) {
            uncorrelated[i] = random.nextGaussian();
        }

        for (int i = 0; i < n; i++) {
            correlated[i] = 0;
            for (int j = 0; j <= i; j++) {
                correlated[i] += choleskyL[i][j] * uncorrelated[j];
            }
        }

        return correlated;
    }
}
