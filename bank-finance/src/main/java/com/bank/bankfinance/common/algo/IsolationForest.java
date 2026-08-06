package com.bank.bankfinance.common.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 轻量级孤立森林（Isolation Forest）无监督异常检测。
 * 不依赖任何第三方 ML 库，适用于交易行为异常识别。
 */
public class IsolationForest {

    private static final double EULER = 0.5772156649015328606;

    private final int numTrees;
    private final int n;
    private final int dim;
    private final double[] mean;
    private final double[] std;
    private final ITree[] trees;

    public IsolationForest(double[][] samples, int numTrees, int sampleSize) {
        this.numTrees = numTrees;
        this.n = samples.length;
        this.dim = samples[0].length;
        this.mean = new double[dim];
        this.std = new double[dim];

        for (int j = 0; j < dim; j++) {
            double m = 0;
            for (double[] s : samples) m += s[j];
            m /= n;
            double v = 0;
            for (double[] s : samples) v += (s[j] - m) * (s[j] - m);
            v = Math.sqrt(v / n);
            mean[j] = m;
            std[j] = (v < 1e-9) ? 1.0 : v;
        }

        double[][] norm = new double[n][dim];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < dim; j++) {
                norm[i][j] = (samples[i][j] - mean[j]) / std[j];
            }
        }

        trees = new ITree[numTrees];
        Random rnd = new Random();
        int ss = Math.max(2, Math.min(sampleSize, n));
        for (int t = 0; t < numTrees; t++) {
            trees[t] = buildTree(subsample(norm, ss, rnd), 0, rnd);
        }
    }

    /**
     * 计算样本异常分数，范围 (0,1]。越接近 1 越异常。
     */
    public double score(double[] x) {
        double[] nx = new double[dim];
        for (int j = 0; j < dim; j++) {
            nx[j] = (x[j] - mean[j]) / std[j];
        }
        double avgPath = 0;
        for (ITree tree : trees) {
            avgPath += tree.pathLength(nx);
        }
        avgPath /= numTrees;
        double c = averagePathConstant(n);
        return Math.pow(2.0, -avgPath / c);
    }

    private double[][] subsample(double[][] data, int size, Random rnd) {
        double[][] out = new double[size][dim];
        for (int i = 0; i < size; i++) {
            out[i] = data[rnd.nextInt(n)];
        }
        return out;
    }

    private ITree buildTree(double[][] data, int depth, Random rnd) {
        int size = data.length;
        if (depth >= maxDepth() || size <= 1 || allSame(data)) {
            return new Leaf(size);
        }
        int f = rnd.nextInt(dim);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double[] row : data) {
            if (row[f] < min) min = row[f];
            if (row[f] > max) max = row[f];
        }
        if (min == max) return new Leaf(size);

        double split = min + rnd.nextDouble() * (max - min);
        List<double[]> left = new ArrayList<>();
        List<double[]> right = new ArrayList<>();
        for (double[] row : data) {
            if (row[f] < split) left.add(row); else right.add(row);
        }
        if (left.isEmpty() || right.isEmpty()) return new Leaf(size);

        double[][] lArr = left.toArray(new double[0][dim]);
        double[][] rArr = right.toArray(new double[0][dim]);
        return new Node(f, split, buildTree(lArr, depth + 1, rnd), buildTree(rArr, depth + 1, rnd));
    }

    private boolean allSame(double[][] data) {
        for (int j = 0; j < dim; j++) {
            double v = data[0][j];
            for (double[] row : data) {
                if (row[j] != v) return false;
            }
        }
        return true;
    }

    private int maxDepth() {
        return (int) Math.ceil(Math.log(Math.max(2, n)) / Math.log(2)) + 1;
    }

    public static double averagePathConstant(int size) {
        if (size <= 1) return 0;
        if (size == 2) return 1;
        double h = Math.log(size - 1) + EULER;
        return 2.0 * h - 2.0 * (size - 1) / size;
    }

    public abstract static class ITree {
        public abstract double pathLength(double[] x);
    }

    public static class Node extends ITree {
        private final int feature;
        private final double split;
        private final ITree left;
        private final ITree right;

        public Node(int feature, double split, ITree left, ITree right) {
            this.feature = feature;
            this.split = split;
            this.left = left;
            this.right = right;
        }

        @Override
        public double pathLength(double[] x) {
            return 1 + (x[feature] < split ? left.pathLength(x) : right.pathLength(x));
        }
    }

    public static class Leaf extends ITree {
        private final int size;

        public Leaf(int size) {
            this.size = size;
        }

        @Override
        public double pathLength(double[] x) {
            return averagePathConstant(size);
        }
    }
}
