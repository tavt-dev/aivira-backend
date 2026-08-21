package com.tien.aivirabackend.service.ai;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class EmbeddingVectorsTest {
    @Test
    void normalize_shouldReturnUnitVector() {
        List<Double> vector = EmbeddingVectors.normalize(List.of(3d, 4d), 2);
        assertThat(vector).containsExactly(0.6d, 0.8d);
    }

    @Test
    void normalize_shouldRejectWrongDimensionAndInvalidValues() {
        assertThatThrownBy(() -> EmbeddingVectors.normalize(List.of(1d), 2))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> EmbeddingVectors.normalize(List.of(Double.NaN), 1))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> EmbeddingVectors.normalize(List.of(0d, 0d), 2))
                .isInstanceOf(IllegalStateException.class);
    }
}

