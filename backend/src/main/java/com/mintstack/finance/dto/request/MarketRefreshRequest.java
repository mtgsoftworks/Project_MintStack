package com.mintstack.finance.dto.request;

import com.mintstack.finance.entity.UserDataPreference.DataType;

import java.util.List;

public record MarketRefreshRequest(
    List<DataType> dataTypes
) {
}
