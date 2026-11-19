package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.SetsApi;
import com.deckbuilder.apigenerator.openapi.api.model.SetDTO;
import com.deckbuilder.mtgdeckbuilder.application.SetService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.SetMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.SetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SetController implements SetsApi {
	private final SetService setService;
	private final SetMapper setMapper;

	@Override
	public ResponseEntity<List<SetDTO>> listSets(
			@RequestParam(name = "pageSize", required = false) Integer pagesize,
			@RequestParam(name = "pageNumber", required = false) Integer pagenumber) {
		final var sets = this.setService.findAll(pagesize != null ? pagesize : 20, pagenumber != null ? pagenumber : 0);
		return ResponseEntity.ok(this.setMapper.toSetDTOs(sets));
	}

	@Override
	public ResponseEntity<SetDTO> getSetById(Integer id) {
		final var set = this.setService.findById(id.longValue())
				.orElseThrow(() -> new SetNotFoundException(id.longValue()));
		return ResponseEntity.ok(this.setMapper.toSetDTO(set));
	}
}
