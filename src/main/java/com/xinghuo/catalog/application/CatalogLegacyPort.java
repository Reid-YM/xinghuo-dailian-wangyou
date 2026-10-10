package com.xinghuo.catalog.application;

import java.util.List;

/** Temporary migration port for the public service catalog. */
public interface CatalogLegacyPort {
    List<CatalogServiceView> publicList();
}
