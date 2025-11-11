package com.hrr.backend.domain.user.entity;

import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @Column(unique = true)
    private Long kakaoId;

    private String nickname;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginStatus loginStatus;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFavor> userFavors = new ArrayList<>();

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