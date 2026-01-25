package com.hrr.backend.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNotificationType is a Querydsl query type for NotificationType
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationType extends EntityPathBase<NotificationType> {

    private static final long serialVersionUID = 1375724650L;

    public static final QNotificationType notificationType = new QNotificationType("notificationType");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath defaultEnabled = createBoolean("defaultEnabled");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isMandatory = createBoolean("isMandatory");

    public final EnumPath<com.hrr.backend.domain.notification.entity.enums.NotificationTypeName> typeName = createEnum("typeName", com.hrr.backend.domain.notification.entity.enums.NotificationTypeName.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotificationType(String variable) {
        super(NotificationType.class, forVariable(variable));
    }

    public QNotificationType(Path<? extends NotificationType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNotificationType(PathMetadata metadata) {
        super(NotificationType.class, metadata);
    }

}

