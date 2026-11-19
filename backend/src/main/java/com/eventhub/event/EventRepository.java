package com.eventhub.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Override
    @EntityGraph(attributePaths = "organizer", type = EntityGraph.EntityGraphType.LOAD)
    List<Event> findAll(Specification<Event> spec, Sort sort);

    @EntityGraph(attributePaths = "organizer", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Event> findWithOrganizerById(Long id);

    /** Trava a linha do evento para evitar exceder a capacidade em inscrições simultâneas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findForUpdateById(@Param("id") Long id);

    boolean existsByOrganizerId(Long organizerId);

    long countByStatus(EventStatus status);

    long countByStatusAndStartAtAfter(EventStatus status, LocalDateTime after);

    @EntityGraph(attributePaths = "organizer", type = EntityGraph.EntityGraphType.LOAD)
    List<Event> findTop5ByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus status, LocalDateTime after);
}
