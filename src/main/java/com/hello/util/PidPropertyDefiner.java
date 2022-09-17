package com.hello.util;

import ch.qos.logback.core.PropertyDefinerBase;

public class PidPropertyDefiner extends PropertyDefinerBase {
    public String getPropertyValue() {
        return Long.toString(ProcessHandle.current().pid());
    }
}
