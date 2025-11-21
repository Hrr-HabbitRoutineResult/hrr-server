package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserTerm;
import com.hrr.backend.domain.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserTermRepository extends JpaRepository<UserTerm, Long> {

    Optional<UserTerm> findByUserAndTerm(User user, Term term);

    List<UserTerm> findAllByUser(User user);
}
