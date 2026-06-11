package com.example.kuriq.service;

import com.example.kuriq.dto.user.request.LoginRequest;
import com.example.kuriq.dto.user.request.SignupRequest;
import com.example.kuriq.entity.notification.NotificationSetting;
import com.example.kuriq.entity.user.*;
import com.example.kuriq.repository.notification.NotificationSettingRepository;
import com.example.kuriq.repository.user.*;
import com.example.kuriq.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate; // Redis 조작 도구 (RefreshTokenRepository 대체)
    private final LoginAttemptRepository loginAttemptRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSettingRepository notificationSettingRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;  // 비번 재설정 시 이메일 발송 필드
    private final SocialAccountRepository socialAccountRepository;  // 소셜 계정 연동 정보 저장/조회
    private final RestTemplate restTemplate = new RestTemplate();   // 카카오 API 호출용 HTTP 클라이언트


    // 카카오
    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;      // 카카오 앱 REST API 키 (application.properties에서 주입)

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;   // 카카오 콘솔에 등록한 Redirect URI (application.properties에서 주입)

    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;   // 카카오 클라이언트 시크릿 (토큰 요청 시 보안 검증용)

    // 구글
    @Value("${oauth.google.client-id}")
    private String googleClientId;      // 구글 OAuth 클라이언트 ID

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;  // 구글 OAuth 클라이언트 시크릿

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;   // 구글 콘솔에 등록한 Redirect URI

    // 네이버
    @Value("${oauth.naver.client-id}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret}")
    private String naverClientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    // 회원가입
    public String signup(SignupRequest req) {

        User existing = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (existing != null) {
            if (!existing.getIsDeleted()) {
                // 소셜 계정으로 가입된 경우 별도 안내
                if (existing.getAuthProvider() != User.AuthProvider.LOCAL) {
                    throw new IllegalArgumentException(
                            "이미 " + existing.getAuthProvider() + " 계정으로 가입된 이메일입니다. 해당 계정으로 로그인해 주세요."
                    );
                }

                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
            // soft delete된 계정이면 재활성화
            existing.reactivate(passwordEncoder.encode(req.getPassword()), req.getName());
            return existing.getId();
        }

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .authProvider(User.AuthProvider.LOCAL)
                .isDeleted(false)
                .build();

        userRepository.save(user);
        // 유저가 생성되는 시점인 회원가입 때 알림 설정이 같이 만들어져야 함(1:1관계라서)
        // 즉, 회원가입할 때 notification_settings 테이블에 데이터가 자동으로 들어가야 함
        notificationSettingRepository.save(NotificationSetting.createDefault(user.getId()));
        return user.getId();
    }

    // 로그인 후 Access/Refresh Token 발급
    public String[] loginAndGetTokens(LoginRequest req, String ip) {
        long failCount = loginAttemptRepository.countFailedAttempts(
                req.getEmail(),
                LocalDateTime.now().minusMinutes(30)
        );

        if (failCount >= 5) {
            throw new IllegalStateException("로그인 시도가 너무 많습니다. 30분 후 다시 시도해 주세요.");
        }

        User user = userRepository.findByEmailAndIsDeletedFalse(req.getEmail())
                .orElseThrow(() -> {
                    loginAttemptRepository.save(LoginAttempt.of(req.getEmail(), false, ip));
                    return new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
                });

        // 소셜 전용 계정 체크(소셜 계정은 password가 null이라서 비번 체크 앞에 와야 함)
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new IllegalArgumentException(
                    "이 이메일은 " + user.getAuthProvider() + " 로그인으로 가입되었습니다. "
                            + user.getAuthProvider() + " 로그인을 이용해 주세요."
            );
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            loginAttemptRepository.save(LoginAttempt.of(req.getEmail(), false, ip));
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        loginAttemptRepository.save(LoginAttempt.of(req.getEmail(), true, ip));

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken);

        return new String[]{accessToken, refreshToken};
    }

    // Refresh Token으로 토큰 재발급
    public String[] refresh(String rawToken) {
        String key = "refresh:" + sha256(rawToken);

        // Redis에서 userId 조회 (없으면 만료됐거나 로그아웃된 토큰)
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다. 다시 로그인해 주세요.");
        }

        // 사용된 토큰은 즉시 삭제 (재사용 방지)
        redisTemplate.delete(key);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtProvider.generateAccessToken(user.getId());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), newRefreshToken);

        return new String[]{newAccessToken, newRefreshToken};
    }

    // 로그아웃
    public void logout(String rawToken) {
        String key = "refresh:" + sha256(rawToken);
        redisTemplate.delete(key); // 키 삭제 => 토큰 무효화
    }

    // Refresh Token을 해시로 변환 후 Redis에 저장
    private void saveRefreshToken(String userId, String rawToken) {
        String key = "refresh:" + sha256(rawToken);
        long ttlSeconds = jwtProvider.getRefreshTokenExpiryMs() / 1000; // ms → 초 변환

        // 명령어: SET key userId EX ttlSeconds (TTL 지나면 Redis가 자동 삭제)
        redisTemplate.opsForValue().set(key, userId, Duration.ofSeconds(ttlSeconds));
    }

    // SHA-256 해시 변환
    private String sha256(String input) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(input.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new RuntimeException("해시 생성 실패", e);
        }
    }

    // 비밀번호 재설정 요청
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);

        // 보안상 존재하지 않는 이메일도 성공 응답 반환 (이메일 존재 여부 노출 방지)
        if (user == null) return;

        // 소셜 로그인 전용 계정은 비밀번호 재설정 불가
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new IllegalArgumentException(
                    "이 계정은 " + user.getAuthProvider() + " 로그인으로 가입되었습니다. "
                            + user.getAuthProvider() + "에서 비밀번호를 관리해 주세요."
            );
        }

        // 기존 미사용 토큰 무효화
        passwordResetTokenRepository.invalidatePreviousTokens(user.getId());

        // 새 토큰 생성
        String rawToken = java.util.UUID.randomUUID().toString();
        passwordResetTokenRepository.save(
                PasswordResetToken.create(user.getId(), sha256(rawToken)) // DB에 해시로 저장
        );

        // 이메일 발송
        emailService.sendPasswordResetEmail(email, rawToken);
    }

    // 비밀번호 재설정 확인
    public void confirmPasswordReset(String rawToken, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                // 사용자가 보내온 토큰을 해시 처리해서 DB에 저장된 해시값과 비교해 토큰 찾음
                .findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 토큰입니다.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.changePassword(passwordEncoder.encode(newPassword));  // passwordEncoder로 암호화된 새 비밀번호를 받아 교체
        resetToken.markUsed();  // 재사용 방지
    }

    // 소셜 로그인 인증 URL 생성
    // 프론트가 이 URL로 리다이렉트하면 카카오 로그인 페이지가 뜸
    public String getSocialAuthorizationUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> "https://kauth.kakao.com/oauth/authorize"
                    + "?client_id=" + kakaoClientId          // 카카오 앱 REST API 키
                    + "&redirect_uri=" + kakaoRedirectUri    // 카카오 콘솔에 등록한 Redirect URI
                    + "&response_type=code"                 // 인증 코드 방식
                    + "&state=kakao"  // 콜백에서 provider 구분용
                    + "&prompt=login";  // 매번 카카오 로그인 창 강제

            case "google" -> "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + googleClientId
                    + "&redirect_uri=" + googleRedirectUri
                    + "&response_type=code"
                    + "&scope=email%20profile" // 이메일이랑 프로필 정보 요청
                    + "&state=google"  // 콜백에서 provider 구분용
                    + "&prompt=select_account";  // 매번 구글 계정 선택 창 강제


            case "naver" -> "https://nid.naver.com/oauth2.0/authorize"
                    + "?client_id=" + naverClientId
                    + "&redirect_uri=" + naverRedirectUri
                    + "&response_type=code"
                    + "&state=naver"
                    + "&auth_type=reauthenticate";  // 매번 네이버 로그인 창 강제
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인 provider: " + provider);
        };
    }

    // 소셜 로그인 메인 로직
    // 반환값: [accessToken, refreshToken, isNewUser("true"/"false")]
    public String[] socialLogin(String providerStr, String code) {
        SocialAccount.Provider provider = parseProvider(providerStr); // provider 문자열 → enum 변환

        // 1단계: provider별로 accessToken 및 사용자 정보 조회
        String socialId;
        String email;
        String name;

        if (provider == SocialAccount.Provider.KAKAO) {
            String accessToken = getKakaoAccessToken(code);
            Map<String, Object> userInfo = getKakaoUserInfo(accessToken);
            socialId = String.valueOf(userInfo.get("id"));  // 카카오 고유 사용자 ID
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;  // 이메일 미동의 시 null
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
            name = profile != null ? (String) profile.get("nickname") : "카카오 사용자";  // 닉네임 없으면 기본값

        } else if (provider == SocialAccount.Provider.GOOGLE) {
            String accessToken = getGoogleAccessToken(code);
            Map<String, Object> userInfo = getGoogleUserInfo(accessToken);
            socialId = String.valueOf(userInfo.get("id"));   // 구글 고유 사용자 ID
            email = (String) userInfo.get("email");          // 구글은 기본으로 이메일 제공
            name = (String) userInfo.get("name");            // 구글 사용자 이름
            if (name == null) name = "구글 사용자";

        } else if (provider == SocialAccount.Provider.NAVER) {
            String accessToken = getNaverAccessToken(code);
            Map<String, Object> userInfo = getNaverUserInfo(accessToken);
            // 네이버 응답 구조: { response: { id, email, name, ... } }
            Map<String, Object> naverResponse = (Map<String, Object>) userInfo.get("response");
            if (naverResponse == null) throw new RuntimeException("네이버 사용자 정보 조회 실패");
            socialId = (String) naverResponse.get("id");
            email    = (String) naverResponse.get("email");
            name     = (String) naverResponse.get("name");
            if (name == null) name = "네이버 사용자";

        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 provider: " + providerStr);
        }

        // 2단계: social_accounts 테이블에서 이미 연동된 계정인지 확인
        SocialAccount existing = socialAccountRepository
                .findByProviderAndSocialId(provider, socialId)
                .orElse(null);

        String userId = null;
        boolean isNewUser = false; // 신규 가입 여부 (탈퇴 후 재가입 포함)

        if (existing != null) {
            // 기존 소셜 계정 → 연결된 유저가 탈퇴했는지 확인
            User linkedUser = userRepository.findById(existing.getUserId()).orElse(null);
            if (linkedUser != null && linkedUser.getIsDeleted()) {
                // 탈퇴한 유저의 소셜 계정 → 삭제 후 신규 가입 처리
                socialAccountRepository.delete(existing);
                isNewUser = true;
            } else {
                // 정상 유저 → 바로 로그인
                userId = existing.getUserId();
            }
        }

        if (userId == null) {
            // 신규 계정 → 동일 이메일로 가입된 로컬 계정이 있는지 먼저 확인
            User user = (email != null)
                    ? userRepository.findByEmailAndIsDeletedFalse(email).orElse(null)
                    : null;

            if (user == null) {
                // 완전 새 유저 생성 (소셜 전용 계정은 password = null)
                User.AuthProvider authProvider = switch (provider) {
                    case KAKAO  -> User.AuthProvider.KAKAO;
                    case GOOGLE -> User.AuthProvider.GOOGLE;
                    case NAVER  -> User.AuthProvider.NAVER;
                };

                user = User.builder()
                        .email(email)
                        .password(null)                  // 소셜 전용 계정은 비밀번호 없음
                        .name(name)
                        .authProvider(authProvider)
                        .isDeleted(false)
                        .build();
                userRepository.save(user);
                notificationSettingRepository.save(NotificationSetting.createDefault(user.getId()));  // 알림 설정 기본값 생성
                isNewUser = true; // 완전 신규 유저
            }

            // social_accounts 테이블에 소셜 계정 연동 정보 저장
            socialAccountRepository.save(
                    SocialAccount.create(user.getId(), provider, socialId, email)
            );
            userId = user.getId();
        }

        // 4단계: JWT 발급
        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        saveRefreshToken(userId, refreshToken);  // refreshToken은 해시로 DB 저장

        // tokens[2]: 신규 가입 여부 (프론트에서 안내 메시지 표시용)
        return new String[]{accessToken, refreshToken, String.valueOf(isNewUser)};
    }

    // 카카오 인증 서버에 code를 보내 accessToken 교환
    private String getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);  // 카카오 API는 form 형식 요구

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");  // 인증 코드 방식 고정값
        body.add("client_id", kakaoClientId);          // 카카오 앱 REST API 키
        body.add("redirect_uri", kakaoRedirectUri);    // 콘솔에 등록한 URI와 반드시 동일해야 함
        body.add("code", code);                        // 프론트에서 받아온 인증 코드
        body.add("client_secret", kakaoClientSecret);  // 클라이언트 시크릿 추가 (보안 강화)

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token", request, Map.class);

        if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
            throw new RuntimeException("카카오 토큰 발급 실패");
        }
        return (String) response.getBody().get("access_token");
    }

    // 카카오 API 서버에 accessToken을 보내 사용자 정보 조회
    private Map<String, Object> getKakaoUserInfo(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me", HttpMethod.GET, request, Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        }
        return response.getBody();
    }

    // 구글 code -> accessToken 교환
    private String getGoogleAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");    // 인증 코드 방식 고정값
        body.add("client_id", googleClientId);           // 구글 클라이언트 ID
        body.add("client_secret", googleClientSecret);   // 구글 클라이언트 시크릿
        body.add("redirect_uri", googleRedirectUri);     // 콘솔에 등록한 URI와 반드시 동일해야 함
        body.add("code", code);                          // 프론트에서 받아온 인증 코드

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token", request, Map.class);

        if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
            throw new RuntimeException("구글 토큰 발급 실패");
        }
        return (String) response.getBody().get("access_token");
    }

    // 구글 accessToken -> 사용자 정보 조회
    private Map<String, Object> getGoogleUserInfo(String googleAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo", HttpMethod.GET, request, Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("구글 사용자 정보 조회 실패");
        }
        return response.getBody();
    }

    private String getNaverAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("redirect_uri", naverRedirectUri);
        body.add("code", code);
        body.add("state", "naver");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://nid.naver.com/oauth2.0/token", request, Map.class);

        if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
            throw new RuntimeException("네이버 토큰 발급 실패");
        }
        return (String) response.getBody().get("access_token");
    }

    private Map<String, Object> getNaverUserInfo(String naverAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(naverAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me", HttpMethod.GET, request, Map.class);

        if (response.getBody() == null) throw new RuntimeException("네이버 사용자 정보 조회 실패");
        return response.getBody();
    }

    // provider 문자열 -> SocialAccount.Provider enum 변환
    // "kakao" -> KAKAO, 지원하지 않는 값이면 예외 발생
    private SocialAccount.Provider parseProvider(String provider) {
        try {
            return SocialAccount.Provider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 provider: " + provider);
        }
    }
}
