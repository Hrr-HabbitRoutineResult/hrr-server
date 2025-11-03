package com.hrr.backend.domain.user.entity;

import com.hrr.backend.domain.user.entity.enums.ReportStatus;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_report")
public class UserReport extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id", nullable = false)
    private User reported;

    @Column(name = "reason")
    private String reason;

    @Lob
    @Column(name = "reason_text", columnDefinition = "TEXT") // ERD에 text(NULL)
    private String reasonText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReportStatus status;
}
