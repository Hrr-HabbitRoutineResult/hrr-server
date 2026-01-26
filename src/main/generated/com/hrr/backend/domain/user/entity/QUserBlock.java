package com.hrr.backend.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserBlock is a Querydsl query type for UserBlock
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserBlock extends EntityPathBase<UserBlock> {

    private static final long serialVersionUID = 1488377245L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserBlock userBlock = new QUserBlock("userBlock");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final QUser blocked;

    public final QUser blocker;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QUserBlock(String variable) {
        this(UserBlock.class, forVariable(variable), INITS);
    }

    public QUserBlock(Path<? extends UserBlock> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserBlock(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserBlock(PathMetadata metadata, PathInits inits) {
        this(UserBlock.class, metadata, inits);
    }

    public QUserBlock(Class<? extends UserBlock> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.blocked = inits.isInitialized("blocked") ? new QUser(forProperty("blocked"), inits.get("blocked")) : null;
        this.blocker = inits.isInitialized("blocker") ? new QUser(forProperty("blocker"), inits.get("blocker")) : null;
    }

}

