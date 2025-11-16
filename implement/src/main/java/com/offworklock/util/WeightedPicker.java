package com.offworklock.util;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * Utility for selecting an entry from a weighted list.
 */
public final class WeightedPicker {

    private final Random random;

    public WeightedPicker() {
        this(new SecureRandom());
    }

    public WeightedPicker(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public <T> T pick(List<T> entries, ToDoubleFunction<T> weightFunction) {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(weightFunction, "weightFunction");
        double total = 0D;
        for (T entry : entries) {
            double weight = Math.max(0D, weightFunction.applyAsDouble(entry));
            total += weight;
        }
        if (total <= 0D || entries.isEmpty()) {
            return null;
        }
        double target = random.nextDouble() * total;
        double cumulative = 0D;
        for (T entry : entries) {
            cumulative += Math.max(0D, weightFunction.applyAsDouble(entry));
            if (cumulative >= target) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }
}
