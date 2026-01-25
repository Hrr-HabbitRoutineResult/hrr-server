package com.hrr.backend.domain.recommendation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecommendationResult is a Querydsl query type for RecommendationResult
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecommendationResult extends EntityPathBase<RecommendationResult> {

    private static final long serialVersionUID = 762831593L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecommendationResult recommendationResult = new QRecommendationResult("recommendationResult");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final com.hrr.backend.domain.challenge.entity.QChallenge challenge;

    public final NumberPath<Float> cosineScore = createNumber("cosineScore", Float.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> ranking = createNumber("ranking", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUserFavor userFavor;

    public QRecommendationResult(String variable) {
        this(RecommendationResult.class, forVariable(variable), INITS);
    }

    public QRecommendationResult(Path<? extends RecommendationResult> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecommendationResult(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecommendationResult(PathMetadata metadata, PathInits inits) {
        this(RecommendationResult.class, metadata, inits);
    }

    public QRecommendationResult(Class<? extends RecommendationResult> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.challenge = inits.isInitialized("challenge") ? new com.hrr.backend.domain.challenge.entity.QChallenge(forProperty("challenge"), inits.get("challenge")) : null;
        this.userFavor = inits.isInitialized("userFavor") ? new com.hrr.backend.domain.user.entity.QUserFavor(forProperty("userFavor"), inits.get("userFavor")) : null;
    }

}

