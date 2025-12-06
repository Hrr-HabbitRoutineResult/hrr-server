package com.hrr.backend.domain.verification.converter;

import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationListResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationMyResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.response.PageResponseDto;
import com.hrr.backend.global.s3.S3UrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VerificationConverter {


    private final S3UrlUtil s3UrlUtil;

   /**
   * Verification 엔티티를 VerificationResponseDto로 변환
     */
    public VerificationResponseDto toResponseDto(Verification verification) {
        return VerificationResponseDto.builder()
                .verificationId(verification.getId())
                .roundId(verification.getRoundId())
                .challengeId(verification.getUserChallenge().getChallenge().getId())
                .type(verification.getType())
                .title(verification.getTitle())
                .content(verification.getContent())
                .textUrl(verification.getTextUrl())
                .photoUrl(s3UrlUtil.toFullUrl(verification.getPhotoUrl()))
                .isQuestion(verification.getIsQuestion())
                .status(verification.getStatus())
                .createdAt(verification.getCreatedAt())
                .userId(verification.getUserChallenge().getUser().getId())
                .userNickname(verification.getUserChallenge().getUser().getNickname())
                .verificationCount(verification.getRoundRecord().getVerificationCount())
                .build();
    }


//    /* 본인 인증글 항목 변환 */
//    public static VerificationMyResponseDto.MyPost toMyPost(Verification entity) {
//
//        return VerificationMyResponseDto.MyPost.builder()
//                .verificationId(entity.getId())
//                .challengeId(entity.getUserChallenge().getChallenge().getId())
//                .userChallengeId(entity.getUserChallenge().getId())
//                .title(entity.getTitle())
//                .content(entity.getContent())
//                .photoUrl(entity.getPhotoUrl())
//                .textUrl(entity.getTextUrl())
//                .isQuestion(entity.getIsQuestion())
//                .roundId(entity.getRoundId())
//                .createdAt(entity.getCreatedAt())
//                .updatedAt(entity.getUpdatedAt())
//                .challengeTitle(entity.getUserChallenge().getChallenge().getTitle())
//                .build();
//    }


//    /*본인 인증글 목록 변환*/
//    public static VerificationMyResponseDto.MyPostList toMyPostList(Page<Verification> page) {
//
//        List<VerificationMyResponseDto.MyPost> dtoList = page.getContent().stream()
//                .map(VerificationConverter::toMyPost)
//                .toList();
//
//        PageImpl<VerificationMyResponseDto.MyPost> dtoPage =
//                new PageImpl<>(dtoList, page.getPageable(), page.getTotalElements());
//
//        return VerificationMyResponseDto.MyPostList.builder()
//                .posts(new PageResponseDto<>(dtoPage))
//                .build();
//    }
//
//    /* 인증글 상세 조회 변환 */
//    public static VerificationDetailResponseDto toDetailResponse(Verification entity) {
//
//        return VerificationDetailResponseDto.builder()
//                .verificationId(entity.getId())
//
//                .challengeId(entity.getUserChallenge().getChallenge().getId())
//                .userChallengeId(entity.getUserChallenge().getId())
//
//                .userId(entity.getUserChallenge().getUser().getId())
//                .nickname(entity.getUserChallenge().getUser().getNickname())
//                .profileImage(entity.getUserChallenge().getUser().getProfileImage())
//
//                .type(entity.getType())
//                .title(entity.getTitle())
//                .content(entity.getContent())
//                .photoUrl(entity.getPhotoUrl())
//                .textUrl(entity.getTextUrl())
//                .isQuestion(entity.getIsQuestion())
//                .status(entity.getStatus())
//
//                .challengeTitle(entity.getUserChallenge().getChallenge().getTitle())
//
//                .createdAt(entity.getCreatedAt())
//                .updatedAt(entity.getUpdatedAt())
//
//                .roundId(entity.getRoundId())
//
//                .build();
//    }

}
