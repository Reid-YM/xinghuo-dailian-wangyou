package com.xinghuo.service;

import com.xinghuo.model.AdminUser;
import com.xinghuo.repository.AdminUserRepository;
import com.xinghuo.security.AdminLoginPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService implements UserDetailsService {
    private final AdminUserRepository repo;

    public AdminUserService(AdminUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser user = this.repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return new AdminLoginPrincipal(user.getUsername(),user.getPasswordHash(),user.getRole().name(),user.isEnabled(),user.getAuthVersion());
    }

    public boolean sessionCurrent(Authentication authentication) {
        if(authentication==null||!authentication.isAuthenticated())return false;
        return repo.findByUsername(authentication.getName()).filter(user->user.isEnabled()
            && authentication.getAuthorities().stream().anyMatch(authority->("ROLE_"+user.getRole().name()).equals(authority.getAuthority()))
            && user.getAuthVersion()==(authentication.getPrincipal() instanceof AdminLoginPrincipal principal?principal.authVersion():0)).isPresent();
    }
}
