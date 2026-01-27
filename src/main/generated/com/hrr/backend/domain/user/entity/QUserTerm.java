package com.hrr.backend.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserTerm is a Querydsl query type for UserTerm
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserTerm extends EntityPathBase<UserTerm> {

    private static final long serialVersionUID = 325636444L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserTerm userTerm = new QUserTerm("userTerm");

    public final DateTimePath<java.time.LocalDateTime> agreedAt = createDateTime("agreedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isAgreed = createBoolean("isAgreed");

    public final com.hrr.backend.domain.term.entity.QTerm term;

    public final QUser user;

    public QUserTerm(String variable) {
        this(UserTerm.class, forVariable(variable), INITS);
    }

    public QUserTerm(Path<? extends UserTerm> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserTerm(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserTerm(PathMetadata metadata, PathInits inits) {
        this(UserTerm.class, metadata, inits);
    }

    public QUserTerm(Class<? extends UserTerm> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.term = inits.isInitialized("term") ? new com.hrr.backend.domain.term.entity.QTerm(forProperty("term")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

