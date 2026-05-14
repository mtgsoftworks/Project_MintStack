package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.UpdateRuntimeSettingRequest;
import com.mintstack.finance.dto.response.RuntimeSettingResponse;
import com.mintstack.finance.entity.RuntimeSetting;
import com.mintstack.finance.repository.RuntimeSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuntimeSettingsService {

    private final RuntimeSettingRepository runtimeSettingRepository;

    @Transactional(readOnly = true)
    public List<RuntimeSettingResponse> getAll() {
        return runtimeSettingRepository.findAll().stream()
            .sorted(Comparator.comparing(RuntimeSetting::getKey))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public RuntimeSettingResponse update(String key, UpdateRuntimeSettingRequest request, String updatedBy) {
        RuntimeSetting setting = runtimeSettingRepository.findByKey(key)
            .orElseGet(() -> RuntimeSetting.builder().key(key).build());
        setting.setValue(request.getValue());
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }
        if (request.getRestartRequired() != null) {
            setting.setRestartRequired(request.getRestartRequired());
        }
        setting.setUpdatedBy(updatedBy);
        return toResponse(runtimeSettingRepository.save(setting));
    }

    private RuntimeSettingResponse toResponse(RuntimeSetting setting) {
        return RuntimeSettingResponse.builder()
            .id(setting.getId())
            .key(setting.getKey())
            .value(setting.getValue())
            .description(setting.getDescription())
            .restartRequired(setting.getRestartRequired())
            .updatedBy(setting.getUpdatedBy())
            .updatedAt(setting.getUpdatedAt())
            .build();
    }
}
