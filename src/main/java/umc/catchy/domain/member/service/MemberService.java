package umc.catchy.domain.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberCategory.domain.MemberCategory;
import umc.catchy.domain.mapping.memberCategory.dto.response.MemberCategoryCreatedResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.response.LoginResponse;
import umc.catchy.domain.member.dto.response.ReIssueTokenResponse;
import umc.catchy.domain.member.dto.response.SignUpResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.JwtUtil;
import umc.catchy.global.util.SecurityUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final JwtUtil jwtUtil;

    public SignUpResponse signUp(SignUpRequest request, MultipartFile profileImage, SocialType socialType) {
        String accessToken = request.accessToken();

        // 프로필 이미지 url 생성
        String profileImageUrl = "";

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

    public ReIssueTokenResponse reIssue() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 재발급
        String newAccessToken = jwtUtil.createAccessToken(member.getEmail());
        String newRefreshToken = jwtUtil.createRefreshToken(member.getEmail());

        member.setAccessToken(newAccessToken);
        member.setRefreshToken(newRefreshToken);

        return ReIssueTokenResponse.of(newAccessToken, newRefreshToken);
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
        String postURL = "https://kapi.kakao.com/v2/user/me";
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

    public String getKakaoAccessToken (String code) {
        String access_Token = "";
        String refresh_Token = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";

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
            sb.append("&client_id="); // TODO REST_API_KEY 입력
            sb.append("&client_secret="); // TODO SECRET_KEY 입력
            sb.append("&redirect_uri=http://localhost:8080/login/oauth2/code/kakao"); // TODO 인가코드 받은 redirect_uri 입력
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

    private Member createMemberEntity(String providerId, String email, String nickname, String profileImageUrl, SocialType socialType) {
        return Member.createMember(
                providerId,
                email,
                nickname,
                profileImageUrl,
                socialType);
    }

    public MemberCategoryCreatedResponse createMemberCategory(CategorySurveyRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        List<Category> categories = categoryRepository.findAllByNameIn(request.getCategories());
        List<MemberCategory> collect = categories.stream().map(category -> MemberCategory.createMemberCategory(currentMember, category)).collect(Collectors.toList());
        memberCategoryRepository.saveAll(collect);
        return new MemberCategoryCreatedResponse(true,"member`s categories are created");
    }
}
