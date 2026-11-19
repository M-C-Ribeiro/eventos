package com.eventhub.demand;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DemandCompletionRepository extends JpaRepository<DemandCompletion, Long> {

    @Query("select c from DemandCompletion c where c.demand.event.id = :eventId")
    List<DemandCompletion> findByEventId(@Param("eventId") Long eventId);

    Optional<DemandCompletion> findByDemandIdAndUserId(Long demandId, Long userId);

    @Modifying
    @Query("delete from DemandCompletion c where c.demand.id = :demandId")
    void deleteByDemandId(@Param("demandId") Long demandId);

    @Modifying
    @Query("delete from DemandCompletion c where c.demand.id in (select d.id from Demand d where d.event.id = :eventId)")
    void deleteByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("delete from DemandCompletion c where c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /** Ao excluir quem registrou conclusões de outras pessoas, as conclusões continuam valendo. */
    @Modifying
    @Query("update DemandCompletion c set c.markedBy = null where c.markedBy.id = :userId")
    void clearMarkedBy(@Param("userId") Long userId);
}
