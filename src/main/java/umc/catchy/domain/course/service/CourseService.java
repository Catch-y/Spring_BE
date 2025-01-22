package umc.catchy.domain.course.service;

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
import umc.catchy.domain.category.dao.CategoryRepository;
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
import umc.catchy.domain.mapping.memberActivetime.dao.MemberActiveTimeRepository;
import umc.catchy.domain.mapping.memberCategory.dao.MemberCategoryRepository;
import umc.catchy.domain.mapping.memberCourse.converter.MemberCourseConverter;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.mapping.memberLocation.dao.MemberLocationRepository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
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
    private CategoryRepository categoryRepository;

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
    public Slice<MemberCourseResponse> getMemberCourses(String type, String upperLocation, String lowerLocation, Long lastId) {
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

        List<Course> courses = courseRepository.findCourses(courseType, upperLocation, lowerLocation, member, lastId);

        // 페이징 설정
        Pageable pageable = PageRequest.of(0, 10);

        // courses가 11개면 다음 페이지가 있음
        boolean hasNext = courses.size() > pageable.getPageSize();

        if (hasNext) {
            courses.remove(courses.size() - 1);
        }

        List<MemberCourseResponse> responses = courses.stream()
                .sorted(Comparator.comparing(Course::getCreatedDate).reversed())
                .map(course -> {
                    List<BigCategory> categories = getCategories(course);
                    return MemberCourseConverter.toMemberCourseResponse(course, categories);
                }).toList();

        return new SliceImpl<>(responses, pageable, hasNext);
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

        // 관심 지역 및 장소 조회
        List<MemberLocation> memberLocations = memberLocationRepository.findAllByMemberId(memberId);
        List<String> preferredCategories = getPreferredCategories(memberId);

        if (memberLocations.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO, "관심 지역이 설정되지 않았습니다.");
        }

        // 랜덤 장소 선택
        List<Place> places = placeRepository.findAll(); // 모든 장소 조회
        Collections.shuffle(places);
        List<Place> limitedPlaces = places.subList(0, Math.min(5, places.size()));

        // 지역 정보 생성
        String region = LocationUtils.extractUpperLocation(limitedPlaces.get(0).getRoadAddress()) + " "
                + LocationUtils.extractLowerLocation(limitedPlaces.get(0).getRoadAddress());

        // GPT 호출 및 응답 처리
        String gptPrompt = buildGptPrompt(limitedPlaces, region, preferredCategories);
        String gptResponse = callOpenAiApi(gptPrompt);

        // GPT 응답 파싱
        GptCourseInfoResponse parsedResponse = parseGptResponseToDto(gptResponse);

        // 데이터베이스 저장 로직 추가
        saveCourseAndPlaces(parsedResponse, memberId);

        // 반환
        return parsedResponse;
    }

    private void saveCourseAndPlaces(GptCourseInfoResponse parsedResponse, Long memberId) {
        // 현재 사용자 가져오기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 추천 시간 파싱
        Pair<LocalTime, LocalTime> recommendTime = parseRecommendTime(parsedResponse.getRecommendTime());

        // 코스 저장
        Course course = Course.builder()
                .courseName(parsedResponse.getCourseName())
                .courseDescription(parsedResponse.getCourseDescription())
                .courseType(CourseType.AI_GENERATED)
                .recommendTimeStart(recommendTime.getLeft())
                .recommendTimeEnd(recommendTime.getRight())
                .member(member)
                .build();
        courseRepository.save(course);

        // 디버깅 로그
        System.out.println("Saved Course: " + course.getCourseName());

        // 장소-코스 관계 저장
        int order = 1;
        for (GptCourseInfoResponse.GptPlaceInfoResponse placeInfo : parsedResponse.getPlaceInfos()) {
            // Place 조회
            Place place = placeRepository.findById(placeInfo.getPlaceId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND, "장소를 찾을 수 없습니다: " + placeInfo.getPlaceId()));

            // PlaceCourse 생성 및 저장
            PlaceCourse placeCourse = PlaceCourse.builder()
                    .course(course)
                    .place(place)
                    .placeOrder(order++)
                    .build();
            placeCourseRepository.save(placeCourse);

            // 디버깅 로그
            System.out.println("Saved PlaceCourse: " + place.getPlaceName() + " (Order: " + (order - 1) + ")");
        }
    }

    // GPT 응답에서 recommendTime 파싱
    private Pair<LocalTime, LocalTime> parseRecommendTime(String recommendTime) {
        try {
            String[] times = recommendTime.split("~");
            LocalTime startTime = LocalTime.parse(times[0].trim());
            LocalTime endTime = times[1].equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(times[1].trim());
            return Pair.of(startTime, endTime);
        } catch (DateTimeParseException e) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO, "시간 형식 파싱 실패: " + recommendTime);
        }
    }


    private LocalTime parseTime(String time) {
        // 24:00 처리
        if ("24:00".equals(time)) {
            return LocalTime.MIDNIGHT; // LocalTime.MIDNIGHT은 00:00을 의미
        }
        return LocalTime.parse(time);
    }

    private List<String> getPreferredCategories(Long memberId) {
        return memberCategoryRepository.findByMemberId(memberId).stream()
                .map(memberCategory -> memberCategory.getCategory().getName())
                .toList();
    }

    private String buildGptPrompt(List<Place> places, String region, List<String> preferredCategories) {
        StringBuilder prompt = new StringBuilder();

        if ("전체 지역".equals(region)) {
            prompt.append("Create a full-day itinerary for all regions. ");
        } else {
            prompt.append(String.format("Create a full-day itinerary for the region '%s'. ", region));
        }

        prompt.append("Use only the following places in the itinerary:\n");

        for (Place place : places) {
            prompt.append(String.format("- Place ID: %d, Name: %s, Road Address: %s, Operating Hours: %s, Category: %s, Description: %s\n",
                    place.getId(), place.getPlaceName(), place.getRoadAddress(), place.getActiveTime(),
                    place.getCategory().getName(), place.getPlaceDescription()));
        }

        prompt.append("\nThe user's preferred categories are: ");
        prompt.append(String.join(", ", preferredCategories));
        prompt.append(". Prioritize places that match these categories but include diverse options for a complete experience.\n");

        prompt.append("\n반환 결과는 JSON 형식이어야 하며, 코스 이름과 설명은 반드시 한국어로 작성해 주세요. 예시는 다음과 같습니다:\n");
        prompt.append("{\n");
        prompt.append("  \"courseName\": \"string\",\n");
        prompt.append("  \"courseDescription\": \"string\",\n");
        prompt.append("  \"recommendTime\": \"HH:mm~HH:mm\",\n");
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

        prompt.append("Ensure the response adheres strictly to this JSON format without additional comments or text.");
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

                // 장소 정보 추출
                List<GptCourseInfoResponse.GptPlaceInfoResponse> places = new ArrayList<>();
                JsonNode placesNode = contentNode.path("places");
                if (placesNode.isArray()) {
                    for (JsonNode placeNode : placesNode) {
                        GptCourseInfoResponse.GptPlaceInfoResponse place = new GptCourseInfoResponse.GptPlaceInfoResponse();

                        // `placeId`를 강제로 숫자로 변환
                        try {
                            place.setPlaceId(placeNode.path("placeId").asLong());
                        } catch (NumberFormatException e) {
                            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR,
                                    "Invalid placeId format: " + placeNode.path("placeId").asText());
                        }

                        place.setName(placeNode.path("name").asText());
                        place.setRoadAddress(placeNode.path("roadAddress").asText());
                        place.setOperatingHours(placeNode.path("operatingHours").asText());
                        place.setRecommendVisitTime(placeNode.path("recommendVisitTime").asText());
                        places.add(place);
                    }
                }

                // DTO 생성
                GptCourseInfoResponse response = new GptCourseInfoResponse();
                response.setCourseName(courseName);
                response.setCourseDescription(courseDescription);
                response.setRecommendTime(recommendTime);
                response.setPlaceInfos(places);

                return response;
            } else {
                throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR, "GPT 응답 형식이 잘못되었습니다.");
            }
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR, "JSON 파싱 실패: " + e.getMessage());
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

            System.out.println("GPT Response: " + response.getBody()); // 디버깅용
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error while calling OpenAI API: " + e.getMessage());
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO, "GPT API 호출 실패");
        }
    }
}
