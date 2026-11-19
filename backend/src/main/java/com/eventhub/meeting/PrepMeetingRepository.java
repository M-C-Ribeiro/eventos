package com.eventhub.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrepMeetingRepository extends JpaRepository<PrepMeeting, Long> {

    List<PrepMeeting> findByEventIdOrderByDateAscTimeAscIdAsc(Long eventId);

    @Modifying
    @Query("delete from PrepMeeting m where m.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
