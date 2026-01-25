package com.hrr.backend.domain.challenge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChallengeLike is a Querydsl query type for ChallengeLike
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChallengeLike extends EntityPathBase<ChallengeLike> {

    private static final long serialVersionUID = -1885426441L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChallengeLike challengeLike = new QChallengeLike("challengeLike");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final QChallenge challenge;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUser user;

    public QChallengeLike(String variable) {
        this(ChallengeLike.class, forVariable(variable), INITS);
    }

    public QChallengeLike(Path<? extends ChallengeLike> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChallengeLike(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChallengeLike(PathMetadata metadata, PathInits inits) {
        this(ChallengeLike.class, metadata, inits);
    }

    public QChallengeLike(Class<? extends ChallengeLike> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.challenge = inits.isInitialized("challenge") ? new QChallenge(forProperty("challenge"), inits.get("challenge")) : null;
        this.user = inits.isInitialized("user") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

