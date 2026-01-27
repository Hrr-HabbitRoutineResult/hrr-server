package com.hrr.backend.domain.challenge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChallenge is a Querydsl query type for Challenge
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChallenge extends EntityPathBase<Challenge> {

    private static final long serialVersionUID = 179382592L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChallenge challenge = new QChallenge("challenge");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final EnumPath<com.hrr.backend.global.common.enums.Category> category = createEnum("category", com.hrr.backend.global.common.enums.Category.class);

    public final ListPath<ChallengeDayJoin, QChallengeDayJoin> challengeDays = this.<ChallengeDayJoin, QChallengeDayJoin>createList("challengeDays", ChallengeDayJoin.class, QChallengeDayJoin.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> currentParticipants = createNumber("currentParticipants", Integer.class);

    public final com.hrr.backend.domain.round.entity.QRound currentRound;

    public final StringPath description = createString("description");

    public final QChallengeEmbedding embedding;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageKey = createString("imageKey");

    public final BooleanPath isPublic = createBoolean("isPublic");

    public final BooleanPath isViewerMode = createBoolean("isViewerMode");

    public final NumberPath<Integer> likeCount = createNumber("likeCount", Integer.class);

    public final NumberPath<Integer> maxParticipants = createNumber("maxParticipants", Integer.class);

    public final StringPath password = createString("password");

    public final ListPath<com.hrr.backend.domain.recommendation.entity.RecommendationResult, com.hrr.backend.domain.recommendation.entity.QRecommendationResult> recommendationResults = this.<com.hrr.backend.domain.recommendation.entity.RecommendationResult, com.hrr.backend.domain.recommendation.entity.QRecommendationResult>createList("recommendationResults", com.hrr.backend.domain.recommendation.entity.RecommendationResult.class, com.hrr.backend.domain.recommendation.entity.QRecommendationResult.class, PathInits.DIRECT2);

    public final StringPath rule = createString("rule");

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final EnumPath<com.hrr.backend.global.common.enums.ChallengeStatus> status = createEnum("status", com.hrr.backend.global.common.enums.ChallengeStatus.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final EnumPath<com.hrr.backend.global.common.enums.VerificationType> verificationType = createEnum("verificationType", com.hrr.backend.global.common.enums.VerificationType.class);

    public final TimePath<java.time.LocalTime> verifyEndTime = createTime("verifyEndTime", java.time.LocalTime.class);

    public final TimePath<java.time.LocalTime> verifyStartTime = createTime("verifyStartTime", java.time.LocalTime.class);

    public QChallenge(String variable) {
        this(Challenge.class, forVariable(variable), INITS);
    }

    public QChallenge(Path<? extends Challenge> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChallenge(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChallenge(PathMetadata metadata, PathInits inits) {
        this(Challenge.class, metadata, inits);
    }

    public QChallenge(Class<? extends Challenge> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.currentRound = inits.isInitialized("currentRound") ? new com.hrr.backend.domain.round.entity.QRound(forProperty("currentRound"), inits.get("currentRound")) : null;
        this.embedding = inits.isInitialized("embedding") ? new QChallengeEmbedding(forProperty("embedding"), inits.get("embedding")) : null;
    }

}

