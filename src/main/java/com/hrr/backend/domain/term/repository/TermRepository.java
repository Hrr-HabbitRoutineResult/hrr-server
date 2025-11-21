package com.hrr.backend.domain.term.repository;

import com.hrr.backend.domain.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermRepository extends JpaRepository<Term, Long> {
    List<Term> findAllByOrderByIdAsc();
}
