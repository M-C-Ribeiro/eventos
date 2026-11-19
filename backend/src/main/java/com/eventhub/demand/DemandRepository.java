package com.eventhub.demand;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DemandRepository extends JpaRepository<Demand, Long> {

    List<Demand> findByEventIdOrderByDueDateAscIdAsc(Long eventId);

    @Modifying
    @Query("delete from Demand d where d.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
