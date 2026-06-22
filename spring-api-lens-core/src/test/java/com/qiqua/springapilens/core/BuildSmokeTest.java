package com.qiqua.springapilens.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuildSmokeTest {
    @Test
    void exposesProjectName() {
        assertThat(ProjectInfo.name()).isEqualTo("springApiLens");
    }
}
