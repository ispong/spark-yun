package com.isxcode.spark.common.userlog;

import com.isxcode.spark.common.security.ContextHolder;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLogEventListener {

    private static final String SYSTEM_USER = "system";

    private final UserActionRepository userActionRepository;

    @Async("springEventThreadPool")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onUserLogEvent(UserLogEvent event) {

        try {
            UserActionEntity userAction = event.getUserAction();
            withAuditUser(userAction.getUserId(), () -> userActionRepository.save(userAction));
        } catch (Exception e) {
            log.error("Save user log failed", e);
        }
    }

    private <T> T withAuditUser(String userId, Supplier<T> supplier) {

        boolean needsContext = Strings.isEmpty(ContextHolder.getUserId());
        if (needsContext) {
            ContextHolder.setCurrentUser(Strings.isEmpty(userId) ? SYSTEM_USER : userId, null);
        }
        try {
            return supplier.get();
        } finally {
            if (needsContext) {
                ContextHolder.clear();
            }
        }
    }
}
