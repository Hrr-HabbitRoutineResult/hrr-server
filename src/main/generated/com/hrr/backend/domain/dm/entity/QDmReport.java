package com.hrr.backend.domain.dm.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDmReport is a Querydsl query type for DmReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmReport extends EntityPathBase<DmReport> {

    private static final long serialVersionUID = 752780704L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDmReport dmReport = new QDmReport("dmReport");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    public final StringPath adminComment = createString("adminComment");

    public final QDmConversation conversation;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath customReason = createString("customReason");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QDmMessage message;

    public final EnumPath<com.hrr.backend.global.common.enums.ReportReason> reason = createEnum("reason", com.hrr.backend.global.common.enums.ReportReason.class);

    public final com.hrr.backend.domain.user.entity.QUser reportedUser;

    public final com.hrr.backend.domain.user.entity.QUser reporter;

    public final EnumPath<com.hrr.backend.global.common.enums.ReportStatus> status = createEnum("status", com.hrr.backend.global.common.enums.ReportStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QDmReport(String variable) {
        this(DmReport.class, forVariable(variable), INITS);
    }

    public QDmReport(Path<? extends DmReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDmReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDmReport(PathMetadata metadata, PathInits inits) {
        this(DmReport.class, metadata, inits);
    }

    public QDmReport(Class<? extends DmReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.conversation = inits.isInitialized("conversation") ? new QDmConversation(forProperty("conversation"), inits.get("conversation")) : null;
        this.message = inits.isInitialized("message") ? new QDmMessage(forProperty("message"), inits.get("message")) : null;
        this.reportedUser = inits.isInitialized("reportedUser") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("reportedUser"), inits.get("reportedUser")) : null;
        this.reporter = inits.isInitialized("reporter") ? new com.hrr.backend.domain.user.entity.QUser(forProperty("reporter"), inits.get("reporter")) : null;
    }

}

