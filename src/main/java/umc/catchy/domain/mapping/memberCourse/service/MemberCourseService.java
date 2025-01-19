package umc.catchy.domain.mapping.memberCourse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.catchy.domain.mapping.memberCourse.dao.MemberCourseRepository;
import umc.catchy.domain.mapping.memberCourse.domain.MemberCourse;
import umc.catchy.domain.mapping.memberCourse.dto.response.CourseBookmarkResponse;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.code.BaseErrorCode;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberCourseService {
    public final MemberCourseRepository memberCourseRepository;
    public final MemberRepository memberRepository;

    public CourseBookmarkResponse toggleBookmark(Long courseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() ->new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        MemberCourse memberCourse = memberCourseRepository.findByCourseIdAndMemberId(courseId, currentMember.getId()).orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_MEMBER_NOT_FOUND));
        MemberCourse.toggleBookmark(memberCourse);
        return CourseBookmarkResponse.builder()
                .memberCourseId(memberCourse.getId())
                .bookmarked(memberCourse.isBookmark())
                .build();
    }

    @Transactional(readOnly = true)
    public Slice<MemberCourseResponse> findAllCourseByBookmarked(int pageSize, Long lastCourseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member currentMember = memberRepository.findById(memberId).orElseThrow(() ->new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        return memberCourseRepository.findCourseByBookmarks(currentMember.getId(),pageSize,lastCourseId);
    }
}
