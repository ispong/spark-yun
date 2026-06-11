package com.isxcode.spark.security.main;

import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.security.authorization.AccessSnapshot;
import com.isxcode.spark.security.authorization.ProductAccessService;
import com.isxcode.spark.security.user.UserEntity;
import com.isxcode.spark.security.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    private final ProductAccessService productAccessService;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        return loadUserByUsername(userId, null);
    }

    public UserDetails loadUserByUsername(String userId, String tenantId) throws UsernameNotFoundException {

        // 返回匿名者用户对象
        if ("sy_anonymous".equals(userId)) {
            return User.withUsername(userId).password("")
                .authorities(AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ANONYMOUS")).build();
        }

        UserEntity userInfo = userRepository.findById(userId).orElseThrow(() -> new IsxAppException("用户不存在"));
        AccessSnapshot access = productAccessService.resolve(userId, tenantId);
        List<String> authorities = new ArrayList<>();
        authorities.add(userInfo.getRoleCode());
        if (access.platformAdmin() && !access.systemAdmin()) {
            authorities.add(RoleType.PLATFORM_ADMIN);
        }
        if (access.tenantId() != null) {
            authorities.add(RoleType.TENANT_MEMBER);
        }
        if (access.tenantAdmin()) {
            authorities.add(RoleType.TENANT_ADMIN);
        }
        if (access.normalAdmin()) {
            authorities.add(RoleType.TENANT_NORMAL_ADMIN);
        }
        authorities.addAll(access.permissions());

        return User.withUsername(userId).password(userInfo.getPasswd())
            .authorities(AuthorityUtils.createAuthorityList(authorities.toArray(String[]::new))).build();
    }
}
