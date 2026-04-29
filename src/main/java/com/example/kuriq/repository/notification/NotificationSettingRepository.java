package com.example.kuriq.repository.notification;

import com.example.kuriq.entity.notification.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

// PK = userId (users와 1:1). findById(userId)로 바로 조회.
// 회원가입 시 NotificationSetting.createDefault(userId)로 자동 생성.
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, String> {}

