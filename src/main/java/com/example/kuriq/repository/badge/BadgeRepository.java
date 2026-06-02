package com.example.kuriq.repository.badge;

import com.example.kuriq.entity.badge.Badge;
import com.example.kuriq.entity.badge.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, String> {

    List<Badge> findByUserIdOrderByAcquiredAtDesc(String userId);

    boolean existsByUserIdAndBadgeType(String userId, BadgeType badgeType);

    @Query("""
        SELECT b.badgeType
        FROM Badge b
        WHERE b.userId = :userId
        """)
    List<BadgeType> findBadgeTypesByUserId(@Param("userId") String userId);
}

