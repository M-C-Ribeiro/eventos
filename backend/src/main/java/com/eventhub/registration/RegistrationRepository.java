package com.eventhub.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByEventIdOrderByNameAsc(Long eventId);

    Optional<Registration> findByEventIdAndEmailIgnoreCase(Long eventId, String email);

    long countByEventIdAndStatusNot(Long eventId, RegistrationStatus status);

    long countByStatusNot(RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    @Query("""
            select r.event.id, count(r) from Registration r
            where r.event.id in :eventIds and r.status <> :excluded
            group by r.event.id
            """)
    List<Object[]> countActiveByEventIds(@Param("eventIds") Collection<Long> eventIds,
                                         @Param("excluded") RegistrationStatus excluded);

    @Modifying
    @Query("delete from Registration r where r.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("update Registration r set r.user = null where r.user.id = :userId")
    void detachUser(@Param("userId") Long userId);
}
