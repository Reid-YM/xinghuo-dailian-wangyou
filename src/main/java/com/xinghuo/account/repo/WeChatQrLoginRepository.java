package com.xinghuo.account.repo;

import com.xinghuo.account.model.WeChatQrLogin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeChatQrLoginRepository extends JpaRepository<WeChatQrLogin, String> {
}
