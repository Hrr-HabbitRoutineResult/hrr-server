package com.hrr.backend.domain.badge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBadgeCondition is a Querydsl query type for BadgeCondition
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBadgeCondition extends EntityPathBase<BadgeCondition> {

    private static final long serialVersionUID = 1189697211L;

    public static final QBadgeCondition badgeCondition = new QBadgeCondition("badgeCondition");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBadgeCondition(String variable) {
        super(BadgeCondition.class, forVariable(variable));
    }

    public QBadgeCondition(Path<? extends BadgeCondition> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBadgeCondition(PathMetadata metadata) {
        super(BadgeCondition.class, metadata);
    }

}

