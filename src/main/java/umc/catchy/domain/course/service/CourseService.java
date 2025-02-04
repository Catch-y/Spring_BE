package umc.catchy.domain.course.service;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.activetime.domain.ActiveTime;
import umc.catchy.domain.category.dao.CategoryRepository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.category.domain.Category;
import umc.catchy.domain.course.converter.CourseConverter;
import umc.catchy.domain.course.dto.response.CourseRecommendationResponse;
import umc.catchy.domain.course.dto.response.PopularCourseInfoResponse;
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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@EnableAsync
@Transactional
@RequiredArgsConstructor
public class CourseService {

    @Value("${cache.recommended-courses.key}")
    private String CACHE_KEY;

    @Value("${cache.recommended-courses.ttl}")
    private long CACHE_TTL;

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
    private final CategoryRepository categoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final GPTCourseService gptCourseService;
    @PersistenceContext
    private EntityManager entityManager;

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }

    //코스의 각 장소 별 간단한 정보 받아오기
    private List<CourseInfoResponse.getPlaceInfoOfCourseDTO> getPlaceListOfCourse(Course course, Member member) {
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
    private Integer calculateNumberOfReviews(Course course) {
        if (!course.isHasReview()) {
            return 0;
        } else {
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
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = getPlaceListOfCourse(course, member);
        return CourseConverter.toCourseInfoDTO(course, calculateNumberOfReviews(course), getRecommendTimeToString(course), placeListOfCourse);
    }

    // 현재 사용자의 코스를 불러옴
    public MemberCourseSliceResponse getMemberCourses(String type, String upperLocation, String lowerLocation, Long lastId) {
        CourseType courseType;

        if ("AI".equals(type)) {
            courseType = CourseType.AI;
        } else if ("DIY".equals(type)) {
            courseType = CourseType.DIY;
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
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

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
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

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
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 이미지 불러오기
        String courseImageUrl = null;

        if (request.getCourseImage() != null) {
            MultipartFile courseImage = request.getCourseImage();

            String keyName = "course-images/" + UUID.randomUUID();
            courseImageUrl = amazonS3Manager.uploadFile(keyName, courseImage);
        }

        // 코스 생성
        Course course = CourseConverter.toCourse(request, courseImageUrl, member);
        course.setCourseType(CourseType.DIY);

        List<Long> placeIds = request.getPlaceIds();

        // 평점 계산
        Double averageRating = placeIds.stream()
                .map(placeId -> placeRepository.findById(placeId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND))
                        .getRating())
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        course.setRating(Math.round(averageRating * 10) / 10.0);

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

    public List<Place> getRecommendedPlaces(List<String> regionList, List<Long> preferredCategoryIds, Long memberId, int maxPlaces) {
        List<String> upperRegions = regionList.stream()
                .map(LocationUtils::extractUpperLocation)
                .map(LocationUtils::normalizeLocation)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> lowerRegions = regionList.stream()
                .map(LocationUtils::extractLowerLocation)
                .map(LocationUtils::normalizeLocation)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // QueryDSL 실행
        List<Place> recommendedPlaces = placeRepository.findRecommendedPlaces(preferredCategoryIds, upperRegions, lowerRegions, memberId, maxPlaces);

        // 3. 상위 n개의 데이터에서 랜덤으로 섞기
        int subsetSize = Math.min(3 * maxPlaces, recommendedPlaces.size());
        List<Place> topPlaces = recommendedPlaces.subList(0, subsetSize);
        Collections.shuffle(topPlaces);

        // 4. 최대 maxPlaces만 반환
        List<Place> finalPlaces = topPlaces.stream().limit(maxPlaces).collect(Collectors.toList());

        return finalPlaces;
    }

    public CompletableFuture<GptCourseInfoResponse> generateCourseAutomatically(boolean isForHome) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<MemberLocation> memberLocations = memberLocationRepository.findAllByMemberId(memberId);
        List<String> preferredCategories = getPreferredCategories(memberId);
        List<String> userStyles = getUserStyles(memberId);
        List<String> activeTimes = getUserActiveTimes(memberId);

        if (memberLocations.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO);
        }

        List<String> regionList = memberLocations.stream()
                .map(memberLocation -> {
                    Location location = memberLocation.getLocation();
                    String upper = location.getUpperLocation();
                    String lower = location.getLowerLocation();

                    upper = (upper != null && !upper.equals("전체")) ? upper : null;
                    lower = (lower != null && !lower.equals("전체")) ? lower : null;

                    return upper + (lower != null ? " " + lower : " 전체");
                })
                .collect(Collectors.toList());

        List<Long> preferredCategoryIds = categoryRepository.findIdsByNames(preferredCategories);
        List<Place> places = getRecommendedPlaces(regionList, preferredCategoryIds, memberId, 100);

        // GPT 프롬프트 생성
        String gptPrompt = buildGptPrompt(regionList, places, preferredCategories, userStyles, activeTimes);

        // OpenAI GPT 호출
        CompletableFuture<String> gptResponseFuture = CompletableFuture.supplyAsync(() ->
                gptCourseService.callOpenAiApiAsync(gptPrompt).join()
        );

        // 이미지 생성 및 업로드
        CompletableFuture<String> courseImageFuture = CompletableFuture.supplyAsync(() ->
                gptCourseService.generateAndUploadCourseImageAsync("AI 추천 코스", "AI가 추천한 여행 코스입니다.").join()
        );

        // 두 작업 완료 후 데이터 처리
        return gptResponseFuture.thenCombine(courseImageFuture, (gptResponse, courseImage) -> {
            // GPT 응답 파싱
            GptCourseInfoResponse parsedResponse = parseGptResponseToDto(gptResponse);
            parsedResponse.setCourseImage(courseImage);

            // AI 코스 저장 (홈 추천인지 여부 전달)
            Long courseId = saveCourseAndPlaces(parsedResponse, member, isForHome).join();
            parsedResponse.setCourseId(courseId);

            return parsedResponse;
        }).exceptionally(e -> {
            e.printStackTrace();
            throw new GeneralException(ErrorStatus.GPT_API_CALL_FAILED);
        });
    }

    public List<String> getUserStyles(Long memberId) {
        return memberStyleRepository.findByMemberId(memberId).stream()
                .map(memberStyle -> memberStyle.getStyle().getName().name())
                .collect(Collectors.toList());
    }

    @Async
    @Transactional
    public CompletableFuture<Long> saveCourseAndPlaces(GptCourseInfoResponse parsedResponse, Member member, boolean isForHome) {
        Pair<LocalTime, LocalTime> recommendTime = parseRecommendTime(parsedResponse.getRecommendTime());

        Course course = Course.builder()
                .courseName(parsedResponse.getCourseName())
                .courseDescription(parsedResponse.getCourseDescription())
                .courseType(CourseType.AI)
                .recommendTimeStart(recommendTime.getLeft())
                .recommendTimeEnd(recommendTime.getRight())
                .courseImage(parsedResponse.getCourseImage())
                .participantsNumber(0L)
                .member(member)
                .build();

        // 코스 저장
        Course savedCourse = courseRepository.saveAndFlush(course);

        int order = 1;
        double totalRating = 0.0;
        int placeCount = 0;

        for (GptCourseInfoResponse.GptPlaceInfoResponse placeInfo : parsedResponse.getPlaceInfos()) {
            Place place = placeRepository.findById(placeInfo.getPlaceId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND));

            PlaceCourse placeCourse = PlaceCourse.builder()
                    .course(savedCourse)
                    .place(place)
                    .placeOrder(order++)
                    .build();

            placeCourseRepository.save(placeCourse);

            // 평점 계산
            if (place.getRating() != null && place.getRating() > 0) {
                totalRating += place.getRating();
                placeCount++;
            }
        }

        // 코스 평점 계산
        double courseRating = placeCount > 0 ? totalRating / placeCount : 0.0;
        courseRating = Math.round(courseRating * 10) / 10.0;

        // 평점 저장
        savedCourse.setRating(courseRating);
        courseRepository.saveAndFlush(savedCourse);

        // 🔥 홈 추천 AI 코스가 아니라면 MemberCourse에 저장
        if (!isForHome) {
            MemberCourse memberCourse = MemberCourse.builder()
                    .course(savedCourse)
                    .member(member)
                    .build();
            memberCourseRepository.save(memberCourse);
        }

        return CompletableFuture.completedFuture(savedCourse.getId());
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

    private String buildGptPrompt(
            List<String> regionList,
            List<Place> places,
            List<String> preferredCategories,
            List<String> userStyles,
            List<String> activeTimes
    ) {
        StringBuilder prompt = new StringBuilder();

        // 지역 정보
        prompt.append("Create a full-day itinerary for the following regions: ");
        prompt.append(String.join(", ", regionList));
        prompt.append(". All places in the itinerary **must be chosen strictly from the provided list of places**.\n");

        // 선호 카테고리
        prompt.append("The user's preferred categories are: ");
        prompt.append(String.join(", ", preferredCategories));
        prompt.append(".\n");

        // 사용자 스타일
        if (!userStyles.isEmpty()) {
            prompt.append("The user prefers the following styles: ");
            prompt.append(String.join(", ", userStyles)).append(".\n");
        }

        // 활동 시간대
        if (!activeTimes.isEmpty()) {
            prompt.append("The user's preferred active times are: ");
            prompt.append(String.join(", ", activeTimes)).append(".\n");
        }

        // 장소 정보 추가
        prompt.append("Here are the places to choose from:\n");
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

        // 응답 형식 안내
        prompt.append("\nThe course name and description must be written in Korean.\n");
        prompt.append("The course description should be concise, no more than 80 characters.\n");
        prompt.append("Please generate a course name and description that fits the selected places and reflects the user's preferred styles.\n");
        prompt.append("The response should include a course name, course description, recommended visit time for each place, and the full list of recommended places in the region.\n");
        prompt.append("Please return only the JSON structure below without any additional text, comments, or markdown formatting (e.g., no ```json). Return only the raw JSON structure:\n");
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

    public List<CourseRecommendationResponse> getHomeRecommendedCourses() {
        // 1. 로그인한 사용자 ID 조회
        Long memberId = SecurityUtil.getCurrentMemberId();

        // 사용자별 Redis 캐시 키 생성
        String userSpecificCacheKey = CACHE_KEY + ":" + memberId;

        // 2. Redis에서 사용자별 캐시 데이터 조회
        String cachedData = redisTemplate.opsForValue().get(userSpecificCacheKey);
        if (cachedData != null) {
            return deserializeCourseRecommendations(cachedData);
        }

        // 3. 캐시 데이터가 없으면 새 데이터를 생성
        List<CourseRecommendationResponse> recommendedCourses = generateRecommendedCourses();

        // 4. 생성된 데이터를 Redis에 캐싱
        String serializedData = serializeCourseRecommendations(recommendedCourses);
        redisTemplate.opsForValue().set(userSpecificCacheKey, serializedData, CACHE_TTL, TimeUnit.SECONDS);

        return recommendedCourses;
    }

    public CompletableFuture<List<GptCourseInfoResponse>> generateMultipleAICourses(int count, boolean isForHome) {
        List<CompletableFuture<GptCourseInfoResponse>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            futures.add(generateCourseAutomatically(isForHome));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    private List<CourseRecommendationResponse> generateRecommendedCourses() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        // 사용자가 직접 만든 코스 조회
        List<Course> userCourses = courseRepository.findTop5ByMemberIdAndCourseTypeOrderByCreatedDateDesc(memberId, CourseType.DIY);

        int userCourseCount = userCourses.size();
        int userCourseDeficit = Math.max(0, 5 - userCourseCount);
        int aiCourseCount = 10 - userCourseCount - userCourseDeficit;

        List<CourseRecommendationResponse> recommendedCourses = new ArrayList<>();
        recommendedCourses.addAll(userCourses.stream()
                .map(course -> CourseRecommendationResponse.fromEntity(course, "USER_CREATED"))
                .collect(Collectors.toList()));

        if (aiCourseCount > 0) {
            // AI 코스 생성 (isForHome = true)
            List<GptCourseInfoResponse> aiCourses = generateMultipleAICourses(aiCourseCount, true).join();

            recommendedCourses.addAll(aiCourses.stream()
                    .map(response -> CourseRecommendationResponse.builder()
                            .courseId(response.getCourseId())
                            .courseName(response.getCourseName())
                            .courseDescription(response.getCourseDescription())
                            .courseImage(response.getCourseImage())
                            .courseType("AI")
                            .build())
                    .collect(Collectors.toList()));
        }

        return recommendedCourses;
    }

    private String serializeCourseRecommendations(List<CourseRecommendationResponse> courses) {
        try {
            return objectMapper.writeValueAsString(courses);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize course recommendations", e);
        }
    }

    private List<CourseRecommendationResponse> deserializeCourseRecommendations(String cachedData) {
        try {
            return objectMapper.readValue(cachedData, new TypeReference<List<CourseRecommendationResponse>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize course recommendations", e);
        }
    }

    public List<PopularCourseInfoResponse> getPopularCourses(){
        return CourseConverter.toPopularCourseInfoResponseList(courseRepository.findPopularCourses());
    }
}
