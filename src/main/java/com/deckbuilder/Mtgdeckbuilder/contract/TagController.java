package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.TagsApi;
import com.deckbuilder.apigenerator.openapi.api.model.TagDTO;
import com.deckbuilder.Mtgdeckbuilder.application.TagService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.TagMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController implements TagsApi {
    private final TagService tagService;
    private final TagMapper tagMapper;

    @Override
    public ResponseEntity<List<TagDTO>> listTags(Integer pagesize, Integer pagenumber) {
        var tags = tagService.findAll(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
        return ResponseEntity.ok(tagMapper.toTagDTOs(tags));
    }

    @Override
    public ResponseEntity<TagDTO> getTagById(Integer id) {
        return tagService.findById(id.longValue())
                        .map(tag -> ResponseEntity.ok(tagMapper.toTagDTO(tag)))
                        .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<TagDTO> createTag(@Valid TagDTO tagDTO) {
        var tag = tagMapper.toTag(tagDTO);
        var created = tagService.create(tag);
        return ResponseEntity.status(HttpStatus.CREATED)
                           .body(tagMapper.toTagDTO(created));
    }

    @Override
    public ResponseEntity<TagDTO> updateTag(Integer id, @Valid TagDTO tagDTO) {
        var tag = tagMapper.toTag(tagDTO);
        var updated = tagService.update(id.longValue(), tag);
        return ResponseEntity.ok(tagMapper.toTagDTO(updated));
    }

    @Override
    public ResponseEntity<Void> deleteTag(Integer id) {
        tagService.deleteById(id.longValue());
        return ResponseEntity.noContent().build();
    }
}