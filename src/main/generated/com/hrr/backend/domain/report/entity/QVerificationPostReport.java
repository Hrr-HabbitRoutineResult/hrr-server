package com.hrr.backend.domain.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVerificationPostReport is a Querydsl query type for VerificationPostReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVerificationPostReport extends EntityPathBase<VerificationPostReport> {

    private static final long serialVersionUID = 176317597L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVerificationPostReport verificationPostReport = new QVerificationPostReport("verificationPostReport");

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

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt;

    public final com.hrr.backend.domain.verification.entity.QVerification verification;

    public QVerificationPostReport(String variable) {
        this(VerificationPostReport.class, forVariable(variable), INITS);
    }

    public QVerificationPostReport(Path<? extends VerificationPostReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVerificationPostReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVerificationPostReport(PathMetadata metadata, PathInits inits) {
        this(VerificationPostReport.class, metadata, inits);
    }

    public QVerificationPostReport(Class<? extends VerificationPostReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new QBaseReport(type, metadata, inits);
        this.createdAt = _super.createdAt;
        this.description = _super.description;
        this.reason = _super.reason;
        this.reporter = _super.reporter;
        this.status = _super.status;
        this.updatedAt = _super.updatedAt;
        this.verification = inits.isInitialized("verification") ? new com.hrr.backend.domain.verification.entity.QVerification(forProperty("verification"), inits.get("verification")) : null;
    }

}

