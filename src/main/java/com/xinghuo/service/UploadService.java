package com.xinghuo.service;

import com.xinghuo.repository.UploadAssetRepository;
import org.springframework.stereotype.Service;

/** @deprecated compatibility bridge; use com.xinghuo.upload.infrastructure.UploadService. */
@Deprecated(forRemoval = true)
@Service
public class UploadService extends com.xinghuo.upload.infrastructure.UploadService {
    public UploadService(UploadAssetRepository repo) {
        super(repo);
    }
}
