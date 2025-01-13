package umc.catchy.domain.activetime.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.activetime.domain.ActiveTime;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ActiveTimeRepository extends JpaRepository<ActiveTime, Long> {
    List<ActiveTime> findAllByDayOfWeekInAndStartTimeAndEndTime(List<DayOfWeek> dayOfWeeks, LocalTime startTime, LocalTime endTime);
    boolean existsByDayOfWeekAndStartTimeAndEndTime(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);
}
