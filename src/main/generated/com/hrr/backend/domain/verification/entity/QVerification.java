package com.hrr.backend.domain.verification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVerification is a Querydsl query type for Verification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVerification extends EntityPathBase<Verification> {

    private static final long serialVersionUID = 162466672L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVerification verification = new QVerification("verification");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isQuestion = createBoolean("isQuestion");

    public final BooleanPath isResolved = createBoolean("isResolved");

    public final StringPath photoUrl = createString("photoUrl");

    public final NumberPath<Integer> reportCount = createNumber("reportCount", Integer.class);

    public final NumberPath<Long> roundId = createNumber("roundId", Long.class);

    public final com.hrr.backend.domain.round.entity.QRoundRecord roundRecord;

    public final EnumPath<com.hrr.backend.domain.verification.entity.enums.VerificationStatus> status = createEnum("status", com.hrr.backend.domain.verification.entity.enums.VerificationStatus.class);

    public final ListPath<String, StringPath> textImages = this.<String, StringPath>createList("textImages", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath textUrl = createString("textUrl");

    public final StringPath title = createString("title");

    public final EnumPath<com.hrr.backend.domain.verification.entity.enums.VerificationPostType> type = createEnum("type", com.hrr.backend.domain.verification.entity.enums.VerificationPostType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUserChallenge userChallenge;

    public QVerification(String variable) {
        this(Verification.class, forVariable(variable), INITS);
    }

    public QVerification(Path<? extends Verification> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVerification(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVerification(PathMetadata metadata, PathInits inits) {
        this(Verification.class, metadata, inits);
    }

    public QVerification(Class<? extends Verification> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.roundRecord = inits.isInitialized("roundRecord") ? new com.hrr.backend.domain.round.entity.QRoundRecord(forProperty("roundRecord"), inits.get("roundRecord")) : null;
        this.userChallenge = inits.isInitialized("userChallenge") ? new com.hrr.backend.domain.user.entity.QUserChallenge(forProperty("userChallenge"), inits.get("userChallenge")) : null;
    }

}

