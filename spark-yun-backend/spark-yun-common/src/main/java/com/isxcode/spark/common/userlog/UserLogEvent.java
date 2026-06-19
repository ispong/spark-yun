package com.isxcode.spark.common.userlog;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserLogEvent extends ApplicationEvent {

    private final UserActionEntity userAction;

    public UserLogEvent(Object source, UserActionEntity userAction) {

        super(source);
        this.userAction = userAction;
    }
}
