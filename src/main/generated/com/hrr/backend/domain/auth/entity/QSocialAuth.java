package com.hrr.backend.domain.auth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSocialAuth is a Querydsl query type for SocialAuth
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSocialAuth extends EntityPathBase<SocialAuth> {

    private static final long serialVersionUID = 772373655L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSocialAuth socialAuth = new QSocialAuth("socialAuth");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath socialId = createString("socialId");

    public final StringPath socialRefreshToken = createString("socialRefreshToken");

    public final EnumPath<com.hrr.backend.domain.auth.entity.enums.SocialType> socialType = createEnum("socialType", com.hrr.backend.domain.auth.entity.enums.SocialType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.hrr.backend.domain.user.entity.QUser user;

    public QSocialAuth(String variable) {
        this(SocialAuth.class, forVariable(variable), INITS);
    }

    public QSocialAuth(Path<? extends SocialAuth> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSocialAuth(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSocialAuth(PathMetadata metadata, PathInits inits) {
        this(SocialAuth.class, metadata, inits);
    }

    public QSocialAuth(Class<? extends SocialAuth> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

