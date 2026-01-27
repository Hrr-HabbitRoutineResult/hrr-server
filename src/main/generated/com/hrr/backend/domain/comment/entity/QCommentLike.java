package com.hrr.backend.domain.comment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCommentLike is a Querydsl query type for CommentLike
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommentLike extends EntityPathBase<CommentLike> {

    private static final long serialVersionUID = 1738734583L;

    public static final QCommentLike commentLike = new QCommentLike("commentLike");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCommentLike(String variable) {
        super(CommentLike.class, forVariable(variable));
    }

    public QCommentLike(Path<? extends CommentLike> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCommentLike(PathMetadata metadata) {
        super(CommentLike.class, metadata);
    }

}

