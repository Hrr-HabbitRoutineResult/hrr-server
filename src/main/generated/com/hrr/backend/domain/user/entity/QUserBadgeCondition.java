package com.hrr.backend.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserBadgeCondition is a Querydsl query type for UserBadgeCondition
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserBadgeCondition extends EntityPathBase<UserBadgeCondition> {

    private static final long serialVersionUID = 1239775656L;

    public static final QUserBadgeCondition userBadgeCondition = new QUserBadgeCondition("userBadgeCondition");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QUserBadgeCondition(String variable) {
        super(UserBadgeCondition.class, forVariable(variable));
    }

    public QUserBadgeCondition(Path<? extends UserBadgeCondition> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserBadgeCondition(PathMetadata metadata) {
        super(UserBadgeCondition.class, metadata);
    }

}

