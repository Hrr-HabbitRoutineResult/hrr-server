package com.hrr.backend.domain.round.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRound is a Querydsl query type for Round
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRound extends EntityPathBase<Round> {

    private static final long serialVersionUID = -1130990560L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRound round = new QRound("round");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final com.hrr.backend.domain.challenge.entity.QChallenge challenge;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> roundNumber = createNumber("roundNumber", Integer.class);

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRound(String variable) {
        this(Round.class, forVariable(variable), INITS);
    }

    public QRound(Path<? extends Round> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRound(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRound(PathMetadata metadata, PathInits inits) {
        this(Round.class, metadata, inits);
    }

    public QRound(Class<? extends Round> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.challenge = inits.isInitialized("challenge") ? new com.hrr.backend.domain.challenge.entity.QChallenge(forProperty("challenge"), inits.get("challenge")) : null;
    }

}

