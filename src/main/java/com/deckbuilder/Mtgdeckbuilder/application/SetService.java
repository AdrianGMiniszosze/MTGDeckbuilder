package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Set;

import java.util.List;
import java.util.Optional;

public interface SetService {
	List<Set> findAll(int pageSize, int pageNumber);
	Optional<Set> findById(Long id);
	Set create(Set set);
	Set update(Long id, Set set);
	void deleteById(Long id);
}
