package com.hrr.backend.domain.search.service;

import java.util.List;

public interface SearchService {
	void incrementSearchCount(String keyword);

	List<String> getTopNPopularKeywords(int limit);
}
