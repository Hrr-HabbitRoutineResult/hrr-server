package com.hrr.backend.domain.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBaseReport is a Querydsl query type for BaseReport
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBaseReport extends EntityPathBase<BaseReport> {

    private static final long serialVersionUID = -1905462637L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBaseReport baseReport = new QBaseReport("baseReport");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final EnumPath<com.hrr.backend.global.common.enums.ReportReason> reason = createEnum("reason", com.hrr.backend.global.common.enums.ReportReason.class);

    public final com.hrr.backend.domain.user.entity.QUser reporter;

    public final EnumPath<com.hrr.backend.global.common.enums.ReportStatus> status = createEnum("status", com.hrr.backend.global.common.enums.ReportStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBaseReport(String variable) {
        this(BaseReport.class, forVariable(variable), INITS);
    }

    public QBaseReport(Path<? extends BaseReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBaseReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBaseReport(PathMetadata metadata, PathInits inits) {
        this(BaseReport.class, metadata, inits);
    }

    public QBaseReport(Class<? extends BaseReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reporter = inits.isInitialized("reporter") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("reporter"), inits.get("reporter")) : null;
    }

}

