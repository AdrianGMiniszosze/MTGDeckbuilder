package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.TagService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.TagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.TagNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.TagEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.TagEntity;
import com.deckbuilder.mtgdeckbuilder.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
	private final TagRepository tagRepository;
	private final TagEntityMapper tagEntityMapper;

	@Override
	public List<Tag> findAll(int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.tagEntityMapper.toModelList(this.tagRepository.findAll(pageable).getContent());
	}

	@Override
	public Optional<Tag> findById(Long id) {
		return this.tagRepository.findById(id).map(this.tagEntityMapper::toModel);
	}

	public Optional<Tag> findByName(String name) {
		return this.tagRepository.findByName(name).map(this.tagEntityMapper::toModel);
	}

	@Override
	@Transactional
	public Tag create(Tag tag) {
		TagEntity entity = this.tagEntityMapper.toEntity(tag);
		entity = this.tagRepository.save(entity);
		return this.tagEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public Tag update(Long id, Tag tag) {
		if (!this.tagRepository.existsById(id)) {
			throw new TagNotFoundException(id);
		}
		TagEntity entity = this.tagEntityMapper.toEntity(tag);
		entity.setId(id);
		entity = this.tagRepository.save(entity);
		return this.tagEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		this.tagRepository.deleteById(id);
	}
}