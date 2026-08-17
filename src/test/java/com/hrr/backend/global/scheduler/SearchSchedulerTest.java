package com.hrr.backend.global.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.hrr.backend.domain.search.entity.KeywordHourlyLog;
import com.hrr.backend.domain.search.repository.KeywordHourlyLogRepository;
import com.hrr.backend.domain.search.repository.PopularKeywordRepository;

class SearchSchedulerTest {

    @Test
    void migrateRedisToLogTable_rollsBackAndKeepsRedisKey_whenDatabaseSaveFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        @SuppressWarnings("unchecked")
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        KeywordHourlyLogRepository hourlyLogRepository = mock(KeywordHourlyLogRepository.class);
        PopularKeywordRepository popularKeywordRepository = mock(PopularKeywordRepository.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(any(), anyLong(), anyLong()))
                .thenReturn(Set.of(tuple));
        when(tuple.getValue()).thenReturn("exercise");
        when(tuple.getScore()).thenReturn(3.0);
        when(hourlyLogRepository.saveAllAndFlush(any()))
                .thenThrow(new IllegalStateException("database unavailable"));

        SearchScheduler scheduler = transactionalProxy(
                new SearchScheduler(redisTemplate, hourlyLogRepository, popularKeywordRepository),
                transactionManager
        );

        assertThatCode(scheduler::migrateRedisToLogTable).doesNotThrowAnyException();

        assertThat(transactionManager.rolledBack).isTrue();
        assertThat(transactionManager.committed).isFalse();
        verify(redisTemplate, never()).delete(any(String.class));
    }

    private SearchScheduler transactionalProxy(
            SearchScheduler target,
            PlatformTransactionManager transactionManager
    ) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        interceptor.afterPropertiesSet();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.addAdvice(interceptor);
        return (SearchScheduler) proxyFactory.getProxy();
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private boolean committed;
        private boolean rolledBack;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // 테스트용 트랜잭션이므로 실제 resource를 시작하지 않는다.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            committed = true;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rolledBack = true;
        }
    }
}
