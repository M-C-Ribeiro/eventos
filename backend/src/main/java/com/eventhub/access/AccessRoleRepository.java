package com.eventhub.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AccessRoleRepository extends JpaRepository<AccessRole, Long> {

    List<AccessRole> findAllByOrderBySystemRoleDescNameAsc();

    List<AccessRole> findAllByIdIn(Collection<Long> ids);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select count(u) from User u join u.roles r where r.id = :roleId")
    long countUsers(@Param("roleId") Long roleId);
}
