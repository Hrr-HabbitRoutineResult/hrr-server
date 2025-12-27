package com.hrr.backend.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// UserService는 사용자 중심 로직 담당으로 남기고, 삭제 관련 로직을 분리하여 관리 차원 로직 담당
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeleteService {

	private final EntityManager em;

	@Transactional
	public void executeHardDelete(LocalDateTime threshold) {
		// 삭제 대상 유저 ID 확보
		List<Long> targetIds = em.createQuery(
				"SELECT u.id FROM User u WHERE u.deletedAt <= :threshold", Long.class)
			.setParameter("threshold", threshold)
			.getResultList();

		if (targetIds.isEmpty()){
			log.info("삭제 대상 사용자가 없습니다.");
			return;
		}

		// TODO: S3 사용하는 객체의 경우 파일 삭제 로직이 선행되어야 함

		log.info("탈퇴 후 한 달 경과 사용자 {} 명에 대한 hard delete를 시작합니다.", targetIds.size());	// 추후 모니터링 통해 삭제 규모 파악하여 필요 시 최적화

		// --- Notifications 체인 정리 ---
		List<Long> eventIds = em.createNativeQuery("SELECT id FROM notification_event WHERE actor_id IN :ids")
			.setParameter("ids", targetIds).getResultList();

		if (!eventIds.isEmpty()) {
			em.createNativeQuery("DELETE FROM notification_delivery WHERE receiver_id IN :ids OR event_id IN :eventIds")
				.setParameter("ids", targetIds).setParameter("eventIds", eventIds).executeUpdate();
			em.createNativeQuery("DELETE FROM notification_event WHERE id IN :eventIds")
				.setParameter("eventIds", eventIds).executeUpdate();
		} else {
			em.createNativeQuery("DELETE FROM notification_delivery WHERE receiver_id IN :ids")
				.setParameter("ids", targetIds).executeUpdate();
		}

		// --- DM 체인 정리 ---
		List<Long> convIds = em.createNativeQuery(
				"SELECT id FROM dm_conversation WHERE user1_id IN :ids OR user2_id IN :ids")
			.setParameter("ids", targetIds).getResultList();

		if (!convIds.isEmpty()) {
			// 순환 참조 끊기 (last_message_id)
			em.createNativeQuery("UPDATE dm_conversation SET last_message_id = NULL WHERE id IN :convIds")
				.setParameter("convIds", convIds).executeUpdate();

			// 읽기 기록 및 신고 정리
			em.createNativeQuery("DELETE FROM dm_read WHERE user_id IN :ids OR conversation_id IN :convIds")
				.setParameter("ids", targetIds).setParameter("convIds", convIds).executeUpdate();
			em.createNativeQuery("DELETE FROM dm_report WHERE reported_user_id IN :ids OR reporter_id IN :ids OR conversation_id IN :convIds")
				.setParameter("ids", targetIds).setParameter("convIds", convIds).executeUpdate();

			// 메시지 및 첨부파일 정리
			List<Long> msgIds = em.createNativeQuery(
					"SELECT id FROM dm_message WHERE sender_id IN :ids OR conversation_id IN :convIds")
				.setParameter("ids", targetIds).setParameter("convIds", convIds).getResultList();

			if (!msgIds.isEmpty()) {
				em.createNativeQuery("DELETE FROM dm_message_image WHERE message_id IN :msgIds")
					.setParameter("msgIds", msgIds).executeUpdate();
				em.createNativeQuery("DELETE FROM dm_message_link WHERE message_id IN :msgIds")
					.setParameter("msgIds", msgIds).executeUpdate();
				em.createNativeQuery("DELETE FROM dm_message WHERE id IN :msgIds")
					.setParameter("msgIds", msgIds).executeUpdate();
			}

			// 대화 참여자 및 대화방 삭제
			em.createNativeQuery("DELETE FROM dm_conversation_participant WHERE user_id IN :ids OR conversation_id IN :convIds")
				.setParameter("ids", targetIds).setParameter("convIds", convIds).executeUpdate();
			em.createNativeQuery("DELETE FROM dm_conversation WHERE id IN :convIds")
				.setParameter("convIds", convIds).executeUpdate();
		}

		// --- Verification / Challenge 체인 정리 ---
		List<Long> ucIds = em.createNativeQuery("SELECT id FROM user_challenge WHERE user_id IN :ids")
			.setParameter("ids", targetIds).getResultList();

		if (!ucIds.isEmpty()) {
			List<Long> rrIds = em.createNativeQuery("SELECT id FROM round_record WHERE user_challenge_id IN :ucIds")
				.setParameter("ucIds", ucIds).getResultList();

			List<Long> verifIds = em.createNativeQuery(
					"SELECT id FROM verification WHERE round_record_id IN :rrIds OR user_challenge_id IN :ucIds")
				.setParameter("rrIds", rrIds.isEmpty() ? List.of(-1L) : rrIds)
				.setParameter("ucIds", ucIds).getResultList();

			if (!verifIds.isEmpty()) {
				em.createNativeQuery("DELETE FROM verification_post_report WHERE reporter_id IN :ids OR verification_id IN :verifIds")
					.setParameter("ids", targetIds).setParameter("verifIds", verifIds).executeUpdate();
				// comment는 ON DELETE CASCADE
				em.createNativeQuery("DELETE FROM verification WHERE id IN :verifIds")
					.setParameter("verifIds", verifIds).executeUpdate();
			}

			if (!rrIds.isEmpty()) {
				em.createNativeQuery("DELETE FROM round_record WHERE id IN :rrIds")
					.setParameter("rrIds", rrIds).executeUpdate();
			}
			em.createNativeQuery("DELETE FROM user_challenge WHERE id IN :ucIds")
				.setParameter("ucIds", ucIds).executeUpdate();
		}

		// --- 단순 직참조 테이블 정리 ---
		deleteSimpleReferences(targetIds);

		// --- 최종 유저 삭제 ---
		em.createNativeQuery("DELETE FROM user WHERE id IN :ids")
			.setParameter("ids", targetIds).executeUpdate();

		log.info("사용자 {}명 데이터 삭제 완료", targetIds.size());
	}

	private void deleteSimpleReferences(List<Long> ids) {
		// FK 제약이 덜 복잡한 테이블들
		String[][] simpleTables = {
			{"challenge_like", "user_id"}, {"challenge_wait", "user_id"},
			{"comment", "user_id"}, {"follow", "follower_id"}, {"follow", "following_id"},
			{"user_block", "blocked_id"}, {"user_block", "blocker_id"},
			{"fcm_token", "user_id"}, {"social_auth", "user_id"},
			{"notification_setting", "user_id"}, {"user_mission", "user_id"},
			{"user_term", "user_id"}, {"user_favor", "user_id"},
			{"user_favor_available_time", "user_favor_id"}, // 컬렉션 테이블
			{"user_favor_category", "user_favor_id"}         // 컬렉션 테이블
		};

		for (String[] table : simpleTables) {
			em.createNativeQuery("DELETE FROM " + table[0] + " WHERE " + table[1] + " IN :ids")
				.setParameter("ids", ids).executeUpdate();
		}
	}
}
