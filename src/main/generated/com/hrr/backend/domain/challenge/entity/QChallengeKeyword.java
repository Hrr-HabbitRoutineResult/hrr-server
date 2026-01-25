package com.hrr.backend.domain.challenge.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChallengeKeyword is a Querydsl query type for ChallengeKeyword
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChallengeKeyword extends EntityPathBase<ChallengeKeyword> {

    private static final long serialVersionUID = -145251191L;

    public static final QChallengeKeyword challengeKeyword = new QChallengeKeyword("challengeKeyword");

    public final com.hrr.backend.global.common.QBaseEntity _super = new com.hrr.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QChallengeKeyword(String variable) {
        super(ChallengeKeyword.class, forVariable(variable));
    }

    public QChallengeKeyword(Path<? extends ChallengeKeyword> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChallengeKeyword(PathMetadata metadata) {
        super(ChallengeKeyword.class, metadata);
    }

}

