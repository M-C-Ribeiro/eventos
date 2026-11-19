package com.eventhub.meeting;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingAttendanceRepository extends JpaRepository<MeetingAttendance, Long> {

    @EntityGraph(attributePaths = "user", type = EntityGraph.EntityGraphType.LOAD)
    List<MeetingAttendance> findByMeetingId(Long meetingId);

    Optional<MeetingAttendance> findByMeetingIdAndUserId(Long meetingId, Long userId);

    /** Todos os registros de presença (presentes) das reuniões de um evento. */
    @Query("select a from MeetingAttendance a where a.meeting.event.id = :eventId and a.present = true")
    List<MeetingAttendance> findPresentByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("delete from MeetingAttendance a where a.meeting.id = :meetingId")
    void deleteByMeetingId(@Param("meetingId") Long meetingId);

    @Modifying
    @Query("delete from MeetingAttendance a where a.meeting.id in (select m.id from PrepMeeting m where m.event.id = :eventId)")
    void deleteByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("delete from MeetingAttendance a where a.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
