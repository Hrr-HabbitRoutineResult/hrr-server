package com.hrr.backend.domain.search.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QKeywordHourlyLog is a Querydsl query type for KeywordHourlyLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QKeywordHourlyLog extends EntityPathBase<KeywordHourlyLog> {

    private static final long serialVersionUID = -456135604L;

    public static final QKeywordHourlyLog keywordHourlyLog = new QKeywordHourlyLog("keywordHourlyLog");

    public final NumberPath<Long> count = createNumber("count", Long.class);

    public final DateTimePath<java.time.LocalDateTime> hour = createDateTime("hour", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath keyword = createString("keyword");

    public QKeywordHourlyLog(String variable) {
        super(KeywordHourlyLog.class, forVariable(variable));
    }

    public QKeywordHourlyLog(Path<? extends KeywordHourlyLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QKeywordHourlyLog(PathMetadata metadata) {
        super(KeywordHourlyLog.class, metadata);
    }

}

