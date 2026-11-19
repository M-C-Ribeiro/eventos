package com.eventhub.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TeamFunctionRepository extends JpaRepository<TeamFunction, Long> {

    List<TeamFunction> findAllByOrderByNameAsc();

    List<TeamFunction> findAllByIdIn(Collection<Long> ids);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
