package com.hrr.backend.domain.verification.converter;

import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationListResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationMyResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.response.PageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

public class VerificationConverter {

    public static VerificationResponseDto toResponse(Verification entity) {

        return VerificationResponseDto.builder()
                .verificationId(entity.getId())
                .challengeId(entity.getUserChallenge().getChallenge().getId())
                .userChallengeId(entity.getUserChallenge().getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .photoUrl(entity.getPhotoUrl())
                .textUrl(entity.getTextUrl())
                .isQuestion(entity.getIsQuestion())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .type(entity.getType())
                .roundId(entity.getRoundId())
                .build();
    }


    /* 본인 인증글 항목 변환 */
    public static VerificationMyResponseDto.MyPost toMyPost(Verification entity) {

        return VerificationMyResponseDto.MyPost.builder()
                .verificationId(entity.getId())
                .challengeId(entity.getUserChallenge().getChallenge().getId())
                .userChallengeId(entity.getUserChallenge().getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .photoUrl(entity.getPhotoUrl())
                .textUrl(entity.getTextUrl())
                .isQuestion(entity.getIsQuestion())
                .roundId(entity.getRoundId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .challengeTitle(entity.getUserChallenge().getChallenge().getTitle())
                .build();
    }


    /*본인 인증글 목록 변환*/
    public static VerificationMyResponseDto.MyPostList toMyPostList(Page<Verification> page) {

        List<VerificationMyResponseDto.MyPost> dtoList = page.getContent().stream()
                .map(VerificationConverter::toMyPost)
                .toList();

        PageImpl<VerificationMyResponseDto.MyPost> dtoPage =
                new PageImpl<>(dtoList, page.getPageable(), page.getTotalElements());

        return VerificationMyResponseDto.MyPostList.builder()
                .posts(new PageResponseDto<>(dtoPage))
                .build();
    }

    /* 인증글 상세 조회 변환 */
    public static VerificationDetailResponseDto toDetailResponse(Verification entity) {

        return VerificationDetailResponseDto.builder()
                .verificationId(entity.getId())

                .challengeId(entity.getUserChallenge().getChallenge().getId())
                .userChallengeId(entity.getUserChallenge().getId())

                .userId(entity.getUserChallenge().getUser().getId())
                .nickname(entity.getUserChallenge().getUser().getNickname())
                .profileImage(entity.getUserChallenge().getUser().getProfileImage())

                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .photoUrl(entity.getPhotoUrl())
                .textUrl(entity.getTextUrl())
                .isQuestion(entity.getIsQuestion())
                .status(entity.getStatus())

                .challengeTitle(entity.getUserChallenge().getChallenge().getTitle())

                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())

                .roundId(entity.getRoundId())

                .build();
    }
    /* 챌린지 + 라운드별 변환 */
    public static VerificationListResponseDto.ListItem toChallengeRoundListItem(Verification entity) {

        return VerificationListResponseDto.ListItem.builder()
                .verificationId(entity.getId())
                .challengeId(entity.getUserChallenge().getChallenge().getId())
                .userChallengeId(entity.getUserChallenge().getId())

                .nickname(entity.getUserChallenge().getUser().getNickname())
                .profileImage(entity.getUserChallenge().getUser().getProfileImage())

                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .photoUrl(entity.getPhotoUrl())
                .textUrl(entity.getTextUrl())

                .isQuestion(entity.getIsQuestion())
                .roundId(entity.getRoundId())

                .challengeTitle(entity.getUserChallenge().getChallenge().getTitle())

                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())

                .build();
    }

    /* 챌린지 + 라운드별 인증글 목록 변환 */
    public static VerificationListResponseDto.ListResponse toChallengeRoundList(Page<Verification> page) {

        List<VerificationListResponseDto.ListItem> dtoList =
                page.getContent().stream()
                        .map(VerificationConverter::toChallengeRoundListItem)
                        .toList();

        PageImpl<VerificationListResponseDto.ListItem> dtoPage =
                new PageImpl<>(dtoList, page.getPageable(), page.getTotalElements());

        return VerificationListResponseDto.ListResponse.builder()
                .posts(new PageResponseDto<>(dtoPage))
                .empty(page.isEmpty())
                .build();
    }


}
