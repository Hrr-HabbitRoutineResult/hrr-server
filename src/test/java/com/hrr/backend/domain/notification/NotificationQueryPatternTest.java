package com.hrr.backend.domain.notification;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import com.hrr.backend.domain.notification.repository.NotificationEventRepository;
import com.hrr.backend.domain.notification.repository.NotificationRepository;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.notification.repository.NotificationTypeRepository;
import com.hrr.backend.domain.notification.service.NotificationService;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.response.SliceResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class NotificationQueryPatternTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationEventRepository notificationEventRepository;
    @Autowired private NotificationTypeRepository notificationTypeRepository;
    @Autowired private NotificationSettingRepository notificationSettingRepository;
    @Autowired private RoundRecordRepository roundRecordRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private ChallengeRepository challengeRepository;
    @Autowired private UserChallengeRepository userChallengeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private SqlCaptureStatementInspector sqlCaptureStatementInspector;

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 3, 22);
    private static final int TOTAL_NOTIFICATION_COUNT = 20;
    private static final int CHALLENGE_EXTENSION_COUNT = 8;
    private static final int LEGACY_ROUND_RECORD_SELECT_COUNT = CHALLENGE_EXTENSION_COUNT;
    private static final int OPTIMIZED_ROUND_RECORD_SELECT_COUNT = 1;

    @Test
    @DisplayName("getNotificationList query pattern: challenge extension RoundRecord lookup is bulk fetched")
    void getNotificationList_RoundRecordLookupIsBulkFetched_Test() {
        User receiver = createQueryPatternData();

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        sqlCaptureStatementInspector.clear();

        SliceResponseDto<NotificationResponseDto.InfoDto> response =
                notificationService.getNotificationList(receiver, null, 0, TOTAL_NOTIFICATION_COUNT);

        List<String> sqls = sqlCaptureStatementInspector.getSqls();
        QueryCounts counts = QueryCounts.from(sqls);
        List<NotificationResponseDto.InfoDto> extensionDtos = response.getContent().stream()
                .filter(dto -> dto.getType() == NotificationTypeName.CHALLENGE_EXTENSION)
                .toList();
        long respondedCount = extensionDtos.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getIsResponded()))
                .count();
        long notRespondedCount = extensionDtos.stream()
                .filter(dto -> Boolean.FALSE.equals(dto.getIsResponded()))
                .count();

        printSqlReport(statistics, sqls, counts, extensionDtos.size());

        assertThat(response.getContent()).hasSize(TOTAL_NOTIFICATION_COUNT);
        assertThat(extensionDtos).hasSize(CHALLENGE_EXTENSION_COUNT);
        assertThat(respondedCount).isEqualTo(4);
        assertThat(notRespondedCount).isEqualTo(4);
        assertThat(counts.notificationDeliverySelects()).isEqualTo(1);
        assertThat(counts.notificationEventOnlySelects()).isZero();
        assertThat(counts.roundRecordSelects()).isEqualTo(OPTIMIZED_ROUND_RECORD_SELECT_COUNT);
    }

    private User createQueryPatternData() {
        Challenge challenge = challengeRepository.save(Challenge.builder()
                .title("query-pattern-challenge")
                .description("query pattern")
                .status(ChallengeStatus.ONGOING)
                .category(Category.HEALTH)
                .startDate(FIXED_DATE.atStartOfDay())
                .verificationType(VerificationType.TEXT)
                .verifyStartTime(LocalTime.of(9, 0))
                .verifyEndTime(LocalTime.of(22, 0))
                .currentParticipants(0)
                .maxParticipants(30)
                .isPublic(true)
                .isViewerMode(false)
                .imageKey("query-pattern-image")
                .build());

        User receiver = createUser("query_receiver");
        UserChallenge userChallenge = userChallengeRepository.save(UserChallenge.builder()
                .user(receiver)
                .challenge(challenge)
                .status(ChallengeJoinStatus.JOINED)
                .build());

        List<NotificationType> types = Arrays.stream(NotificationTypeName.values())
                .map(typeName -> NotificationType.builder()
                        .typeName(typeName)
                        .defaultEnabled(true)
                        .isMandatory(false)
                        .build())
                .toList();
        notificationTypeRepository.saveAll(types);

        for (int index = 0; index < TOTAL_NOTIFICATION_COUNT; index++) {
            Round round = roundRepository.save(Round.builder()
                    .challenge(challenge)
                    .roundNumber(index + 1)
                    .startDate(FIXED_DATE.plusWeeks(index))
                    .endDate(FIXED_DATE.plusWeeks(index).plusDays(6))
                    .build());

            NotificationTypeName typeName;
            if (index < CHALLENGE_EXTENSION_COUNT) {
                typeName = NotificationTypeName.CHALLENGE_EXTENSION;
                RoundRecord record = roundRecordRepository.save(RoundRecord.builder()
                        .round(round)
                        .userChallenge(userChallenge)
                        .build());
                if (index % 2 == 0) {
                    record.updateNextRoundIntent(NextRoundIntent.CONTINUE);
                }
            } else {
                typeName = switch (index % 3) {
                    case 0 -> NotificationTypeName.VERIFICATION_DEADLINE_3H;
                    case 1 -> NotificationTypeName.VERIFICATION_DEADLINE_1H;
                    default -> NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS;
                };
            }

            NotificationType type = findType(types, typeName);
            NotificationEvent event = notificationEventRepository.save(NotificationEvent.builder()
                    .type(type)
                    .category(categoryOf(typeName))
                    .targetType(ResourceType.CHALLENGE)
                    .targetId(challenge.getId())
                    .contextType(ResourceType.ROUND)
                    .contextId(round.getId())
                    .title("notification " + index)
                    .message("message " + index)
                    .imageKey("image-" + index)
                    .createdDate(FIXED_DATE.plusDays(index))
                    .build());

            notificationRepository.save(NotificationDelivery.builder()
                    .event(event)
                    .receiver(receiver)
                    .isRead(false)
                    .build());
        }

        return receiver;
    }

    private User createUser(String name) {
        User user = userRepository.save(User.builder()
                .name(name)
                .nickname(name + "_nick")
                .isPublic(true)
                .build());
        notificationSettingRepository.save(NotificationSetting.builder()
                .user(user)
                .isVerificationEnabled(true)
                .isChallengeEnabled(true)
                .isFollowEnabled(true)
                .isBadgeEnabled(true)
                .build());
        return user;
    }

    private NotificationType findType(List<NotificationType> types, NotificationTypeName typeName) {
        return types.stream()
                .filter(type -> type.getTypeName() == typeName)
                .findFirst()
                .orElseThrow();
    }

    private NotificationCategory categoryOf(NotificationTypeName typeName) {
        return switch (typeName) {
            case CHALLENGE_EXTENSION, CHALLENGE_EXTENSION_SUCCESS, CHALLENGE_EXTENSION_CANCEL ->
                    NotificationCategory.CHALLENGE;
            case VERIFICATION_DEADLINE_3H, VERIFICATION_DEADLINE_1H, VERIFICATION_DEADLINE_NOW ->
                    NotificationCategory.VERIFICATION;
        };
    }

    private void printSqlReport(
            Statistics statistics,
            List<String> sqls,
            QueryCounts counts,
            long extensionDtoCount
    ) {
        System.out.println();
        System.out.println("=== NotificationServiceImpl.getNotificationList SQL analysis ===");
        System.out.println("Returned notifications: " + TOTAL_NOTIFICATION_COUNT);
        System.out.println("Returned CHALLENGE_EXTENSION notifications: " + extensionDtoCount);
        System.out.println("Before refactoring RoundRecord SELECT count: " + LEGACY_ROUND_RECORD_SELECT_COUNT);
        System.out.println("After refactoring RoundRecord SELECT count: " + counts.roundRecordSelects());
        System.out.println("Hibernate prepare statement count: " + statistics.getPrepareStatementCount());
        System.out.println("Hibernate query execution count: " + statistics.getQueryExecutionCount());
        System.out.println("NotificationDelivery list SELECT count: " + counts.notificationDeliverySelects());
        System.out.println("NotificationEvent standalone SELECT count: " + counts.notificationEventOnlySelects());
        System.out.println("NotificationType standalone SELECT count: " + counts.notificationTypeSelects());
        System.out.println("RoundRecord SELECT count: " + counts.roundRecordSelects());
        System.out.println("Total captured SELECT count: " + counts.totalSelects());
        System.out.println("N+1 judgment: RoundRecord SELECT count is reduced from CHALLENGE_EXTENSION row count to one IN query.");
        System.out.println("Event fetch join judgment: NotificationEvent standalone SELECT count is 0, so findMyNotifications JOIN FETCH prevents NotificationDelivery.event N+1 in this scenario.");
        System.out.println();
        System.out.println("--- Hibernate JPQL query stats ---");
        for (String query : statistics.getQueries()) {
            QueryStatistics queryStatistics = statistics.getQueryStatistics(query);
            System.out.println("count=" + queryStatistics.getExecutionCount() + " query=" + query);
        }
        System.out.println();
        System.out.println("--- Captured SELECT SQL ---");
        AtomicInteger index = new AtomicInteger(1);
        sqls.stream()
                .filter(QueryCounts::isSelect)
                .forEach(sql -> System.out.println(index.getAndIncrement() + ". " + sql));
        System.out.println("=== End SQL analysis ===");
        System.out.println();
    }

    private record QueryCounts(
            long notificationDeliverySelects,
            long notificationEventOnlySelects,
            long notificationTypeSelects,
            long roundRecordSelects,
            long totalSelects
    ) {
        private static QueryCounts from(List<String> sqls) {
            long notificationDeliverySelects = 0;
            long notificationEventOnlySelects = 0;
            long notificationTypeSelects = 0;
            long roundRecordSelects = 0;
            long totalSelects = 0;

            for (String sql : sqls) {
                if (!isSelect(sql)) {
                    continue;
                }

                totalSelects++;
                String normalized = normalize(sql);

                if (normalized.contains(" from notification_delivery ")) {
                    notificationDeliverySelects++;
                }
                if (normalized.contains(" from notification_event ")
                        && !normalized.contains(" notification_delivery ")) {
                    notificationEventOnlySelects++;
                }
                if (normalized.contains(" from notification_type ")) {
                    notificationTypeSelects++;
                }
                if (normalized.contains(" from round_record ")) {
                    roundRecordSelects++;
                }
            }

            return new QueryCounts(
                    notificationDeliverySelects,
                    notificationEventOnlySelects,
                    notificationTypeSelects,
                    roundRecordSelects,
                    totalSelects
            );
        }

        private static boolean isSelect(String sql) {
            return sql != null && sql.trim().toLowerCase(Locale.ROOT).startsWith("select");
        }

        private static String normalize(String sql) {
            return " " + sql.toLowerCase(Locale.ROOT)
                    .replace('"', ' ')
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .replaceAll("\\s+", " ")
                    .trim() + " ";
        }
    }

    @TestConfiguration
    static class SqlCaptureTestConfiguration {

        @Bean
        SqlCaptureStatementInspector sqlCaptureStatementInspector() {
            return new SqlCaptureStatementInspector();
        }

        @Bean
        HibernatePropertiesCustomizer sqlCaptureHibernatePropertiesCustomizer(
                SqlCaptureStatementInspector sqlCaptureStatementInspector
        ) {
            return hibernateProperties -> hibernateProperties.put(
                    "hibernate.session_factory.statement_inspector",
                    sqlCaptureStatementInspector
            );
        }
    }

    static class SqlCaptureStatementInspector implements StatementInspector {
        private final List<String> sqls = new CopyOnWriteArrayList<>();

        @Override
        public String inspect(String sql) {
            sqls.add(sql);
            return sql;
        }

        void clear() {
            sqls.clear();
        }

        List<String> getSqls() {
            return List.copyOf(sqls);
        }
    }
}
