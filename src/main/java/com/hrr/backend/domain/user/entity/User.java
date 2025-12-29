package com.hrr.backend.domain.user.entity;

import com.hrr.backend.domain.auth.entity.SocialAuth;
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

	@Column(name = "name")	// 로그인 시 제공받는 이름. 중복이 가능하기에 unique 설정 x. 소셜 타입에 따라 이름 제공이 되지 않을 수 있어 nullable.
	private String name;

    @Column(name = "nickname", length = 20, unique = true)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "password", length = 225, nullable = true)
    private String password;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;	// 탈퇴 시점 - 탈퇴 후 1개월이 지나면 hard delete, 1개월 동안은 DELETED 상태로써 재가입 불가능

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

	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoginStatus loginStatus = LoginStatus.NEW;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private SocialAuth socialAuth;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFavor> userFavors = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserMission> userMissions = new ArrayList<>();

	/**
	 * 로그인/회원가입용 팩토리 메서드
	 */
	public static User signUp(String name, String profileImage) {
		return User.builder()
			.name(name)
			.profileImage(profileImage)
			.loginStatus(LoginStatus.NEW)
			.userLevel(UserLevel.BRONZE)
			.userRole(UserRole.USER)
			.build();
	}

    /** 닉네임 업데이트 */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

	/** 이름 업데이트 */
	public void updateName(String name) {
		this.name = name;
	}

    /** 프로필 이미지 업데이트 */
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

	/** 이메일 업데이트 */
	public void updateEmail(String email) {
		this.email = email;
	}

    /** 온보딩 진행 상태로 다음 단계 반환
     * new -> terms : 약관동의 화면
     * terms_done -> signup: 가입 정보 입력 화면
     * active -> main : 메인화면으로 gogo
     */
    public String determineNextStep() {
        if (this.loginStatus == null) {
            return "TERMS"; // 혹시 null이면 가장 처음 단계로
        }

        return switch (this.loginStatus) {
            case NEW -> "TERMS";
            case TERMS_DONE -> "SIGNUP";
            case ACTIVE -> "MAIN";
        };
    }

    /** 로그인 상태 변경 */
    public void updateLoginStatus(LoginStatus status) {
        this.loginStatus = status;
    }

	// 탈퇴 처리 메서드
	public void withdraw() {
		this.userStatus = UserStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}
}
