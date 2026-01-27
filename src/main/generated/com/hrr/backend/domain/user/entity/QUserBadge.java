package com.hrr.backend.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserBadge is a Querydsl query type for UserBadge
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserBadge extends EntityPathBase<UserBadge> {

    private static final long serialVersionUID = 1488039091L;

    public static final QUserBadge userBadge = new QUserBadge("userBadge");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QUserBadge(String variable) {
        super(UserBadge.class, forVariable(variable));
    }

    public QUserBadge(Path<? extends UserBadge> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserBadge(PathMetadata metadata) {
        super(UserBadge.class, metadata);
    }

}

