package com.xinghuo.catalog.application;

import com.xinghuo.dto.ApiDtos;
import java.util.List;

/** Temporary migration port for the public service catalog. */
public interface CatalogLegacyPort {
    List<ApiDtos.ServiceResponse> publicList();
}
