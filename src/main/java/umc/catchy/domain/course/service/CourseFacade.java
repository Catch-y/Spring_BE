package umc.catchy.domain.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.dto.request.CourseCreateRequest;
import umc.catchy.domain.course.dto.request.CourseUpdateRequest;
import umc.catchy.domain.course.dto.response.CourseDetailResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseFacade {

    private static final String COURSE_IMAGE_PREFIX = "course-images/";
    private static final String TIME_FORMAT = "HH:mm";

    private final CourseCommandService commandService;
    private final CourseService courseService;
    private final MemberRepository memberRepository;
    private final AmazonS3Manager s3Manager;

    public CourseDetailResponse createCourse(CourseCreateRequest request) {
        Member member = getCurrentMember();

        String courseImageUrl = null;
        if (request.courseImage() != null) {
            String keyName = COURSE_IMAGE_PREFIX + UUID.randomUUID();
            courseImageUrl = s3Manager.uploadFile(keyName, request.courseImage());
        }

        try {
            Course course = request.toEntity(member, courseImageUrl);
            Course savedCourse = commandService.saveCourse(course);
            commandService.registerPlacesToCourse(savedCourse, request.placeIds());
            commandService.saveMemberCourse(savedCourse, member);

            return courseService.getCourseDetails(savedCourse.getId());

        } catch (Exception e) {
            if (courseImageUrl != null) {
                s3Manager.deleteImage(courseImageUrl);
            }
            throw e;
        }
    }

    public CourseDetailResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = courseService.getCourse(courseId);
        Member member = getCurrentMember();

        if (!course.getMember().equals(member)) {
            throw new GeneralException(ErrorStatus.COURSE_INVALID_MEMBER);
        }

        String newImageUrl = null;
        String oldImageUrl = null;

        if (request.courseImage() != null) {
            oldImageUrl = course.getCourseImage();

            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                s3Manager.deleteImage(oldImageUrl);
            }

            String keyName = COURSE_IMAGE_PREFIX + UUID.randomUUID();
            newImageUrl = s3Manager.uploadFile(keyName, request.courseImage());
        }

        try {
            LocalTime startTime = null;
            LocalTime endTime = null;

            if (!request.recommendTimeStart().isEmpty() && !request.recommendTimeEnd().isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
                startTime = LocalTime.parse(request.recommendTimeStart(), formatter);
                endTime = LocalTime.parse(request.recommendTimeEnd(), formatter);
            }

            commandService.updateCourse(
                    course,
                    request.courseName(),
                    request.courseDescription(),
                    newImageUrl,
                    request.placeIds(),
                    startTime,
                    endTime
            );

            return courseService.getCourseDetails(courseId);

        } catch (Exception e) {
            if (newImageUrl != null) {
                s3Manager.deleteImage(newImageUrl);
                log.error("코스 수정 실패. 업로드된 이미지 삭제: {}", newImageUrl);
            }
            throw e;
        }
    }

    public void deleteCourse(Long courseId) {
        Course course = courseService.getCourse(courseId);
        Member member = getCurrentMember();

        commandService.deleteCourse(course, member);
    }

    private Member getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
