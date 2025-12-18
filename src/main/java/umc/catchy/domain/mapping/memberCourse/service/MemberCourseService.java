package umc.catchy.domain.mapping.memberCourse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.CourseBookmarkResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.dto.SliceResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberCourseService {

    private final MemberCourseRepository memberCourseRepository;
    private final MemberRepository memberRepository;

    public CourseBookmarkResponse toggleBookmark(Long courseId) {
        Member currentMember = getCurrentMember();
        MemberCourse memberCourse = memberCourseRepository.findByCourseIdAndMemberId(courseId, currentMember.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_MEMBER_NOT_FOUND));

        memberCourse.toggleBookmark();

        return new CourseBookmarkResponse(
                memberCourse.getId(),
                memberCourse.isBookmark()
        );
    }

    @Transactional(readOnly = true)
    public SliceResponse<MemberCourseResponse> findAllCourseByBookmarked(int pageSize, Long lastCourseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Slice<MemberCourseResponse> courseByBookmarks = memberCourseRepository.findCourseByBookmarks(memberId, pageSize, lastCourseId);
        return SliceResponse.of(courseByBookmarks);
    }

    @Transactional(readOnly = true)
    public SliceResponse<MemberCourseResponse> getMemberCourses(CourseType courseType, String upperLocation,
                                                                String lowerLocation, Long lastId) {
        Member member = getCurrentMember();

        Slice<MemberCourseResponse> responses = memberCourseRepository.findCourseByFilters(
                courseType, upperLocation, lowerLocation, member.getId(), lastId
        );

        return SliceResponse.of(responses);
    }

    private Member getCurrentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
