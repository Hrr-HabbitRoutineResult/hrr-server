package com.hrr.backend.domain.kickout.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QKickout is a Querydsl query type for Kickout
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QKickout extends EntityPathBase<Kickout> {

    private static final long serialVersionUID = 920971552L;

    public static final QKickout kickout = new QKickout("kickout");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QKickout(String variable) {
        super(Kickout.class, forVariable(variable));
    }

    public QKickout(Path<? extends Kickout> path) {
        super(path.getType(), path.getMetadata());
    }

    public QKickout(PathMetadata metadata) {
        super(Kickout.class, metadata);
    }

}

