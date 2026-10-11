package com.xinghuo.upload.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadAssetRepository extends JpaRepository<UploadAsset, UUID> {
}
