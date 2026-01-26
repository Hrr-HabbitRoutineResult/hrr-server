package com.hrr.backend.domain.round.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoundRecord is a Querydsl query type for RoundRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoundRecord extends EntityPathBase<RoundRecord> {

    private static final long serialVersionUID = -178113391L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoundRecord roundRecord = new QRoundRecord("roundRecord");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> finalRank = createNumber("finalRank", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.hrr.backend.domain.round.entity.enums.NextRoundIntent> nextRoundIntent = createEnum("nextRoundIntent", com.hrr.backend.domain.round.entity.enums.NextRoundIntent.class);

    public final QRound round;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUserChallenge userChallenge;

    public final NumberPath<Integer> verificationCount = createNumber("verificationCount", Integer.class);

    public final ListPath<com.hrr.backend.domain.verification.entity.Verification, com.hrr.backend.domain.verification.entity.QVerification> verifications = this.<com.hrr.backend.domain.verification.entity.Verification, com.hrr.backend.domain.verification.entity.QVerification>createList("verifications", com.hrr.backend.domain.verification.entity.Verification.class, com.hrr.backend.domain.verification.entity.QVerification.class, PathInits.DIRECT2);

    public final NumberPath<Integer> warnCount = createNumber("warnCount", Integer.class);

    public QRoundRecord(String variable) {
        this(RoundRecord.class, forVariable(variable), INITS);
    }

    public QRoundRecord(Path<? extends RoundRecord> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoundRecord(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoundRecord(PathMetadata metadata, PathInits inits) {
        this(RoundRecord.class, metadata, inits);
    }

    public QRoundRecord(Class<? extends RoundRecord> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.round = inits.isInitialized("round") ? new QRound(forProperty("round"), inits.get("round")) : null;
        this.userChallenge = inits.isInitialized("userChallenge") ? new com.hrr.backend.domain.user.entity.QUserChallenge(forProperty("userChallenge"), inits.get("userChallenge")) : null;
    }

}

