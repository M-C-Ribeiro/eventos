package com.eventhub.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    List<ScheduleItem> findByEventIdOrderByDateAscTimeAscIdAsc(Long eventId);

    @Modifying
    @Query("delete from ScheduleItem s where s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
