package com.hrr.backend.domain.comment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCommentReport is a Querydsl query type for CommentReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommentReport extends EntityPathBase<CommentReport> {

    private static final long serialVersionUID = 349899156L;

    public static final QCommentReport commentReport = new QCommentReport("commentReport");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCommentReport(String variable) {
        super(CommentReport.class, forVariable(variable));
    }

    public QCommentReport(Path<? extends CommentReport> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCommentReport(PathMetadata metadata) {
        super(CommentReport.class, metadata);
    }

}

