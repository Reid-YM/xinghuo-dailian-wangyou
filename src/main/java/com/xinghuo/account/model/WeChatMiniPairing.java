package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wechat_mini_pairing")
@Getter
@Setter
public class WeChatMiniPairing {
    @Id
    @Column(length = 43)
    private String id;
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;
    @Column(name = "browser_hash", nullable = false, length = 64)
    private String browserHash;
    @Column(name = "target_user_id")
    private Long targetUserId;
    @Column(name = "mini_open_id", length = 128)
    private String miniOpenId;
    @Column(name = "mini_union_id", length = 128)
    private String miniUnionId;
    @Column(name = "approved_user_id")
    private Long approvedUserId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
}
