package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.model.Set;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.SetEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.SetEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.SetRepository;
import com.deckbuilder.Mtgdeckbuilder.application.SetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SetServiceImpl implements SetService {
    private final SetRepository setRepository;
    private final SetEntityMapper setEntityMapper;

    @Override
    public List<Set> findAll(int pageSize, int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return setEntityMapper.toModelList(setRepository.findAll(pageable).getContent());
    }

    @Override
    public Optional<Set> findById(Long id) {
        return setRepository.findById(id).map(setEntityMapper::toModel);
    }

    public Optional<Set> findByName(String name) {
        return setRepository.findByName(name).map(setEntityMapper::toModel);
    }

    @Override
    public Set create(Set set) {
        SetEntity entity = setEntityMapper.toEntity(set);
        entity = setRepository.save(entity);
        return setEntityMapper.toModel(entity);
    }

    @Override
    public Set update(Long id, Set set) {
        if (!setRepository.existsById(id)) {
            throw new IllegalArgumentException("Set not found with id: " + id);
        }
        SetEntity entity = setEntityMapper.toEntity(set);
        entity.setId(id);
        entity = setRepository.save(entity);
        return setEntityMapper.toModel(entity);
    }

    @Override
    public void deleteById(Long id) {
        setRepository.deleteById(id);
    }
}