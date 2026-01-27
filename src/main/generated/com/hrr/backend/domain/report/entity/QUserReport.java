package com.hrr.backend.domain.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserReport is a Querydsl query type for UserReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserReport extends EntityPathBase<UserReport> {

    private static final long serialVersionUID = 504411149L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserReport userReport = new QUserReport("userReport");

    public final QBaseReport _super;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt;

    //inherited
    public final StringPath description;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final EnumPath<com.hrr.backend.global.common.enums.ReportReason> reason;

    // inherited
    public final com.hrr.backend.domain.user.entity.QUser reporter;

    //inherited
    public final EnumPath<com.hrr.backend.global.common.enums.ReportStatus> status;

    public final com.hrr.backend.domain.user.entity.QUser targetUser;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt;

    public QUserReport(String variable) {
        this(UserReport.class, forVariable(variable), INITS);
    }

    public QUserReport(Path<? extends UserReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserReport(PathMetadata metadata, PathInits inits) {
        this(UserReport.class, metadata, inits);
    }

    public QUserReport(Class<? extends UserReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new QBaseReport(type, metadata, inits);
        this.createdAt = _super.createdAt;
        this.description = _super.description;
        this.reason = _super.reason;
        this.reporter = _super.reporter;
        this.status = _super.status;
        this.targetUser = inits.isInitialized("targetUser") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("targetUser"), inits.get("targetUser")) : null;
        this.updatedAt = _super.updatedAt;
    }

}

