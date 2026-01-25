package com.hrr.backend.domain.dm.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDmMessage is a Querydsl query type for DmMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmMessage extends EntityPathBase<DmMessage> {

    private static final long serialVersionUID = 1721687323L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDmMessage dmMessage = new QDmMessage("dmMessage");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final StringPath clientMessageUuid = createString("clientMessageUuid");

    public final StringPath content = createString("content");

    public final QDmConversation conversation;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.hrr.backend.domain.dm.entity.enums.DeliveryStatus> deliveryStatus = createEnum("deliveryStatus", com.hrr.backend.domain.dm.entity.enums.DeliveryStatus.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.hrr.backend.domain.dm.entity.enums.DmMessageType> messageType = createEnum("messageType", com.hrr.backend.domain.dm.entity.enums.DmMessageType.class);

    public final com.hrr.backend.domain.user.entity.QUser sender;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QDmMessage(String variable) {
        this(DmMessage.class, forVariable(variable), INITS);
    }

    public QDmMessage(Path<? extends DmMessage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDmMessage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDmMessage(PathMetadata metadata, PathInits inits) {
        this(DmMessage.class, metadata, inits);
    }

    public QDmMessage(Class<? extends DmMessage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.conversation = inits.isInitialized("conversation") ? new QDmConversation(forProperty("conversation"), inits.get("conversation")) : null;
        this.sender = inits.isInitialized("sender") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("sender"), inits.get("sender")) : null;
    }

}

