package com.freepoint.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PointKeyGeneratorTest {

    @Test
    void UUID_v7_형식의_36자_키를_생성한다() {
        String key = PointKeyGenerator.generate();

        assertThat(key).hasSize(36);
        assertThat(UUID.fromString(key).version()).isEqualTo(7);
    }

    @Test
    void 나중에_생성한_키가_문자열_정렬상_뒤에_온다() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            keys.add(PointKeyGenerator.generate());
        }

        assertThat(keys).isSorted();
        assertThat(new HashSet<>(keys)).hasSize(keys.size());
    }
}
