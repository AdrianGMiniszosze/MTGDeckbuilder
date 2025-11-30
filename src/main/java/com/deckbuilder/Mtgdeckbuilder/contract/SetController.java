package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.SetsApi;
import com.deckbuilder.apigenerator.openapi.api.model.SetDTO;
import com.deckbuilder.mtgdeckbuilder.application.SetService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.SetMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.SetNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SetController implements SetsApi {
	private final SetService setService;
	private final SetMapper setMapper;

	@Override
	public ResponseEntity<List<SetDTO>> listSets(Integer pagesize, Integer pagenumber) {
		final var sets = this.setService.findAll(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
		return ResponseEntity.ok(this.setMapper.toSetDTOs(sets));
	}

	@Override
	public ResponseEntity<SetDTO> getSetById(Integer id) {
		final var set = this.setService.findById(id.longValue())
				.orElseThrow(() -> new SetNotFoundException(id.longValue()));
		return ResponseEntity.ok(this.setMapper.toSetDTO(set));
	}

	@Override
	public ResponseEntity<SetDTO> createSet(@Valid SetDTO setDTO) {
		final var set = this.setMapper.toSet(setDTO);
		final var created = this.setService.create(set);
		return ResponseEntity.status(HttpStatus.CREATED).body(this.setMapper.toSetDTO(created));
	}

	@Override
	public ResponseEntity<SetDTO> updateSet(Integer id, @Valid SetDTO setDTO) {
		final var set = this.setMapper.toSet(setDTO);
		final var updated = this.setService.update(id.longValue(), set);
		return ResponseEntity.ok(this.setMapper.toSetDTO(updated));
	}

	@Override
	public ResponseEntity<Void> deleteSet(Integer id) {
		this.setService.deleteById(id.longValue());
		return ResponseEntity.noContent().build();
	}
}
