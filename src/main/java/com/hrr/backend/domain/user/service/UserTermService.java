package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserTermRequestDto;
import com.hrr.backend.domain.user.entity.User;

public interface UserTermService {

    void saveUserTerms(User user, UserTermRequestDto.AgreeRequest request);
}
