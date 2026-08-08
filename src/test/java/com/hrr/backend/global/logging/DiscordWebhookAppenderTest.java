package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

class DiscordWebhookAppenderTest {

    @Test
    void buildDedupKey_usesMessageTemplate_insteadOfDynamicArguments() {
        DiscordWebhookAppender appender = new DiscordWebhookAppender();
        ILoggingEvent first = mockEvent("처리 실패. userId={}", "처리 실패. userId=1");
        ILoggingEvent second = mockEvent("처리 실패. userId={}", "처리 실패. userId=999");

        assertThat(appender.buildDedupKey(first)).isEqualTo(appender.buildDedupKey(second));
    }

    @Test
    void start_disablesDeliveryWithoutThrowing_whenWebhookUrlIsInvalid() {
        DiscordWebhookAppender appender = new DiscordWebhookAppender();
        appender.setWebhookUrl("not-an-absolute-url");

        assertThatCode(appender::start).doesNotThrowAnyException();
        assertThat(appender.isStarted()).isTrue();
    }

    private ILoggingEvent mockEvent(String template, String formattedMessage) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("test.logger");
        when(event.getMessage()).thenReturn(template);
        when(event.getFormattedMessage()).thenReturn(formattedMessage);
        return event;
    }
}
