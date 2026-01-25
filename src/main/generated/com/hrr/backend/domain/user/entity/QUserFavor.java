package com.hrr.backend.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserFavor is a Querydsl query type for UserFavor
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserFavor extends EntityPathBase<UserFavor> {

    private static final long serialVersionUID = 1491750734L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserFavor userFavor = new QUserFavor("userFavor");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final EnumPath<com.hrr.backend.global.common.enums.AgeGroup> ageGroup = createEnum("ageGroup", com.hrr.backend.global.common.enums.AgeGroup.class);

    public final SetPath<com.hrr.backend.global.common.enums.AvailableTime, EnumPath<com.hrr.backend.global.common.enums.AvailableTime>> availableTime = this.<com.hrr.backend.global.common.enums.AvailableTime, EnumPath<com.hrr.backend.global.common.enums.AvailableTime>>createSet("availableTime", com.hrr.backend.global.common.enums.AvailableTime.class, EnumPath.class, PathInits.DIRECT2);

    public final SetPath<com.hrr.backend.global.common.enums.Category, EnumPath<com.hrr.backend.global.common.enums.Category>> category = this.<com.hrr.backend.global.common.enums.Category, EnumPath<com.hrr.backend.global.common.enums.Category>>createSet("category", com.hrr.backend.global.common.enums.Category.class, EnumPath.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.hrr.backend.global.common.enums.Gender> gender = createEnum("gender", com.hrr.backend.global.common.enums.Gender.class);

    public final EnumPath<com.hrr.backend.global.common.enums.Goal> goal = createEnum("goal", com.hrr.backend.global.common.enums.Goal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.hrr.backend.global.common.enums.Job> job = createEnum("job", com.hrr.backend.global.common.enums.Job.class);

    public final ListPath<com.hrr.backend.domain.recommendation.entity.RecommendationResult, com.hrr.backend.domain.recommendation.entity.QRecommendationResult> recommendationResults = this.<com.hrr.backend.domain.recommendation.entity.RecommendationResult, com.hrr.backend.domain.recommendation.entity.QRecommendationResult>createList("recommendationResults", com.hrr.backend.domain.recommendation.entity.RecommendationResult.class, com.hrr.backend.domain.recommendation.entity.QRecommendationResult.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final QUser user;

    public QUserFavor(String variable) {
        this(UserFavor.class, forVariable(variable), INITS);
    }

    public QUserFavor(Path<? extends UserFavor> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserFavor(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserFavor(PathMetadata metadata, PathInits inits) {
        this(UserFavor.class, metadata, inits);
    }

    public QUserFavor(Class<? extends UserFavor> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

