package com.hrr.backend.global.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/** Discord 전송 자체의 장애 로그가 다시 Discord appender로 들어가는 재귀를 차단한다. */
public class DiscordInternalLogFilter extends Filter<ILoggingEvent> {

    static final String INTERNAL_LOGGER_NAME = "com.hrr.backend.global.logging.DiscordDeliveryMonitor";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        return INTERNAL_LOGGER_NAME.equals(event.getLoggerName()) ? FilterReply.DENY : FilterReply.NEUTRAL;
    }
}
