package com.eventhub.demand;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DemandAssignmentRepository extends JpaRepository<DemandAssignment, Long> {

    /** Atribuições de todas as demandas de um evento, já com função e pessoa carregadas. */
    @EntityGraph(attributePaths = {"function", "user"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("select a from DemandAssignment a where a.demand.event.id = :eventId")
    List<DemandAssignment> findByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("delete from DemandAssignment a where a.demand.id = :demandId")
    void deleteByDemandId(@Param("demandId") Long demandId);

    @Modifying
    @Query("delete from DemandAssignment a where a.demand.id in (select d.id from Demand d where d.event.id = :eventId)")
    void deleteByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("delete from DemandAssignment a where a.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from DemandAssignment a where a.function.id = :functionId")
    void deleteByFunctionId(@Param("functionId") Long functionId);

    /** Quem sai da equipe de um evento deixa de responder pelas demandas atribuídas a ele nesse evento. */
    @Modifying
    @Query("""
            delete from DemandAssignment a
            where a.user.id in :userIds
              and a.demand.id in (select d.id from Demand d where d.event.id = :eventId)
            """)
    void deleteUserAssignmentsInEvent(@Param("eventId") Long eventId, @Param("userIds") Collection<Long> userIds);
}
