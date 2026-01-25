package com.hrr.backend.domain.challenge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChallengeWait is a Querydsl query type for ChallengeWait
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChallengeWait extends EntityPathBase<ChallengeWait> {

    private static final long serialVersionUID = -1885106475L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChallengeWait challengeWait = new QChallengeWait("challengeWait");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final QChallenge challenge;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUser user;

    public QChallengeWait(String variable) {
        this(ChallengeWait.class, forVariable(variable), INITS);
    }

    public QChallengeWait(Path<? extends ChallengeWait> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChallengeWait(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChallengeWait(PathMetadata metadata, PathInits inits) {
        this(ChallengeWait.class, metadata, inits);
    }

    public QChallengeWait(Class<? extends ChallengeWait> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.challenge = inits.isInitialized("challenge") ? new QChallenge(forProperty("challenge"), inits.get("challenge")) : null;
        this.user = inits.isInitialized("user") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

