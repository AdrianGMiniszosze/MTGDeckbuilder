package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.SetService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.SetRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.SetNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.SetEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.SetEntity;
import com.deckbuilder.mtgdeckbuilder.model.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SetServiceImpl implements SetService {
	private final SetRepository setRepository;
	private final SetEntityMapper setEntityMapper;

	@Override
	public List<Set> findAll(int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.setEntityMapper.toModelList(this.setRepository.findAll(pageable).getContent());
	}

	@Override
	public Optional<Set> findById(Long id) {
		return this.setRepository.findById(id).map(this.setEntityMapper::toModel);
	}

	public Optional<Set> findByName(String name) {
		return this.setRepository.findByName(name).map(this.setEntityMapper::toModel);
	}

	@Override
	@Transactional
	public Set create(Set set) {
		SetEntity entity = this.setEntityMapper.toEntity(set);
		entity = this.setRepository.save(entity);
		return this.setEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public Set update(Long id, Set set) {
		if (!this.setRepository.existsById(id)) {
			throw new SetNotFoundException(id);
		}
		SetEntity entity = this.setEntityMapper.toEntity(set);
		entity.setId(id);
		entity = this.setRepository.save(entity);
		return this.setEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		this.setRepository.deleteById(id);
	}
}