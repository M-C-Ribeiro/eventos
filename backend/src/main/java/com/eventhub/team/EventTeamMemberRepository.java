package com.eventhub.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventTeamMemberRepository extends JpaRepository<EventTeamMember, Long> {

    @EntityGraph(attributePaths = {"user", "function", "function.permissions"}, type = EntityGraph.EntityGraphType.LOAD)
    List<EventTeamMember> findByEventIdOrderByUserNameAsc(Long eventId);

    @EntityGraph(attributePaths = {"function", "function.permissions"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<EventTeamMember> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByFunctionId(Long functionId);

    long countByFunctionId(Long functionId);

    /** Em quantas equipes a pessoa tem uma função que concede a permissão informada. */
    @Query("""
            select count(m) from EventTeamMember m join m.function f join f.permissions p
            where m.user.id = :userId and p = :permission
            """)
    long countMembershipsWithPermission(@Param("userId") Long userId,
                                        @Param("permission") com.eventhub.access.Permission permission);

    @Modifying
    @Query("delete from EventTeamMember m where m.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("delete from EventTeamMember m where m.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
