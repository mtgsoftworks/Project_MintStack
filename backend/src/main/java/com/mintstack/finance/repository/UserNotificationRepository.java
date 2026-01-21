package com.mintstack.finance.repository;

import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    Page<UserNotification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<UserNotification> findByUserAndIsReadOrderByCreatedAtDesc(User user, Boolean isRead, Pageable pageable);

    long countByUserAndIsRead(User user, Boolean isRead);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsRead(@Param("user") User user);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    int markAsRead(@Param("id") UUID id, @Param("user") User user);
}
