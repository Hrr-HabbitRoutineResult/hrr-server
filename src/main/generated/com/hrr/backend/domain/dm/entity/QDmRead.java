package com.hrr.backend.domain.dm.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDmRead is a Querydsl query type for DmRead
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmRead extends EntityPathBase<DmRead> {

    private static final long serialVersionUID = -164580094L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDmRead dmRead = new QDmRead("dmRead");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final QDmConversation conversation;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QDmMessage lastReadMessage;

    public final DateTimePath<java.time.LocalDateTime> readAt = createDateTime("readAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUser user;

    public QDmRead(String variable) {
        this(DmRead.class, forVariable(variable), INITS);
    }

    public QDmRead(Path<? extends DmRead> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDmRead(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDmRead(PathMetadata metadata, PathInits inits) {
        this(DmRead.class, metadata, inits);
    }

    public QDmRead(Class<? extends DmRead> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.conversation = inits.isInitialized("conversation") ? new QDmConversation(forProperty("conversation"), inits.get("conversation")) : null;
        this.lastReadMessage = inits.isInitialized("lastReadMessage") ? new QDmMessage(forProperty("lastReadMessage"), inits.get("lastReadMessage")) : null;
        this.user = inits.isInitialized("user") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

