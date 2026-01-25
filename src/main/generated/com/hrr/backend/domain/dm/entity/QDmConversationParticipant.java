package com.hrr.backend.domain.dm.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDmConversationParticipant is a Querydsl query type for DmConversationParticipant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmConversationParticipant extends EntityPathBase<DmConversationParticipant> {

    private static final long serialVersionUID = -1802563644L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDmConversationParticipant dmConversationParticipant = new QDmConversationParticipant("dmConversationParticipant");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final QDmConversation conversation;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isBlocked = createBoolean("isBlocked");

    public final BooleanPath isMuted = createBoolean("isMuted");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUser user;

    public QDmConversationParticipant(String variable) {
        this(DmConversationParticipant.class, forVariable(variable), INITS);
    }

    public QDmConversationParticipant(Path<? extends DmConversationParticipant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDmConversationParticipant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDmConversationParticipant(PathMetadata metadata, PathInits inits) {
        this(DmConversationParticipant.class, metadata, inits);
    }

    public QDmConversationParticipant(Class<? extends DmConversationParticipant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.conversation = inits.isInitialized("conversation") ? new QDmConversation(forProperty("conversation"), inits.get("conversation")) : null;
        this.user = inits.isInitialized("user") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

