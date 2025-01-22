package umc.catchy.domain.activetime.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.activetime.domain.ActiveTime;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface ActiveTimeRepository extends JpaRepository<ActiveTime, Long> {
    Optional<ActiveTime> findByDayOfWeekAndStartTimeAndEndTime(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);
}
