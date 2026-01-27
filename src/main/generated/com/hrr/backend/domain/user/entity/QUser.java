package com.hrr.backend.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 516817104L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> followerCount = createNumber("followerCount", Long.class);

    public final NumberPath<Long> followingCount = createNumber("followingCount", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isPublic = createBoolean("isPublic");

    public final EnumPath<com.hrr.backend.domain.user.entity.enums.LoginStatus> loginStatus = createEnum("loginStatus", com.hrr.backend.domain.user.entity.enums.LoginStatus.class);

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final com.hrr.backend.domain.notification.entity.QNotificationSetting notificationSetting;

    public final StringPath password = createString("password");

    public final StringPath phoneNumber = createString("phoneNumber");

    public final NumberPath<Long> points = createNumber("points", Long.class);

    public final StringPath profileImage = createString("profileImage");

    public final com.hrr.backend.domain.auth.entity.QSocialAuth socialAuth;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final QUserFavor userFavor;

    public final EnumPath<com.hrr.backend.domain.user.entity.enums.UserLevel> userLevel = createEnum("userLevel", com.hrr.backend.domain.user.entity.enums.UserLevel.class);

    public final ListPath<UserMission, QUserMission> userMissions = this.<UserMission, QUserMission>createList("userMissions", UserMission.class, QUserMission.class, PathInits.DIRECT2);

    public final EnumPath<com.hrr.backend.domain.user.entity.enums.UserRole> userRole = createEnum("userRole", com.hrr.backend.domain.user.entity.enums.UserRole.class);

    public final EnumPath<com.hrr.backend.domain.user.entity.enums.UserStatus> userStatus = createEnum("userStatus", com.hrr.backend.domain.user.entity.enums.UserStatus.class);

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.notificationSetting = inits.isInitialized("notificationSetting") ? new com.hrr.backend.domain.notification.entity.QNotificationSetting(forProperty("notificationSetting"), inits.get("notificationSetting")) : null;
        this.socialAuth = inits.isInitialized("socialAuth") ? new com.hrr.backend.domain.auth.entity.QSocialAuth(forProperty("socialAuth"), inits.get("socialAuth")) : null;
        this.userFavor = inits.isInitialized("userFavor") ? new QUserFavor(forProperty("userFavor"), inits.get("userFavor")) : null;
    }

}

