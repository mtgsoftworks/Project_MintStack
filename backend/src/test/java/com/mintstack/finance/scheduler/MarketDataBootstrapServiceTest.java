package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.repository.InstrumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataBootstrapService Unit Tests")
class MarketDataBootstrapServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private MarketDataProviderResolver providerResolver;

    @InjectMocks
    private MarketDataBootstrapService bootstrapService;

    @Test
    @DisplayName("bootstrapCryptoInstruments should persist new entities without preset id")
    void bootstrapCryptoInstruments_ShouldPersistWithoutPresetId() {
        when(instrumentRepository.findBySymbolAndIsSimulated(anyString(), eq(false)))
            .thenReturn(Optional.empty());
        when(providerResolver.fetchCryptoPrice(anyString(), any()))
            .thenReturn(new BigDecimal("100.00"));
        when(instrumentRepository.save(any(Instrument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        bootstrapService.bootstrapCryptoInstruments(new UserApiConfig());

        ArgumentCaptor<Instrument> captor = ArgumentCaptor.forClass(Instrument.class);
        verify(instrumentRepository, times(5)).save(captor.capture());

        assertThat(captor.getAllValues())
            .isNotEmpty()
            .allSatisfy(instrument -> assertThat(instrument.getId()).isNull());
    }
}
