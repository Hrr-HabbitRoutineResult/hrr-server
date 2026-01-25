package com.hrr.backend.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationEvent is a Querydsl query type for NotificationEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationEvent extends EntityPathBase<NotificationEvent> {

    private static final long serialVersionUID = -316161174L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationEvent notificationEvent = new QNotificationEvent("notificationEvent");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final com.hrr.backend.domain.user.entity.QUser actor;

    public final EnumPath<com.hrr.backend.domain.notification.entity.enums.NotificationCategory> category = createEnum("category", com.hrr.backend.domain.notification.entity.enums.NotificationCategory.class);

    public final NumberPath<Long> contextId = createNumber("contextId", Long.class);

    public final EnumPath<com.hrr.backend.domain.notification.entity.enums.ResourceType> contextType = createEnum("contextType", com.hrr.backend.domain.notification.entity.enums.ResourceType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageKey = createString("imageKey");

    public final StringPath message = createString("message");

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final EnumPath<com.hrr.backend.domain.notification.entity.enums.ResourceType> targetType = createEnum("targetType", com.hrr.backend.domain.notification.entity.enums.ResourceType.class);

    public final StringPath title = createString("title");

    public final QNotificationType type;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotificationEvent(String variable) {
        this(NotificationEvent.class, forVariable(variable), INITS);
    }

    public QNotificationEvent(Path<? extends NotificationEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationEvent(PathMetadata metadata, PathInits inits) {
        this(NotificationEvent.class, metadata, inits);
    }

    public QNotificationEvent(Class<? extends NotificationEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.actor = inits.isInitialized("actor") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("actor"), inits.get("actor")) : null;
        this.type = inits.isInitialized("type") ? new QNotificationType(forProperty("type")) : null;
    }

}

