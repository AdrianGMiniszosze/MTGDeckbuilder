package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Tag;

import java.util.List;
import java.util.Optional;

public interface TagService {
	List<Tag> findAll(int pageSize, int pageNumber);
	Optional<Tag> findById(Long id);
	Tag create(Tag tag);
	Tag update(Long id, Tag tag);
	void deleteById(Long id);
}
