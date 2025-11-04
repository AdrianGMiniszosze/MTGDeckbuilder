package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.model.Tag;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.TagEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.TagEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.TagRepository;
import com.deckbuilder.Mtgdeckbuilder.application.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TagEntityMapper tagEntityMapper;

    @Override
    public List<Tag> findAll(int pageSize, int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return tagEntityMapper.toModelList(tagRepository.findAll(pageable).getContent());
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return tagRepository.findById(id).map(tagEntityMapper::toModel);
    }

    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name).map(tagEntityMapper::toModel);
    }

    @Override
    public Tag create(Tag tag) {
        TagEntity entity = tagEntityMapper.toEntity(tag);
        entity = tagRepository.save(entity);
        return tagEntityMapper.toModel(entity);
    }

    @Override
    public Tag update(Long id, Tag tag) {
        if (!tagRepository.existsById(id)) {
            throw new IllegalArgumentException("Tag not found with id: " + id);
        }
        TagEntity entity = tagEntityMapper.toEntity(tag);
        entity.setId(id);
        entity = tagRepository.save(entity);
        return tagEntityMapper.toModel(entity);
    }

    @Override
    public void deleteById(Long id) {
        tagRepository.deleteById(id);
    }
}