package com.example.kuriq.service;

import com.example.kuriq.entity.notification.UnsubscribeToken;
import com.example.kuriq.repository.notification.UnsubscribeTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UnsubscribeTokenService unsubscribeTokenService;

    private static final String BASE_URL = "https://kuriq.com";

    // 비밀번호 재설정 이메일 발송
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[큐릭] 비밀번호 재설정 안내");
        message.setText(
                "비밀번호 재설정을 요청하셨습니다.\n\n" +
                        "아래 토큰을 비밀번호 재설정 확인 화면에 입력해 주세요.\n\n" +
                        "토큰: " + rawToken + "\n\n" +
                        "이 토큰은 1시간 후 만료됩니다.\n" +
                        "본인이 요청하지 않으셨다면 이 메일을 무시해 주세요."
        );
        mailSender.send(message);
    }

    // 주간 시작 알림
    public void sendWeeklyStartEmail(String toEmail, String userId, String name,
                                     String courseName, String dashboardUrl) {
        String unsubscribeLink = unsubscribeTokenService.createUnsubscribeLink(userId, UnsubscribeToken.Channel.EMAIL);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[큐릭] 큐리가 이번 주 학습을 준비했어요!");
        message.setText(
                name + "님, 이번 주 학습이 시작됐어요!\n\n" +
                        "\"" + courseName + "\" 부터 시작해 볼까요?\n\n" +
                        "[학습 시작하기 →] " + dashboardUrl + "\n\n" +
                        "---\n" +
                        "알림을 더 이상 받고 싶지 않으시면 아래 링크를 클릭해 주세요.\n" +
                        "수신 거부: " + unsubscribeLink
        );
        mailSender.send(message);
    }

    // 미완료 리마인드 알림
    public void sendIncompleteReminderEmail(String toEmail, String userId, String name,
                                            int remainingCount, String dashboardUrl) {
        String unsubscribeLink = unsubscribeTokenService.createUnsubscribeLink(userId, UnsubscribeToken.Channel.EMAIL);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[큐릭] 이번 주 강좌가 " + remainingCount + "개 남았어요");
        message.setText(
                name + "님, 이번 주 마감이 이틀 남았어요.\n\n" +
                        "남은 강좌 " + remainingCount + "개를 마무리해 볼까요?\n\n" +
                        "[대시보드 확인 →] " + dashboardUrl + "\n\n" +
                        "---\n" +
                        "수신 거부: " + unsubscribeLink
        );
        mailSender.send(message);
    }

    // 장기 미활동 알림
    public void sendInactivityEmail(String toEmail, String userId, String name,
                                    String dashboardUrl) {
        String unsubscribeLink = unsubscribeTokenService.createUnsubscribeLink(userId, UnsubscribeToken.Channel.EMAIL);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[큐릭] 큐리가 기다리고 있어요 🦉");
        message.setText(
                name + "님, 요즘 바쁘셨나요?\n\n" +
                        "잠깐이라도 한 강좌 들어보는 건 어떨까요?\n\n" +
                        "[다시 시작하기 →] " + dashboardUrl + "\n\n" +
                        "---\n" +
                        "수신 거부: " + unsubscribeLink
        );
        mailSender.send(message);
    }

    // 완료 축하 알림
    public void sendCompletionEmail(String toEmail, String userId, String name,
                                    String roadmapTitle, String dashboardUrl) {
        String unsubscribeLink = unsubscribeTokenService.createUnsubscribeLink(userId, UnsubscribeToken.Channel.EMAIL);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[큐릭] 로드맵 완료를 축하해요! 🎓");
        message.setText(
                name + "님, 축하해요!\n\n" +
                        "\"" + roadmapTitle + "\" 로드맵을 모두 이수했어요!\n\n" +
                        "[새 로드맵 만들기 →] " + dashboardUrl + "\n\n" +
                        "---\n" +
                        "수신 거부: " + unsubscribeLink
        );
        mailSender.send(message);
    }
}
