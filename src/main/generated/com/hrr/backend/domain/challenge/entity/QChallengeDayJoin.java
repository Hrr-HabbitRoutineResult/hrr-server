package com.hrr.backend.domain.challenge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChallengeDayJoin is a Querydsl query type for ChallengeDayJoin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChallengeDayJoin extends EntityPathBase<ChallengeDayJoin> {

    private static final long serialVersionUID = 2116300166L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChallengeDayJoin challengeDayJoin = new QChallengeDayJoin("challengeDayJoin");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final QChallenge challenge;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.hrr.backend.global.common.enums.ChallengeDays> dayOfWeek = createEnum("dayOfWeek", com.hrr.backend.global.common.enums.ChallengeDays.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QChallengeDayJoin(String variable) {
        this(ChallengeDayJoin.class, forVariable(variable), INITS);
    }

    public QChallengeDayJoin(Path<? extends ChallengeDayJoin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChallengeDayJoin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChallengeDayJoin(PathMetadata metadata, PathInits inits) {
        this(ChallengeDayJoin.class, metadata, inits);
    }

    public QChallengeDayJoin(Class<? extends ChallengeDayJoin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.challenge = inits.isInitialized("challenge") ? new QChallenge(forProperty("challenge"), inits.get("challenge")) : null;
    }

}

