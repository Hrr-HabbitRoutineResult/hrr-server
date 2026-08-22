package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;

import java.time.LocalDateTime;

public class NotificationEventUpsertRepositoryImpl implements NotificationEventUpsertRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void upsert(NotificationEvent event) {
        if (isH2()) {
            upsertForH2(event);
            return;
        }

        upsertForMySql(event);
    }

    private void upsertForMySql(NotificationEvent event) {
        entityManager.createNativeQuery("""
                        INSERT INTO notification_event (
                            actor_id, category, context_id, context_type, created_at, created_date,
                            image_key, message, target_id, target_type, title, type_id, updated_at
                        )
                        VALUES (
                            :actorId, :category, :contextId, :contextType, :createdAt, :createdDate,
                            :imageKey, :message, :targetId, :targetType, :title, :typeId, :updatedAt
                        )
                        ON DUPLICATE KEY UPDATE id = id
                        """)
                .setParameter("actorId", event.getActor() == null ? null : event.getActor().getId())
                .setParameter("category", event.getCategory().name())
                .setParameter("contextId", event.getContextId())
                .setParameter("contextType", event.getContextType().name())
                .setParameter("createdAt", LocalDateTime.now())
                .setParameter("createdDate", event.getCreatedDate())
                .setParameter("imageKey", event.getImageKey())
                .setParameter("message", event.getMessage())
                .setParameter("targetId", event.getTargetId())
                .setParameter("targetType", event.getTargetType().name())
                .setParameter("title", event.getTitle())
                .setParameter("typeId", event.getType().getId())
                .setParameter("updatedAt", LocalDateTime.now())
                .executeUpdate();
    }

    private void upsertForH2(NotificationEvent event) {
        entityManager.createNativeQuery("""
                        MERGE INTO notification_event (
                            actor_id, category, context_id, context_type, created_at, created_date,
                            image_key, message, target_id, target_type, title, type_id, updated_at
                        )
                        KEY (context_type, context_id, type_id, created_date)
                        VALUES (
                            :actorId, :category, :contextId, :contextType, :createdAt, :createdDate,
                            :imageKey, :message, :targetId, :targetType, :title, :typeId, :updatedAt
                        )
                        """)
                .setParameter("actorId", event.getActor() == null ? null : event.getActor().getId())
                .setParameter("category", event.getCategory().name())
                .setParameter("contextId", event.getContextId())
                .setParameter("contextType", event.getContextType().name())
                .setParameter("createdAt", LocalDateTime.now())
                .setParameter("createdDate", event.getCreatedDate())
                .setParameter("imageKey", event.getImageKey())
                .setParameter("message", event.getMessage())
                .setParameter("targetId", event.getTargetId())
                .setParameter("targetType", event.getTargetType().name())
                .setParameter("title", event.getTitle())
                .setParameter("typeId", event.getType().getId())
                .setParameter("updatedAt", LocalDateTime.now())
                .executeUpdate();
    }

    private boolean isH2() {
        return entityManager.unwrap(Session.class)
                .doReturningWork(connection ->
                        connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2"));
    }
}
