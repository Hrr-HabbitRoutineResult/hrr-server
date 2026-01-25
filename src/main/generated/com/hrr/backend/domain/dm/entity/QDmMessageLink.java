package com.hrr.backend.domain.dm.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDmMessageLink is a Querydsl query type for DmMessageLink
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmMessageLink extends EntityPathBase<DmMessageLink> {

    private static final long serialVersionUID = 327744437L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDmMessageLink dmMessageLink = new QDmMessageLink("dmMessageLink");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QDmMessage message;

    public final StringPath thumbnailUrl = createString("thumbnailUrl");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath url = createString("url");

    public QDmMessageLink(String variable) {
        this(DmMessageLink.class, forVariable(variable), INITS);
    }

    public QDmMessageLink(Path<? extends DmMessageLink> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDmMessageLink(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDmMessageLink(PathMetadata metadata, PathInits inits) {
        this(DmMessageLink.class, metadata, inits);
    }

    public QDmMessageLink(Class<? extends DmMessageLink> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.message = inits.isInitialized("message") ? new QDmMessage(forProperty("message"), inits.get("message")) : null;
    }

}

