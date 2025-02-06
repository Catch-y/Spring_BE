package umc.catchy.domain.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.activetime.dao.ActiveTimeRepository;
import umc.catchy.domain.activetime.domain.ActiveTime;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.location.dao.LocationRepository;
import umc.catchy.domain.location.domain.Location;
import umc.catchy.domain.location.dto.request.LocationSurveyRequest;
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberActivetime.domain.MemberActiveTime;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberCategory.domain.MemberCategory;
import umc.catchy.domain.mapping.memberCategory.dto.response.MemberCategoryCreatedResponse;
import umc.catchy.domain.mapping.memberCategoryVote.dao.MemberCategoryVoteRepository;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberGroup.dao.MemberGroupRepository;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;
import umc.catchy.domain.mapping.memberLocation.dto.response.MemberLocationCreatedResponse;
import umc.catchy.domain.mapping.memberPlaceVote.dao.MemberPlaceVoteRepository;
import umc.catchy.domain.mapping.memberStyle.dao.MemberStyleRepository;
import umc.catchy.domain.mapping.memberStyle.domain.MemberStyle;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.NicknameRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.request.StyleAndActiveTimeSurveyRequest;
import umc.catchy.domain.member.dto.request.*;
import umc.catchy.domain.member.dto.response.*;
import umc.catchy.domain.style.dao.StyleRepository;
import umc.catchy.domain.style.domain.Style;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.JwtUtil;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final StyleRepository styleRepository;
    private final ActiveTimeRepository activeTimeRepository;
    private final MemberActiveTimeRepository memberActiveTimeRepository;
    private final MemberStyleRepository memberStyleRepository;
    private final LocationRepository locationRepository;
    private final MemberLocationRepository memberLocationRepository;

    private final AmazonS3Manager s3Manager;
    private final JwtUtil jwtUtil;
    private final MemberPlaceVoteRepository memberPlaceVoteRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final MemberCategoryVoteRepository memberCategoryVoteRepository;

    @Value("${security.kakao.client-id}")
    private String KAKAO_CLIENT_ID;

    @Value("${security.kakao.client-secret}")
    private String KAKAO_CLIENT_SECRET;

    @Value("${security.kakao.redirect-url}")
    private String KAKAO_REDIRECT_URL;

    @Value("${security.kakao.token-request-url}")
    private String KAKAO_TOKEN_URL;

    @Value("${security.kakao.info-request-url}")
    private String KAKAO_INFO_URL;

    @Value("${security.apple.key-id}")
    private String APPLE_KEY_ID;

    @Value("${security.apple.service-id}")
    private String APPLE_CLIENT_ID;

    @Value("${security.apple.team-id}")
    private String APPLE_TEAM_ID;

    @Value("${security.apple.redirect-url}")
    private String APPLE_REDIRECT_URL;

    @Value("${security.apple.request-url}")
    private String APPLE_REQUEST_URL;

    @Value("${security.apple.private-key}")
    private String APPLE_PRIVATE_KEY;

    public SignUpResponse signUp(SignUpRequest request, MultipartFile profileImage, SocialType socialType) {
        String accessToken = request.accessToken();

        String profileImageUrl = null;

        // 프로필 이미지 url 생성
        if (profileImage != null) {
            String keyName = "profile-images/" + UUID.randomUUID();
            profileImageUrl = s3Manager.uploadFile(keyName, profileImage);
        }

        // 유저 정보 받아오기
        Map<String, String> info = new HashMap<>();
        if (socialType == SocialType.KAKAO) info = getKakaoInfo(accessToken);
        else if (socialType == SocialType.APPLE) info = getAppleInfo(accessToken);

        String providerId = info.get("providerId");
        String email = info.get("email");

        // providerId 중복 확인
        memberRepository.findByProviderId(info.get("providerId"))
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.PROVIDER_ID_DUPLICATE);
                });

        // 이메일 중복 확인
        memberRepository.findByEmail(info.get("email"))
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.EMAIL_DUPLICATE);
                });

        // 닉네임 중복 확인
        memberRepository.findByNickname(request.nickname())
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE);
                });

        Member newMember = createMemberEntity(providerId, email, request.nickname(), profileImageUrl, socialType);

        String authorizationCode = request.authorizationCode();

        // 애플 회원가입이면 인가 코드를 저장
        if (socialType == SocialType.APPLE) {

            if (authorizationCode == null) throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);

            newMember.setAuthorizationCode(authorizationCode);
        }

        // 토큰 생성
        newMember.setAccessToken(jwtUtil.createAccessToken(newMember.getEmail()));
        newMember.setRefreshToken(jwtUtil.createRefreshToken(newMember.getEmail()));

        memberRepository.save(newMember);

        return SignUpResponse.of(newMember);
    }

    public LoginResponse login(LoginRequest request, SocialType socialType) {
        String token = request.accessToken();

        // 멤버 조회
        Optional<Member> optionalMember = getMemberByTokenAndSocialType(token, socialType);

        // 계정이 존재하지 않을 경우
        Member member = optionalMember.orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 로그인 성공 시 토큰 생성
        String accessToken = jwtUtil.createAccessToken(member.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(member.getEmail());

        member.setAccessToken(accessToken);
        member.setRefreshToken(refreshToken);

        return LoginResponse.of(member, accessToken, refreshToken);
    }

    public ReIssueTokenResponse validateRefreshToken() {
        String refreshToken = SecurityUtil.extractRefreshToken();

        if (refreshToken == null) throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);

        System.out.println(refreshToken);

        // 리프레시 토큰 만료 검사
        boolean isValid = jwtUtil.validateToken(refreshToken);

        // 등록된 유저가 아니면 예외 처리
        Member member = memberRepository.findByRefreshToken(refreshToken).orElseThrow(() ->
            new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));


        // 리프레시 토큰이 유효할 때
        if (isValid) {
            // 액세스 토큰 재발급
            String newAccessToken = jwtUtil.createAccessToken(member.getEmail());
            String newRefreshToken = jwtUtil.createRefreshToken(member.getEmail());

            member.setAccessToken(newAccessToken);
            member.setRefreshToken(newRefreshToken);

            return ReIssueTokenResponse.of(newAccessToken, newRefreshToken);
        }
        // 리프레시 토큰이 만료되었을 때
        else throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
    }

    public String getKakaoAccessToken (String code) {
        String access_Token = "";
        String refresh_Token = "";
        String reqURL = KAKAO_TOKEN_URL;

        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //POST 요청을 위해 기본값이 false인 setDoOutput을 true로
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            //POST 요청에 필요로 요구하는 파라미터 스트림을 통해 전송
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=" + KAKAO_CLIENT_ID);
            sb.append("&client_secret=" + KAKAO_CLIENT_SECRET);
            sb.append("&redirect_uri=" + KAKAO_REDIRECT_URL);
            sb.append("&code=" + code);
            bw.write(sb.toString());
            bw.flush();

            //결과 코드가 200이라면 성공
            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            if (responseCode != 200) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String errorLine = "";
                StringBuilder errorResult = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResult.append(errorLine);
                }
                System.out.println("Error response body: " + errorResult.toString());
                errorReader.close();
            }

            //요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            //Gson 라이브러리에 포함된 클래스로 JSON파싱 객체 생성
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            access_Token = element.getAsJsonObject().get("access_token").getAsString();
            refresh_Token = element.getAsJsonObject().get("refresh_token").getAsString();

            System.out.println("access_token : " + access_Token);
            System.out.println("refresh_token : " + refresh_Token);

            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return access_Token;
    }

    public ProfileResponse getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        return ProfileResponse.of(member);
    }

    public NicknameResponse updateNickname(NicknameRequest request) {
        // 닉네임 중복 검사
        memberRepository.findByNickname(request.nickname())
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE);
                });

        Long memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 닉네임 변경
        member.setNickname(request.nickname());

        return NicknameResponse.of(member);
    }

    public ProfileImageResponse updateProfileImage(MultipartFile newProfileImage) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String originProfileImageUrl = member.getProfileImage();

        // 기존에 프로필 사진이 있었다면 제거
        if (originProfileImageUrl != null) {
            s3Manager.deleteImage(originProfileImageUrl);
        }

        // 프로필 사진 url 생성
        String keyName = "profile-images/" + UUID.randomUUID();
        String newProfileImageUrl = s3Manager.uploadFile(keyName, newProfileImage);

        // 이미지 변경
        member.setProfileImage(newProfileImageUrl);

        return ProfileImageResponse.of(member);
    }

    public void validateNickname(NicknameRequest request) {
        String nickname = request.nickname();

        // 닉네임 중복 검사
        memberRepository.findByNickname(nickname)
                .ifPresent(member -> {
                    throw new GeneralException(ErrorStatus.NICKNAME_DUPLICATE);
                });
    }

    public void withdraw(String authorizationCode) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (member.getSocialType() == SocialType.APPLE) {

            if (authorizationCode == null) {
                throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);
            }

            try {
                appleWithdraw(authorizationCode);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new GeneralException(ErrorStatus.APPLE_WITHDRAW_FAILED);
            } catch (net.minidev.json.parser.ParseException e) {
                throw new RuntimeException(e);
            }
        }

        // 연관 엔티티 삭제
        memberStyleRepository.deleteAllByMember(member);
        memberPlaceVoteRepository.deleteAllByMember(member);
        memberCategoryVoteRepository.deleteAllByMember(member);
        memberActiveTimeRepository.deleteAllByMember(member);
        memberLocationRepository.deleteAllByMember(member);
        memberGroupRepository.deleteAllByMember(member);
        memberCourseRepository.deleteAllByMember(member);
        memberCategoryRepository.deleteAllByMember(member);

        memberRepository.delete(member);
    }

    public void logout() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        member.setAccessToken(null);
        member.setRefreshToken(null);
        member.deleteFcmToken(member.getFcmInfo());
    }

    private Optional<Member> getMemberByTokenAndSocialType(String token, SocialType socialType) {
        Map<String, String> info = new HashMap<>();

        if (socialType == SocialType.KAKAO) info = getKakaoInfo(token);
        else if (socialType == SocialType.APPLE) info = getAppleInfo(token);

        return memberRepository.findByEmailAndProviderId(info.get("email"), info.get("providerId"));

    }

    private Map<String, String> getAppleInfo(String token) {
        Map<String, String> info = new HashMap<>();

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            ReadOnlyJWTClaimsSet getPayload = signedJWT.getJWTClaimsSet();

            ObjectMapper objectMapper = new ObjectMapper();
            JSONObject payload = objectMapper.readValue(getPayload.toJSONObject().toJSONString(), JSONObject.class);

            String providerId = String.valueOf(payload.get("sub"));
            String email = String.valueOf(payload.get("email"));

            info.put("providerId", providerId);
            info.put("email", email);

        } catch (ParseException | JsonProcessingException exception) {
            exception.printStackTrace();
        }

        return info;
    }

    private Map<String, String> getKakaoInfo(String token) {
        String postURL = KAKAO_INFO_URL;
        Map<String, String> info = new HashMap<>();

        try {
            URL url = new URL(postURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Authorization", "Bearer " + token);

            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            StringBuilder result = new StringBuilder();

            while ((line = br.readLine()) != null) {
                result.append(line);
            }
            System.out.println("response body : " + result);

            JsonElement element = JsonParser.parseString(result.toString());
            String providerId = String.valueOf(element.getAsJsonObject().get("id"));

            JsonObject kakaoAccount = element.getAsJsonObject().get("kakao_account").getAsJsonObject();
            String email = kakaoAccount.getAsJsonObject().get("email").getAsString();

            info.put("providerId", providerId);
            info.put("email", email);

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return info;
    }

    private Member createMemberEntity(String providerId, String email, String nickname, String profileImageUrl, SocialType socialType) {
        return Member.createMember(
                providerId,
                email,
                nickname,
                profileImageUrl,
                socialType,
                FcmInfo.createFcmInfo());
    }

    private void appleWithdraw(String authorizationCode)
            throws IOException, net.minidev.json.parser.ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObj = (JSONObject) jsonParser.parse(generateAuthToken(authorizationCode));

        String accessToken = String.valueOf(jsonObj.get("access_token"));

        if (accessToken != null) {
            RestTemplate restTemplate = new RestTemplateBuilder().build();
            String revokeUrl = APPLE_REQUEST_URL + "/auth/oauth2/v2/revoke";
            LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", APPLE_CLIENT_ID);
            params.add("client_secret", createClientSecret());
            params.add("token", accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);
            restTemplate.postForEntity(revokeUrl, httpEntity, String.class);
        }
    }

    public String generateAuthToken(String code) throws IOException {
        if (code == null) throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", APPLE_CLIENT_ID);
        params.add("client_secret", createClientSecret());
        params.add("code", code);
        params.add("redirect_uri", APPLE_REDIRECT_URL);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    APPLE_REQUEST_URL + "/auth/token",
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_UNAUTHORIZED);
        }
    }

    private String createClientSecret() throws IOException {
        Date expirationDate = Date.from(LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant());
        Map<String, Object> jwtHeader = new HashMap<>();
        jwtHeader.put("kid", APPLE_KEY_ID);
        jwtHeader.put("alg", "ES256"); // alg

        return Jwts.builder()
                .setHeaderParams(jwtHeader)
                .setIssuer(APPLE_TEAM_ID) // iss
                .setIssuedAt(new Date(System.currentTimeMillis())) // 발행 시간
                .setExpiration(expirationDate) // 만료 시간
                .setAudience(APPLE_REQUEST_URL) // aud
                .setSubject(APPLE_CLIENT_ID) // sub
                .signWith(SignatureAlgorithm.ES256, getPrivateKey())
                .compact();
    }

    private PrivateKey getPrivateKey() throws IOException {
        String privateKey = APPLE_PRIVATE_KEY.replace("\\\\", "\\").replace("\\n", "\n");
        Reader pemReader = new StringReader(privateKey);
        PEMParser pemParser = new PEMParser(pemReader);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        try {
            PrivateKeyInfo object = (PrivateKeyInfo) pemParser.readObject();
            return converter.getPrivateKey(object);
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.AUTHORIZATION_CODE_UNAUTHORIZED);
        }
    }

    public MemberCategoryCreatedResponse createMemberCategory(CategorySurveyRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        List<Category> categories = categoryRepository.findAllByNameIn(request.getCategories());
        List<MemberCategory> collect = categories.stream().map(category -> MemberCategory.createMemberCategory(currentMember, category)).collect(Collectors.toList());
        memberCategoryRepository.saveAll(collect);

        List<Long> memberCategoryIds = collect.stream()
                .map(MemberCategory::getId)
                .toList();

        return new MemberCategoryCreatedResponse(memberCategoryIds);
    }

    public StyleAndActiveTimeSurveyCreatedResponse createStyleAndActiveTimeSurvey(StyleAndActiveTimeSurveyRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        List<Style> styleList = styleRepository.findAllByNameIn(request.getStyleNames());

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<ActiveTime> activeTimeList = request.getActiveTimes().stream().map(activeTime ->
                activeTimeRepository.findByDayOfWeekAndStartTimeAndEndTime(activeTime.getDayOfWeek(),
                                LocalTime.parse(activeTime.getStartTime(),dateTimeFormatter),
                                LocalTime.parse(activeTime.getEndTime(), dateTimeFormatter))
                .orElseGet(() -> activeTimeRepository.save(ActiveTime.createActiveTime(activeTime.getDayOfWeek(),
                        LocalTime.parse(activeTime.getStartTime(),dateTimeFormatter),
                        LocalTime.parse(activeTime.getEndTime(), dateTimeFormatter)))
                )).toList();

        List<MemberStyle> memberStyleList = styleList.stream().map(style -> MemberStyle.createMemberStyle(currentMember, style)).collect(Collectors.toList());
        List<Long> memberStyleIds = saveMemberStyleAndReturnIds(memberStyleList);
        List<MemberActiveTime> memberActiveTimeList = activeTimeList.stream().map(activeTime -> MemberActiveTime.createMemberActiveTime(currentMember, activeTime)).collect(Collectors.toList());
        List<Long> memberActiveTimeIds = saveMemberActiveTimeAndReturnIds(memberActiveTimeList);

        return new StyleAndActiveTimeSurveyCreatedResponse(memberStyleIds, memberActiveTimeIds);
    }

    private List<Long> saveMemberStyleAndReturnIds(List<MemberStyle> memberStyleList) {
        List<MemberStyle> savedEntities = memberStyleRepository.saveAll(memberStyleList);

        return savedEntities.stream()
                .map(MemberStyle::getId) // 저장된 엔티티의 ID 값 추출
                .collect(Collectors.toList());
    }

    private List<Long> saveMemberActiveTimeAndReturnIds(List<MemberActiveTime> memberActiveTimeList) {
        List<MemberActiveTime> savedEntities = memberActiveTimeRepository.saveAll(memberActiveTimeList);

        return savedEntities.stream()
                .map(MemberActiveTime::getId)
                .collect(Collectors.toList());
    }

    public MemberLocationCreatedResponse createMemberLocation(List<LocationSurveyRequest> request) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        List<Location> locationList = request.stream().map(r -> locationRepository.findByUpperLocationAndLowerLocation(r.getUpperLocation(), r.getLowerLocation())
                    .orElseGet(() -> locationRepository.save(Location.createLocation(r.getUpperLocation(), r.getLowerLocation())))
        ).toList();

        List<MemberLocation> memberLocationList = locationList.stream().map(location -> MemberLocation.createMemberLocation(currentMember, location)).collect(Collectors.toList());
        memberLocationRepository.saveAll(memberLocationList);

        List<Long> memberLocationIds = memberLocationList.stream()
                .map(MemberLocation::getId)
                .collect(Collectors.toList());

        return new MemberLocationCreatedResponse(memberLocationIds);
    }

    public void toggleAppAlarm() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        currentMember.toggleAppAlarmState(currentMember.getFcmInfo());
    }

    public void updateFcmToken(UpdateFcmTokenRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        currentMember.updateFcmToken(currentMember.getFcmInfo(),request.getFcmToken());
    }
}

