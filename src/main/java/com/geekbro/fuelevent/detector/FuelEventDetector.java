package com.geekbro.fuelevent.detector;

import com.geekbro.fuelevent.config.FuelEventDetectionProperties;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class FuelEventDetector {

    private final FuelEventDetectionProperties properties;

    public List<Double> detectEvents(List<Double> readings, double threshold) {
        if (readings == null || readings.size() < 2) {
            return new ArrayList<>();
        }

        double[] s = readings.stream().mapToDouble(Double::doubleValue).toArray();
        int n = s.length;

        List<int[]> groups = groupCandidateJumps(s, threshold);

        List<Double> events = new ArrayList<>();
        for (int[] group : groups) {
            int beforeIdx = group[0];
            int afterIdx = group[1] + 1;
            if (afterIdx >= n) continue;

            double beforeVal = s[beforeIdx];
            double direction = Math.signum(s[afterIdx] - beforeVal);

            int settledIdx = extendThroughRamp(s, afterIdx, direction, threshold);
            double afterVal = s[settledIdx];

            if (revertsToBaseline(s, settledIdx, beforeVal, threshold)) {
                continue;
            }

            double delta = afterVal - beforeVal;
            if (Math.abs(delta) > threshold) {
                events.add(round1(delta));
            }
        }
        return events;
    }

    private int extendThroughRamp(double[] s, int fromIdx, double direction, double threshold) {
        int n = s.length;
        int idx = fromIdx;
        int extended = 0;
        while (idx + 1 < n && extended < properties.getMaxRampExtension()) {
            double step = s[idx + 1] - s[idx];
            boolean sameDirectionOrFlat = (direction >= 0 && step >= 0) || (direction < 0 && step <= 0);
            if (sameDirectionOrFlat && Math.abs(step) <= threshold * 3) {
                idx++;
                extended++;
            } else {
                break;
            }
        }
        return idx;
    }

    private List<int[]> groupCandidateJumps(double[] s, double threshold) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < s.length; i++) {
            if (Math.abs(s[i] - s[i - 1]) > threshold) {
                candidates.add(i - 1);
            }
        }

        List<int[]> groups = new ArrayList<>();
        if (candidates.isEmpty()) return groups;

        int start = candidates.get(0);
        int prev = candidates.get(0);
        for (int i = 1; i < candidates.size(); i++) {
            int c = candidates.get(i);
            if (c - prev <= properties.getGroupGap()) {
                prev = c;
            } else {
                groups.add(new int[]{start, prev});
                start = c;
                prev = c;
            }
        }
        groups.add(new int[]{start, prev});
        return groups;
    }

    private boolean revertsToBaseline(double[] s, int fromIdx, double beforeVal, double threshold) {
        int end = Math.min(s.length - 1, fromIdx + properties.getHoldSamples());
        for (int i = fromIdx + 1; i <= end; i++) {
            if (Math.abs(s[i] - beforeVal) <= threshold) {
                return true;
            }
        }
        return false;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}