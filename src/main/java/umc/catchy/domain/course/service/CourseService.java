package umc.catchy.domain.course.service;

import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.activetime.domain.ActiveTime;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.course.converter.CourseConverter;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.request.CourseCreateRequest;
import umc.catchy.domain.course.dto.request.CourseUpdateRequest;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.courseReview.dao.CourseReviewRepository;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseSliceResponse;
import umc.catchy.domain.location.domain.Location;
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;
import umc.catchy.domain.mapping.memberStyle.dao.MemberStyleRepository;
import umc.catchy.domain.mapping.placeCourse.dao.PlaceCourseRepository;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;
import umc.catchy.domain.mapping.placeVisit.dao.PlaceVisitRepository;
import umc.catchy.domain.mapping.placeVisit.domain.PlaceVisit;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.place.converter.PlaceConverter;
import umc.catchy.domain.place.dao.PlaceRepository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {

    @Value("${openai.model}")
    private String openAiModel;

    @Value("${openai.api.img-url}")
    private String dallEApiUrl;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final PlaceCourseRepository placeCourseRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final MemberRepository memberRepository;
    private final MemberCourseRepository memberCourseRepository;
    private final AmazonS3Manager amazonS3Manager;
    private final PlaceRepository placeRepository;
    private final MemberLocationRepository memberLocationRepository;
    private final MemberActiveTimeRepository memberActiveTimeRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberStyleRepository memberStyleRepository;

    private Course getCourse(Long courseId){
        return courseRepository.findById(courseId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    //코스의 각 장소 별 간단한 정보 받아오기
    private List<CourseInfoResponse.getPlaceInfoOfCourseDTO> getPlaceListOfCourse(Course course, Member member){
        return placeCourseRepository.findAllByCourse(course).stream()
                .map(placeCourse -> {
                    // 멤버의 장소 방문 여부 확인
                    Boolean isVisited = placeVisitRepository.findByPlaceAndMember(placeCourse.getPlace(), member)
                            .map(PlaceVisit::isVisited)
                            .orElse(false); // null -> 기본값 false
                    return PlaceConverter.toPlaceInfoOfCourseDTO(placeCourse.getPlace(), isVisited);
                })
                .collect(Collectors.toList());
    }

    //Course : 리뷰 개수 로직
    private Integer calculateNumberOfReviews(Course course){
        if(!course.isHasReview()){
            return 0;
        }
        else{
            return courseReviewRepository.countAllByCourse(course);
        }
    }

    //Course : 추천 시간대 String 변환
    private String getRecommendTimeToString(Course course) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime startTime = course.getRecommendTimeStart() != null ? course.getRecommendTimeStart() : LocalTime.of(9, 0);
        LocalTime endTime = course.getRecommendTimeEnd() != null ? course.getRecommendTimeEnd() : LocalTime.of(21, 0);

        return startTime.format(formatter) + " ~ " + endTime.format(formatter);
    }

    //코스의 상세 정보 받아오기
    public CourseInfoResponse.getCourseInfoDTO getCourseDetails(Long courseId) {
        Course course = getCourse(courseId);
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseConverter.toCourseInfoDTO(course, calculateNumberOfReviews(course), getRecommendTimeToString(course), placeListOfCourse);
    }

    // 현재 사용자의 코스를 불러옴
    public MemberCourseSliceResponse getMemberCourses(String type, String upperLocation, String lowerLocation, Long lastId) {
        CourseType courseType;

        if ("AI".equals(type)) {
            courseType = CourseType.AI_GENERATED;
        } else if ("DIY".equals(type)) {
            courseType = CourseType.USER_CREATED;
        } else {
            throw new GeneralException(ErrorStatus.INVALID_COURSE_TYPE);
        }

        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Slice<MemberCourseResponse> responses = memberCourseRepository.findCourseByFilters(courseType, upperLocation, lowerLocation, memberId, lastId);

        return MemberCourseSliceResponse.from(responses);
    }

    // 코스 수정
    public CourseInfoResponse.getCourseInfoDTO updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = getCourse(courseId);

        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 사용자가 가지고 있는 코스인지 검증
        memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));

        // 코스 이름 수정
        if (!request.getCourseName().isEmpty()) {
            course.setCourseName(request.getCourseName());
        }

        // 코스 설명 수정
        if (!request.getCourseDescription().isEmpty()) {
            course.setCourseDescription(request.getCourseDescription());
        }

        // 코스 이미지 수정
        if (request.getCourseImage() != null) {
            String originCourseImageUrl = course.getCourseImage();

            if (!originCourseImageUrl.isEmpty())
                amazonS3Manager.deleteImage(originCourseImageUrl);

            MultipartFile newCourseImage = request.getCourseImage();

            String keyName = "course-images/" + UUID.randomUUID();
            String newCourseImageUrl = amazonS3Manager.uploadFile(keyName, newCourseImage);

            course.setCourseImage(newCourseImageUrl);
        }

        // 코스 장소 수정
        if (!request.getPlaceIds().isEmpty()) {
            List<Long> placeIds = request.getPlaceIds();

            // 기존의 장소는 제거
            List<PlaceCourse> originPlaces = placeCourseRepository.findAllByCourse(course);
            placeCourseRepository.deleteAll(originPlaces);

            // 코스에 추가
            IntStream.range(0, placeIds.size()).forEach(index -> {
                Long placeId = placeIds.get(index);
                Place place = placeRepository.findById(placeId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

                // List의 Index를 기반으로 코스 순서 결정
                PlaceCourse newPlaceCourse = PlaceCourse.builder()
                        .course(course)
                        .place(place)
                        .placeOrder(index + 1)
                        .build();

                placeCourseRepository.save(newPlaceCourse);
            });
        }

        // 추천 시간대 수정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        if (!request.getRecommendTimeStart().isEmpty()) {
            course.setRecommendTimeStart(LocalTime.parse(request.getRecommendTimeStart(), formatter));
        }

        if (!request.getRecommendTimeEnd().isEmpty()) {
            course.setRecommendTimeEnd(LocalTime.parse(request.getRecommendTimeEnd(), formatter));
        }

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseConverter.toCourseInfoDTO(course, calculateNumberOfReviews(course), getRecommendTimeToString(course), placeListOfCourse);
    }

    public void deleteCourse(Long courseId) {
        Course course = getCourse(courseId);

        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 사용자가 가지고 있는 코스인지 검증
        MemberCourse memberCourse = memberCourseRepository.findByCourseAndMember(course, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER));

        memberCourseRepository.delete(memberCourse);

        // 코스의 장소들 삭제
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);
        placeCourseRepository.deleteAll(placeCourses);

        // 코스 삭제
        courseRepository.delete(course);
    }

    // 코스 생성(DIY)
    public CourseInfoResponse.getCourseInfoDTO createCourseByDIY(CourseCreateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 이미지 불러오기
        String courseImageUrl = "";

        if (request.getCourseImage() != null) {
            MultipartFile courseImage = request.getCourseImage();

            String keyName = "course-images/" + UUID.randomUUID();
            courseImageUrl = amazonS3Manager.uploadFile(keyName, courseImage);
        }

        // 코스 생성
        Course course = CourseConverter.toCourse(request, courseImageUrl, member);
        course.setCourseType(CourseType.USER_CREATED);

        // PlaceCourse 생성
        List<Long> placeIds = request.getPlaceIds();

        IntStream.range(0, placeIds.size()).forEach(index -> {
            Long placeId = placeIds.get(index);
            Place place = placeRepository.findById(placeId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

            // List의 Index를 기반으로 코스 순서 결정
            PlaceCourse newPlaceCourse = PlaceCourse.builder()
                    .course(course)
                    .place(place)
                    .placeOrder(index + 1)
                    .build();

            placeCourseRepository.save(newPlaceCourse);
        });

        // MemberCourse 생성
        MemberCourse memberCourse = MemberCourse.builder()
                .course(course)
                .member(member)
                .build();

        memberCourseRepository.save(memberCourse);

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseConverter.toCourseInfoDTO(course, calculateNumberOfReviews(course), getRecommendTimeToString(course), placeListOfCourse);
    }

    private List<BigCategory> getCategories(Course course) {
        List<PlaceCourse> placeCourses = placeCourseRepository.findAllByCourse(course);

        return placeCourses.stream()
                .map(PlaceCourse::getPlace)
                .map(Place::getCategory)
                .map(Category::getBigCategory)
                .distinct()
                .toList();
    }

    public GptCourseInfoResponse generateCourseAutomatically() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        // 관심 지역 및 선호 카테고리 조회
        List<MemberLocation> memberLocations = memberLocationRepository.findAllByMemberId(memberId);
        List<String> preferredCategories = getPreferredCategories(memberId);
        List<String> userStyles = getUserStyles(memberId);
        List<String> activeTimes = getUserActiveTimes(memberId);

        if (memberLocations.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }

        // 관심 지역 리스트 생성
        List<String> regionList = memberLocations.stream()
                .map(memberLocation -> {
                    Location location = memberLocation.getLocation();
                    return LocationUtils.extractUpperLocation(location.getUpperLocation()) + " " +
                            LocationUtils.extractLowerLocation(location.getLowerLocation());
                })
                .collect(Collectors.toList());

        List<Place> places = placeRepository.findAll();

        // GPT 프롬프트 생성 및 호출
        String gptPrompt = buildGptPrompt(regionList, places, preferredCategories, userStyles, activeTimes);
        String gptResponse = callOpenAiApi(gptPrompt);

        // 응답 파싱
        GptCourseInfoResponse parsedResponse = parseGptResponseToDto(gptResponse);

        // 이미지 생성 및 업로드
        String courseImage = generateAndUploadCourseImage(
                parsedResponse.getCourseName(),
                parsedResponse.getCourseDescription()
        );
        parsedResponse.setCourseImage(courseImage);

        // 데이터베이스 저장
        saveCourseAndPlaces(parsedResponse, memberId);

        return parsedResponse;
    }

    public List<String> getUserStyles(Long memberId) {
        return memberStyleRepository.findByMemberId(memberId).stream()
                .map(memberStyle -> memberStyle.getStyle().getName().name())
                .collect(Collectors.toList());
    }

    private void saveCourseAndPlaces(GptCourseInfoResponse parsedResponse, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Pair<LocalTime, LocalTime> recommendTime = parseRecommendTime(parsedResponse.getRecommendTime());

        Course course = Course.builder()
                .courseName(parsedResponse.getCourseName())
                .courseDescription(parsedResponse.getCourseDescription())
                .courseType(CourseType.AI_GENERATED)
                .recommendTimeStart(recommendTime.getLeft())
                .recommendTimeEnd(recommendTime.getRight())
                .courseImage(parsedResponse.getCourseImage())
                .participantsNumber(0L)
                .member(member)
                .build();
        courseRepository.save(course);

        int order = 1;
        double totalRating = 0.0;
        int placeCount = 0;

        for (GptCourseInfoResponse.GptPlaceInfoResponse placeInfo : parsedResponse.getPlaceInfos()) {
            Place place = placeRepository.findById(placeInfo.getPlaceId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

            PlaceCourse placeCourse = PlaceCourse.builder()
                    .course(course)
                    .place(place)
                    .placeOrder(order++)
                    .build();
            placeCourseRepository.save(placeCourse);

            // 평점 계산 (리뷰가 없는 경우 제외)
            if (place.getRating() != null && place.getRating() > 0) {
                totalRating += place.getRating();
                placeCount++;
            }
        }

        // 코스 평점 계산 (소수 둘째 자리 반올림)
        double courseRating = placeCount > 0 ? totalRating / placeCount : 0.0;
        courseRating = Math.round(courseRating * 10) / 10.0;

        course.setRating(courseRating);
        courseRepository.save(course);

        parsedResponse.setCourseRating(courseRating);
    }

    // GPT 응답에서 recommendTime 파싱
    private Pair<LocalTime, LocalTime> parseRecommendTime(String recommendTime) {
        try {
            String[] times = recommendTime.split("~");
            LocalTime startTime = LocalTime.parse(times[0].trim());
            LocalTime endTime = times[1].equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(times[1].trim());
            return Pair.of(startTime, endTime);
        } catch (DateTimeParseException e) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }
    }

    private List<String> getPreferredCategories(Long memberId) {
        return memberCategoryRepository.findByMemberId(memberId).stream()
                .map(memberCategory -> memberCategory.getCategory().getName())
                .toList();
    }

    public List<String> getUserActiveTimes(Long memberId) {
        return memberActiveTimeRepository.findByMemberId(memberId).stream()
                .map(memberActiveTime -> {
                    ActiveTime activeTime = memberActiveTime.getActiveTime();
                    return activeTime.getDayOfWeek().toString() + " " +
                            activeTime.getStartTime().toString() + "~" +
                            activeTime.getEndTime().toString();
                })
                .collect(Collectors.toList());
    }

    private String buildGptPrompt(List<String> regionList, List<Place> places, List<String> preferredCategories, List<String> userStyles, List<String> activeTimes) {
        StringBuilder prompt = new StringBuilder();

        // 지역 정보
        if (regionList.isEmpty() || regionList.contains("전체 지역")) {
            prompt.append("Create a full-day itinerary for all regions and suggest places to visit. ");
        } else {
            prompt.append("Create a full-day itinerary for the following regions: ");
            prompt.append(String.join(", ", regionList));
            prompt.append(". All places in the itinerary **must strictly belong to the same region** ");
            prompt.append("(UpperLocation and LowerLocation). Do not include places from different regions.\n");
        }

        // 선호 카테고리
        prompt.append("The user's preferred categories are: ");
        prompt.append(String.join(", ", preferredCategories));
        prompt.append(". Please recommend places that align with these preferences while ensuring diversity in the suggested itinerary.\n");

        // 관심 스타일 추가
        prompt.append("The user prefers the following styles: ");
        prompt.append(String.join(", ", userStyles));
        prompt.append(". Please take these styles into account when recommending the itinerary.\n");

        // 선호 시간대 추가
        prompt.append("The user's preferred active times are: ");
        prompt.append(String.join(", ", activeTimes));
        prompt.append(". Please generate a recommendTime that aligns with these active times.\n");

        prompt.append("Ensure that the itinerary includes at least 2 places and at most 5 places.\n");

        // 장소 상세 정보
        for (Place place : places) {
            prompt.append(String.format(
                    "- Place ID: %d, Name: %s, Road Address: %s, Operating Hours: %s, Category: %s, Description: %s\n",
                    place.getId(),
                    place.getPlaceName(),
                    place.getRoadAddress(),
                    place.getActiveTime(),
                    place.getCategory().getName(),
                    place.getPlaceDescription()
            ));
        }

        // 응답 형식
        prompt.append("\nThe course name and description must be written in Korean.\n");
        prompt.append("The course description should be concise, no more than 80 characters.\n");
        prompt.append("Please generate a course name and description that fits the selected places and reflects the user's preferred styles.\n");
        prompt.append("The response should include a course name, course description, recommended visit time for each place, and the full list of recommended places in the region.\n");
        prompt.append("Please strictly return the response in the following JSON format. Do not include any extra text, comments, or explanations outside the JSON structure:\n");
        prompt.append("The response should include a field `courseImage` with a URL to the generated image.\n");
        prompt.append("{\n");
        prompt.append("  \"courseName\": \"string (in Korean)\",\n");
        prompt.append("  \"courseDescription\": \"string (in Korean)\",\n");
        prompt.append("  \"recommendTime\": \"HH:mm~HH:mm\",\n");
        prompt.append("  \"courseImage\": \"string (image URL)\",\n");
        prompt.append("  \"courseRating\": \"numeric (0.0~5.0)\",\n");
        prompt.append("  \"places\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"placeId\": \"numeric\",\n");
        prompt.append("      \"name\": \"string\",\n");
        prompt.append("      \"roadAddress\": \"string\",\n");
        prompt.append("      \"operatingHours\": \"HH:mm-HH:mm\",\n");
        prompt.append("      \"recommendVisitTime\": \"HH:mm~HH:mm\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("Return only this JSON structure, with no additional text.");
        return prompt.toString();
    }

    private GptCourseInfoResponse parseGptResponseToDto(String gptResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(gptResponse);

            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                String content = choicesNode.get(0).path("message").path("content").asText();
                JsonNode contentNode = objectMapper.readTree(content);

                // 코스 정보 추출
                String courseName = contentNode.path("courseName").asText("AI 추천 코스");
                String courseDescription = contentNode.path("courseDescription").asText("AI가 추천한 여행 코스입니다.");
                String recommendTime = contentNode.path("recommendTime").asText("09:00~21:00");
                String courseImage = contentNode.path("courseImage").asText("");
                Double courseRating = contentNode.path("courseRating").asDouble(0.0);

                // 장소 정보 추출
                List<GptCourseInfoResponse.GptPlaceInfoResponse> places = new ArrayList<>();
                JsonNode placesNode = contentNode.path("places");
                if (placesNode.isArray()) {
                    for (JsonNode placeNode : placesNode) {
                        GptCourseInfoResponse.GptPlaceInfoResponse place = new GptCourseInfoResponse.GptPlaceInfoResponse();

                        try {
                            place.setPlaceId(placeNode.path("placeId").asLong());
                        } catch (NumberFormatException e) {
                            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR,
                                    "Invalid placeId format: " + placeNode.path("placeId").asText());
                        }

                        place.setName(placeNode.path("name").asText());
                        place.setRoadAddress(placeNode.path("roadAddress").asText());
                        place.setRecommendVisitTime(placeNode.path("recommendVisitTime").asText());
                        places.add(place);
                    }
                }

                // DTO 생성
                GptCourseInfoResponse response = new GptCourseInfoResponse();
                response.setCourseName(courseName);
                response.setCourseDescription(courseDescription);
                response.setRecommendTime(recommendTime);
                response.setCourseImage(courseImage);
                response.setCourseRating(courseRating);
                response.setPlaceInfos(places);

                return response;
            } else {
                throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR);
            }
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR);
        }
    }

    private String callOpenAiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a travel itinerary assistant."),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 1000
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = new RestTemplate()
                    .postForEntity(openAiApiUrl, request, String.class);

            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error while calling OpenAI API: " + e.getMessage());
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }
    }

    private String generateCourseImage(String courseName, String courseDescription) {
        try {
            // 프롬프트 작성: 코스 이름과 설명 기반
            String prompt = String.format(
                    "Create a visually balanced and centered image that represents the theme of the course: '%s'. " +
                            "Ensure the image utilizes the full canvas (e.g., not just the top area) and reflects the course's description: '%s'. " +
                            "The image should be artistic and visually appealing for a 512x512 size.",
                    courseName,
                    courseDescription
            );

            // DALL-E API 요청 헤더 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            // DALL-E 요청 바디
            Map<String, Object> requestBody = Map.of(
                    "prompt", prompt,
                    "n", 1,
                    "size", "512x512"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // DALL-E API 호출
            ResponseEntity<String> response = new RestTemplate()
                    .postForEntity(dallEApiUrl, request, String.class);

            // 응답 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode imageUrlNode = rootNode.path("data").get(0).path("url");

            if (imageUrlNode.isMissingNode()) {
                throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
            }

            return imageUrlNode.asText();
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }
    }

    private String uploadImageToS3(String imageUrl) {
        try {
            // 이미지 다운로드
            InputStream inputStream = new URL(imageUrl).openStream();

            // 이미지 메타데이터 설정
            String contentType = "image/png";
            long contentLength = inputStream.available();

            // S3에 업로드
            String keyName = "course-images/" + UUID.randomUUID() + ".png";
            amazonS3Manager.uploadInputStream(keyName, inputStream, contentType, contentLength);

            // 업로드된 S3 URL 반환
            return amazonS3Manager.getFileUrl(keyName);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.IMAGE_GENERATION_ERROR);
        }
    }

    public String generateAndUploadCourseImage(String courseName, String courseDescription) {
        // DALL-E API로 이미지 생성
        String dallEImageUrl = generateCourseImage(courseName, courseDescription);

        // 생성된 이미지를 S3에 업로드
        return uploadImageToS3(dallEImageUrl);
    }
}
