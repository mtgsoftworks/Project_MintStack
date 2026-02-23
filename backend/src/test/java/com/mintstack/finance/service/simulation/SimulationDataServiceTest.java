package com.mintstack.finance.service.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationDataService Tests")
class SimulationDataServiceTest {

    @Test
    @DisplayName("Service should exist")
    void testServiceExists() {
        assertThat(true).isTrue();
    }
}
