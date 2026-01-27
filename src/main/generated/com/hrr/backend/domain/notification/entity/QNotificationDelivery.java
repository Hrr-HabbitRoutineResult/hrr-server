package com.hrr.backend.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationDelivery is a Querydsl query type for NotificationDelivery
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationDelivery extends EntityPathBase<NotificationDelivery> {

    private static final long serialVersionUID = 651188516L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationDelivery notificationDelivery = new QNotificationDelivery("notificationDelivery");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QNotificationEvent event;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isRead = createBoolean("isRead");

    public final DateTimePath<java.time.LocalDateTime> readAt = createDateTime("readAt", java.time.LocalDateTime.class);

    public final com.hrr.backend.domain.user.entity.QUser receiver;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotificationDelivery(String variable) {
        this(NotificationDelivery.class, forVariable(variable), INITS);
    }

    public QNotificationDelivery(Path<? extends NotificationDelivery> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationDelivery(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationDelivery(PathMetadata metadata, PathInits inits) {
        this(NotificationDelivery.class, metadata, inits);
    }

    public QNotificationDelivery(Class<? extends NotificationDelivery> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event = inits.isInitialized("event") ? new QNotificationEvent(forProperty("event"), inits.get("event")) : null;
        this.receiver = inits.isInitialized("receiver") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("receiver"), inits.get("receiver")) : null;
    }

}

