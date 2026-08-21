package com.tien.aivirabackend.service.ai;

import java.util.ArrayList;
import java.util.List;

final class EmbeddingVectors {
    private EmbeddingVectors() {}

    static List<Double> normalize(List<Double> values, int expectedDimension) {
        if (values == null || values.size() != expectedDimension)
            throw new IllegalStateException("Embedding dimension mismatch");
        double norm = 0;
        for (Double value : values) {
            if (value == null || !Double.isFinite(value)) throw new IllegalStateException("Invalid embedding value");
            norm += value * value;
        }
        if (norm == 0) throw new IllegalStateException("Empty embedding vector");
        double divisor = Math.sqrt(norm);
        List<Double> result = new ArrayList<>(values.size());
        for (double value : values) result.add(value / divisor);
        return List.copyOf(result);
    }
}

