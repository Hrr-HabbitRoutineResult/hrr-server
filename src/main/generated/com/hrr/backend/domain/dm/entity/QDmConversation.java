package com.hrr.backend.domain.dm.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDmConversation is a Querydsl query type for DmConversation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmConversation extends EntityPathBase<DmConversation> {

    private static final long serialVersionUID = -680719377L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDmConversation dmConversation = new QDmConversation("dmConversation");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QDmMessage lastMessage;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUser user1;

    public final com.hrr.backend.domain.user.entity.QUser user2;

    public QDmConversation(String variable) {
        this(DmConversation.class, forVariable(variable), INITS);
    }

    public QDmConversation(Path<? extends DmConversation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDmConversation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDmConversation(PathMetadata metadata, PathInits inits) {
        this(DmConversation.class, metadata, inits);
    }

    public QDmConversation(Class<? extends DmConversation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.lastMessage = inits.isInitialized("lastMessage") ? new QDmMessage(forProperty("lastMessage"), inits.get("lastMessage")) : null;
        this.user1 = inits.isInitialized("user1") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("user1"), inits.get("user1")) : null;
        this.user2 = inits.isInitialized("user2") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("user2"), inits.get("user2")) : null;
    }

}

