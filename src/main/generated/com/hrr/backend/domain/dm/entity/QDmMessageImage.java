package com.hrr.backend.domain.dm.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDmMessageImage is a Querydsl query type for DmMessageImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmMessageImage extends EntityPathBase<DmMessageImage> {

    private static final long serialVersionUID = 1567479040L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDmMessageImage dmMessageImage = new QDmMessageImage("dmMessageImage");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> filesizeBytes = createNumber("filesizeBytes", Long.class);

    public final NumberPath<Integer> height = createNumber("height", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QDmMessage message;

    public final StringPath mimetype = createString("mimetype");

    public final StringPath originFilename = createString("originFilename");

    public final StringPath s3Key = createString("s3Key");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> width = createNumber("width", Integer.class);

    public QDmMessageImage(String variable) {
        this(DmMessageImage.class, forVariable(variable), INITS);
    }

    public QDmMessageImage(Path<? extends DmMessageImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDmMessageImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDmMessageImage(PathMetadata metadata, PathInits inits) {
        this(DmMessageImage.class, metadata, inits);
    }

    public QDmMessageImage(Class<? extends DmMessageImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.message = inits.isInitialized("message") ? new QDmMessage(forProperty("message"), inits.get("message")) : null;
    }

}

