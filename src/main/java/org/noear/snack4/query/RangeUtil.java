package org.noear.snack4.query;

/**
 * @author noear 2025/5/5 created
 */
public class RangeUtil {
    public static int normalize(int val, int len) {
        if (val >= 0) {
            return val;
        } else {
            return len + val;
        }
    }

    public static Bounds bounds(int start, int end, int step, int len) {
        int n_start = start;//normalize(start, len);
        int n_end = end;//normalize(end, len);

        int lower, upper;
        if (step >= 0) {
            lower = Math.min(Math.max(n_start, 0), len);
            upper = Math.min(Math.max(n_end, 0), len);
        } else {
            upper = Math.min(Math.max(n_start, -1), len - 1);
            lower = Math.min(Math.max(n_end, -1), len - 1);
        }

        return new Bounds(lower, upper);
    }

    public static class Bounds {
        final int lower, upper;

        public Bounds(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
        }

        public int getLower() {
            return lower;
        }

        public int getUpper() {
            return upper;
        }
    }
}