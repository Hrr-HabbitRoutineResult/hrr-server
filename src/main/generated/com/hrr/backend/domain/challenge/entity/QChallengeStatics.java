package com.hrr.backend.domain.challenge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChallengeStatics is a Querydsl query type for ChallengeStatics
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChallengeStatics extends EntityPathBase<ChallengeStatics> {

    private static final long serialVersionUID = -1227979163L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChallengeStatics challengeStatics = new QChallengeStatics("challengeStatics");

    public final QChallenge challenge;

    public final EnumPath<com.hrr.backend.global.common.enums.FavorType> favorType = createEnum("favorType", com.hrr.backend.global.common.enums.FavorType.class);

    public final StringPath favorValue = createString("favorValue");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> number = createNumber("number", Integer.class);

    public QChallengeStatics(String variable) {
        this(ChallengeStatics.class, forVariable(variable), INITS);
    }

    public QChallengeStatics(Path<? extends ChallengeStatics> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChallengeStatics(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChallengeStatics(PathMetadata metadata, PathInits inits) {
        this(ChallengeStatics.class, metadata, inits);
    }

    public QChallengeStatics(Class<? extends ChallengeStatics> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.challenge = inits.isInitialized("challenge") ? new QChallenge(forProperty("challenge"), inits.get("challenge")) : null;
    }

}

