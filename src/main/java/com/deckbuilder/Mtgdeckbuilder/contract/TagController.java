package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.TagsApi;
import com.deckbuilder.apigenerator.openapi.api.model.TagDTO;
import com.deckbuilder.mtgdeckbuilder.application.TagService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.TagMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.TagNotFoundException;
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
		final var tags = this.tagService.findAll(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
		return ResponseEntity.ok(this.tagMapper.toTagDTOs(tags));
	}

	@Override
	public ResponseEntity<TagDTO> getTagById(Integer id) {
		final var tag = this.tagService.findById(id.longValue())
				.orElseThrow(() -> new TagNotFoundException(id.longValue()));
		return ResponseEntity.ok(this.tagMapper.toTagDTO(tag));
	}

	@Override
	public ResponseEntity<TagDTO> createTag(@Valid TagDTO tagDTO) {
		final var tag = this.tagMapper.toTag(tagDTO);
		final var created = this.tagService.create(tag);
		return ResponseEntity.status(HttpStatus.CREATED).body(this.tagMapper.toTagDTO(created));
	}

	@Override
	public ResponseEntity<TagDTO> updateTag(Integer id, @Valid TagDTO tagDTO) {
		final var tag = this.tagMapper.toTag(tagDTO);
		final var updated = this.tagService.update(id.longValue(), tag);
		return ResponseEntity.ok(this.tagMapper.toTagDTO(updated));
	}

	@Override
	public ResponseEntity<Void> deleteTag(Integer id) {
		this.tagService.deleteById(id.longValue());
		return ResponseEntity.noContent().build();
	}
}
