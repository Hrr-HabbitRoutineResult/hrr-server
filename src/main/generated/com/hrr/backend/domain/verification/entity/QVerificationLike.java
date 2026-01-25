package com.hrr.backend.domain.verification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QVerificationLike is a Querydsl query type for VerificationLike
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVerificationLike extends EntityPathBase<VerificationLike> {

    private static final long serialVersionUID = 998242087L;

    public static final QVerificationLike verificationLike = new QVerificationLike("verificationLike");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QVerificationLike(String variable) {
        super(VerificationLike.class, forVariable(variable));
    }

    public QVerificationLike(Path<? extends VerificationLike> path) {
        super(path.getType(), path.getMetadata());
    }

    public QVerificationLike(PathMetadata metadata) {
        super(VerificationLike.class, metadata);
    }

}

