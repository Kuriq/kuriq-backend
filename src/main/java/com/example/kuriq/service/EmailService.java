package com.example.kuriq.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

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
}
