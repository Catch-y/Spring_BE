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
import umc.catchy.domain.course.dao.CourseRepository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.dto.request.CourseCreateRequest;
import umc.catchy.domain.course.dto.request.CourseUpdateRequest;
import umc.catchy.domain.course.dto.response.CourseInfoResponse;
import umc.catchy.domain.course.dto.response.GPTPlaceDTO;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public CourseInfoResponse.getCourseInfoDTO generateCourseAutomatically() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        // 1. 모든 관심 지역 조회
        List<MemberLocation> memberLocations = memberLocationRepository.findAllByMemberId(memberId);
        if (memberLocations.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO, "사용자의 관심 지역이 설정되지 않았습니다.");
        }
        List<String> preferredCategories = getPreferredCategories(memberId);

        // 2. 랜덤으로 관심 지역 선택
        Random random = new Random();
        MemberLocation selectedRegion = memberLocations.get(random.nextInt(memberLocations.size()));
        String region = selectedRegion.getLocation().getUpperLocation() + " " + selectedRegion.getLocation().getLowerLocation();
        System.out.println("Selected Region: " + region); // 디버깅용

        // 3. 관심 지역 기반 장소 조회
        List<Place> places = placeRepository.findPlacesByRegion(
                selectedRegion.getLocation().getUpperLocation(),
                selectedRegion.getLocation().getLowerLocation()
        );

        // 장소가 없으면 예외 처리
        if (places.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO, "추천할 장소가 없습니다.");
        }

        // 4. 장소 순서 랜덤 섞기
        Collections.shuffle(places);

        // 최소 2개에서 최대 5개 사이의 랜덤 개수로 장소 선택
        int minPlaces = 2;
        int maxPlaces = Math.min(5, places.size()); // 최대 장소 개수는 전체 장소 개수와 5 중 더 작은 값
        int randomPlacesCount = new Random().nextInt((maxPlaces - minPlaces) + 1) + minPlaces; // 최소 2개에서 최대 maxPlaces 개

        List<Place> limitedPlaces = places.subList(0, randomPlacesCount);

        // 디버깅 로그
        System.out.println("Limited Places: " + limitedPlaces.stream().map(Place::getPlaceName).toList());

        // 5. GPT 프롬프트 생성 및 호출
        String gptPrompt = buildGptPrompt(places, region, preferredCategories); // preferredCategories 전달
        String gptResponse = callOpenAiApi(gptPrompt);

        // 6. GPT 응답 파싱
        Pair<LocalTime, LocalTime> recommendTime = parseRecommendTime(gptResponse);
        List<Long> matchedPlaceIds = parseGptResponseAndMatchPlaceIds(gptResponse, limitedPlaces);

        // 7. 현재 사용자 가져오기
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 8. 코스 생성 및 저장
        Course course = Course.builder()
                .courseName(region + " AI 추천 코스")
                .courseDescription("사용자의 지역을 기반으로 생성된 추천 코스")
                .courseType(CourseType.AI_GENERATED)
                .recommendTimeStart(recommendTime.getLeft())
                .recommendTimeEnd(recommendTime.getRight())
                .member(member)
                .build();
        courseRepository.save(course);

        // 9. place_course 저장
        IntStream.range(0, matchedPlaceIds.size())
                .forEach(index -> {
                    Long placeId = matchedPlaceIds.get(index);

                    PlaceCourse placeCourse = PlaceCourse.builder()
                            .course(course)
                            .place(placeRepository.findById(placeId)
                                    .orElseThrow(() -> new GeneralException(ErrorStatus.PLACE_NOT_FOUND)))
                            .placeOrder(index + 1)
                            .build();

                    placeCourseRepository.save(placeCourse);
                });

        // 10. 반환 데이터 구성
        List<CourseInfoResponse.getPlaceInfoOfCourseDTO> placeListOfCourse = matchedPlaceIds.stream()
                .map(placeId -> PlaceConverter.toPlaceInfoOfCourseDTO(
                        placeRepository.findById(placeId).orElse(null), false))
                .toList();

        return CourseConverter.toCourseInfoDTO(course, matchedPlaceIds.size(), getRecommendTimeToString(course), placeListOfCourse);
    }

    // GPT 응답에서 recommendTime 파싱
    private Pair<LocalTime, LocalTime> parseRecommendTime(String gptResponse) {
        try {
            System.out.println("Raw GPT Response: " + gptResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(gptResponse);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();
            System.out.println("Extracted Content: " + content);

            String recommendTimeRegex = "\\*\\*RecommendTime:\\s*(\\d{2}:\\d{2})~(\\d{2}:\\d{2})\\*\\*";
            Pattern recommendTimePattern = Pattern.compile(recommendTimeRegex);
            Matcher recommendTimeMatcher = recommendTimePattern.matcher(content);

            LocalTime earliest = LocalTime.MAX;
            LocalTime latest = LocalTime.MIN;

            // 각 장소의 Recommend Time 파싱
            while (recommendTimeMatcher.find()) {
                // parseTime 메서드를 사용하여 24:00 처리
                LocalTime start = parseTime(recommendTimeMatcher.group(1));
                LocalTime end = parseTime(recommendTimeMatcher.group(2));

                if (start.isBefore(earliest)) {
                    earliest = start;
                }
                if (end.isAfter(latest)) {
                    latest = end;
                }
            }

            // Recommend Time이 없을 경우 기본값 사용
            if (earliest.equals(LocalTime.MAX) || latest.equals(LocalTime.MIN)) {
                System.out.println("Recommend Time not found. Using default: 09:00~21:00.");
                earliest = LocalTime.of(9, 0); // 기본 시작 시간
                latest = LocalTime.of(21, 0); // 기본 종료 시간
            }

            return Pair.of(earliest, latest);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR,
                    "JSON 파싱 실패: " + e.getMessage());
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR,
                    "Recommend Time 파싱 실패: " + e.getMessage());
        }
    }

    private LocalTime parseTime(String time) {
        // 24:00 처리
        if ("24:00".equals(time)) {
            return LocalTime.MIDNIGHT; // LocalTime.MIDNIGHT은 00:00을 의미
        }
        return LocalTime.parse(time);
    }

    // GPT 응답 내용 파싱
    private List<Long> parseGptResponseAndMatchPlaceIds(String response, List<Place> filteredPlaces) {
        try {
            System.out.println("Raw GPT Response: " + response);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);

            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode content = choices.get(0).path("message").path("content");
                System.out.println("Extracted Content: " + content.asText());

                List<Long> gptPlaceIds = parsePlaceIdsFromContent(content.asText());

                Set<Long> validPlaceIds = filteredPlaces.stream()
                        .map(Place::getId)
                        .collect(Collectors.toSet());

                List<Long> matchedPlaceIds = gptPlaceIds.stream()
                        .filter(validPlaceIds::contains)
                        .toList();

                if (matchedPlaceIds.size() < 2) {
                    System.out.println("Matched places are less than 2. Adjusting to default places.");
                    // 디폴트 값 설정 또는 예외 처리 대신 기본 로직으로 대체
                    matchedPlaceIds = filteredPlaces.stream()
                            .map(Place::getId)
                            .limit(2) // 최소 2개를 선택
                            .toList();
                }
                return matchedPlaceIds.stream().limit(5).toList();
            }

            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR, "GPT 응답 형식이 잘못되었습니다.");
        } catch (JsonProcessingException e) {
            System.err.println("Invalid JSON structure: " + e.getMessage());
            System.err.println("Response content: " + response);
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR, "잘못된 JSON 형식입니다: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            System.err.println("Response content: " + response);
            throw new GeneralException(ErrorStatus.JSON_PARSING_ERROR, "JSON 파싱 실패: " + e.getMessage());
        }

    }

    private List<Long> parsePlaceIdsFromContent(String content) {
        List<Long> placeIds = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- Place ID:")) {
                try {
                    Long placeId = Long.parseLong(line.substring(11).trim());
                    placeIds.add(placeId);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Place ID: " + line);
                }
            }
        }
        return placeIds;
    }

    private List<String> getPreferredCategories(Long memberId) {
        return memberCategoryRepository.findByMemberId(memberId).stream()
                .map(memberCategory -> memberCategory.getCategory().getName())
                .toList();
    }

    private String buildGptPrompt(List<Place> places, String region, List<String> preferredCategories) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Create a full-day itinerary for the region '%s'. ", region));
        prompt.append("Use only the following places in the itinerary:\n");

        for (Place place : places) {
            prompt.append(String.format("- Place ID: %d, Name: %s, Road Address: %s, Operating Hours: %s, Category: %s, Description: %s\n",
                    place.getId(), place.getPlaceName(), place.getRoadAddress(), place.getActiveTime(),
                    place.getCategory().getName(), place.getPlaceDescription()));
        }

        prompt.append("\nThe user's preferred categories are: ");
        prompt.append(String.join(", ", preferredCategories));
        prompt.append(". Prioritize places that match these categories, but also include diverse options for a complete experience.\n");

        prompt.append("\nThe itinerary should include the following details for each location, strictly following this format:\n");
        prompt.append("- Place ID: [numeric ID]\n");
        prompt.append("- Name: [Place name]\n");
        prompt.append("- Road Address: [Road address]\n");
        prompt.append("- Operating Hours: HH:mm-HH:mm\n");
        prompt.append("- Recommend Visit Time: HH:mm~HH:mm\n");
        prompt.append("\nAdditionally, provide the overall start and end times for the entire day in this format:\n");
        prompt.append("**RecommendTime: HH:mm~HH:mm**\n");
        prompt.append("\nEnsure that:\n");
        prompt.append("1. All times strictly follow the HH:mm format.\n");
        prompt.append("2. Use `~` as the separator for Recommend Visit Time and overall RecommendTime.\n");
        prompt.append("3. Do not include any additional comments or extra information.\n");
        prompt.append("4. The response should be structured and clean, strictly adhering to the format above.\n");
        prompt.append("\nArrange the places in an order that reflects the typical flow of a day, starting from morning to late night.\n");
        prompt.append("Return the data in a structured and easy-to-read format.");
        return prompt.toString();
    }

    private List<GPTPlaceDTO> filterValidPlaces(List<GPTPlaceDTO> gptPlaces) {
        return gptPlaces.stream()
                .filter(gptPlace -> placeRepository.existsById(gptPlace.getId()))
                .toList();
    }

    private String extractRegionFromRoadAddress(String roadAddress) {
        if (roadAddress == null || roadAddress.isEmpty()) {
            return null;
        }

        String[] parts = roadAddress.split(" ");
        return parts.length > 0 ? parts[0] : null;
    }

    private String extractCityFromRoadAddress(String roadAddress) {
        if (roadAddress == null || roadAddress.isEmpty()) {
            return null;
        }

        String[] parts = roadAddress.split(" ");
        return parts.length > 1 ? parts[1] : null;
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
            throw new GeneralException(ErrorStatus.INVALID_REQUEST_INFO, "GPT API 호출 실패");
        }
    }
}
