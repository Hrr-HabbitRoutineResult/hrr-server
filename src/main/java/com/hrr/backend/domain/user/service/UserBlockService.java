package com.hrr.backend.domain.user.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.dto.UserBlockResponse;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserBlock;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
// 메소드가 많지 않아 인터페이스 생략
public class UserBlockService {

	private final UserRepository userRepository;
	private final UserBlockRepository userBlockRepository;
	private final FollowRepository followRepository;
	private final CommentRepository commentRepository;

	@Transactional
	public void blockUser(Long blockerId, Long blockedId) {
		// 사용자 확인 - 탈퇴 여부 관계 없이 차단하고 싶을 수 있으니 모든 사용자 차단 허용
		User blocker = userRepository.findById(blockerId)
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
		User blocked = userRepository.findById(blockedId)
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

		// 셀프 차단 체크
		if (blocker.getId().equals(blockedId)) {
			throw new GlobalException(ErrorCode.CANNOT_BLOCK_SELF);
		}

		// 이미 차단했는지 확인
		if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
			throw new GlobalException(ErrorCode.ALREADY_BLOCKED);
		}

        // 상호 팔로우 관계 해제 및 카운트 감소
        // (1) 내가 상대를 팔로우 중인 경우 해제
        followRepository.findByFollowerIdAndFollowingId(blockerId, blockedId)
                .ifPresent(follow -> {
                    followRepository.delete(follow); // 데이터 삭제
                    blocker.decrementFollowingCount(); // 내 팔로잉 수 감소
                    blocked.decrementFollowerCount();  // 상대 팔로워 수 감소
                });

        // (2) 상대가 나를 팔로우 중인 경우 해제
        followRepository.findByFollowerIdAndFollowingId(blockedId, blockerId)
                .ifPresent(follow -> {
                    followRepository.delete(follow); // 데이터 삭제
                    blocked.decrementFollowingCount(); // 상대 팔로잉 수 감소
                    blocker.decrementFollowerCount();  // 내 팔로워 수 감소
                });

		// 차단 내역 저장
		UserBlock userBlock = UserBlock.builder()
			.blocker(blocker)
			.blocked(blocked)
			.build();
		userBlockRepository.save(userBlock);
	}

	@Transactional
	public void unblock(Long blockerId, Long blockedId) {
		// 사용자 확인
		User blocker = userRepository.findById(blockerId)
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
		User blocked = userRepository.findById(blockedId)
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

		// 차단 관계가 있는지 확인 후 삭제
		if (!userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
			throw new GlobalException(ErrorCode.NOT_BLOCKED_USER);
		}

		userBlockRepository.deleteByBlockerAndBlocked(blocker, blocked);
	}

	@Transactional(readOnly = true)
	public SliceResponseDto<UserBlockResponse> getMyBlockList(User blocker, Pageable pageable) {

		Slice<UserBlock> blockSlice = userBlockRepository.findByBlocker(blocker, pageable);

		Slice<UserBlockResponse> responseSlice = blockSlice.map(block -> new UserBlockResponse(block.getBlocked()));

		// 공통 SliceResponseDto로 감싸서 반환
		return SliceResponseDto.of(responseSlice);
	}

	@Transactional
	public void blockUserByCommentId(Long blockerId, Long commentId) {
		// 댓글 조회 및 작성자(Target) 추출
		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

		User blockedUser = comment.getUser();

		// 본인 차단 여부 등 유효성 검사 후 기존 blockUser 로직 호출
		blockUser(blockerId, blockedUser.getId());
	}
}
