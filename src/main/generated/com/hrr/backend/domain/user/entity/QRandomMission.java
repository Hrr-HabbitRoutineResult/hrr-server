package com.hrr.backend.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRandomMission is a Querydsl query type for RandomMission
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRandomMission extends EntityPathBase<RandomMission> {

    private static final long serialVersionUID = -702503964L;

    public static final QRandomMission randomMission = new QRandomMission("randomMission");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final EnumPath<com.hrr.backend.global.common.enums.Category> category = createEnum("category", com.hrr.backend.global.common.enums.Category.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageKey = createString("imageKey");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRandomMission(String variable) {
        super(RandomMission.class, forVariable(variable));
    }

    public QRandomMission(Path<? extends RandomMission> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRandomMission(PathMetadata metadata) {
        super(RandomMission.class, metadata);
    }

}

