package com.hrr.backend.domain.challenge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChallengeEmbedding is a Querydsl query type for ChallengeEmbedding
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChallengeEmbedding extends EntityPathBase<ChallengeEmbedding> {

    private static final long serialVersionUID = 1701291511L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChallengeEmbedding challengeEmbedding1 = new QChallengeEmbedding("challengeEmbedding1");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final QChallenge challenge;

    public final ArrayPath<byte[], Byte> challengeEmbedding = createArray("challengeEmbedding", byte[].class);

    public final StringPath challengeText = createString("challengeText");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QChallengeEmbedding(String variable) {
        this(ChallengeEmbedding.class, forVariable(variable), INITS);
    }

    public QChallengeEmbedding(Path<? extends ChallengeEmbedding> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChallengeEmbedding(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChallengeEmbedding(PathMetadata metadata, PathInits inits) {
        this(ChallengeEmbedding.class, metadata, inits);
    }

    public QChallengeEmbedding(Class<? extends ChallengeEmbedding> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.challenge = inits.isInitialized("challenge") ? new QChallenge(forProperty("challenge"), inits.get("challenge")) : null;
    }

}

