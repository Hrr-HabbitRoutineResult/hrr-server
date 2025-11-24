package com.hrr.backend.domain.user.entity;

import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.user.entity.enums.UserLevel;
import com.hrr.backend.domain.user.entity.enums.UserRole;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "nickname", length = 20, nullable = false)
    private String nickname;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "password", length = 225, nullable = true)
    private String password;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "profile_image", length = 225)
    private String profileImage;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private UserLevel userLevel = UserLevel.BRONZE;

    @NotNull
    @Builder.Default
    @Column(name = "follower_count", nullable = false)
    private Long followerCount = 0L;

    @NotNull
    @Builder.Default
    @Column(name = "following_count", nullable = false)
    private Long followingCount = 0L;

    @NotNull
    @Builder.Default
    @Column(name = "points", nullable = false)
    private Long points = 0L;

    @NotNull
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole userRole = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus userStatus;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationSetting> notificationSettings = new ArrayList<>();

    @Column(unique = true)
    private Long kakaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginStatus loginStatus;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFavor> userFavors = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserMission> userMissions = new ArrayList<>();

    /** 카카오 로그인용 팩토리 메서드 */
    public static User newKakao(Long kakaoId, String nickname, String profileImage) {
        User user = new User();
        user.kakaoId = kakaoId;
        user.nickname = nickname;
        user.profileImage = profileImage;
        // 초기 로그인 상태 명시적으로 설정 (nullable=false 대응)
        user.loginStatus = LoginStatus.NEW;
        return user;
    }

    /** 닉네임 업데이트 */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 프로필 이미지 업데이트 */
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    /** 로그인 상태 변경 */
    public void updateLoginStatus(LoginStatus status) {
        this.loginStatus = status;
    }
}
