package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.FormatsApi;
import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
import com.deckbuilder.mtgdeckbuilder.application.FormatService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.FormatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class FormatController implements FormatsApi {
	private final FormatService formatService;
	private final FormatMapper formatMapper;
	private final CardMapper cardMapper;

	@Override
	public ResponseEntity<List<FormatDTO>> listFormats(Integer pagesize, Integer pagenumber) {
		final var formats = this.formatService.getAll();
		return ResponseEntity.ok(this.formatMapper.toDtoList(formats));
	}

	@Override
	public ResponseEntity<FormatDTO> getFormatById(Integer id) {
		return this.formatService.findById(id.longValue()).map(format -> ResponseEntity.ok(this.formatMapper.toDto(format)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<FormatDTO> createFormat(FormatDTO formatDTO) {
		final var format = this.formatMapper.toModel(formatDTO);
		final var created = this.formatService.create(format);
		return ResponseEntity.status(201).body(this.formatMapper.toDto(created));
	}

	@Override
	public ResponseEntity<FormatDTO> updateFormat(Integer id, FormatDTO formatDTO) {
		final var format = this.formatMapper.toModel(formatDTO);
		final var updated = this.formatService.update(id.longValue(), format);
		return ResponseEntity.ok(this.formatMapper.toDto(updated));
	}

	@Override
	public ResponseEntity<Void> deleteFormat(Integer id) {
		final boolean deleted = this.formatService.deleteById(id.longValue());
		return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}

	@Override
	public ResponseEntity<List<CardDTO>> listCardsForFormat(Integer id, Integer pagesize, Integer pagenumber) {
		final var cards = this.formatService.findCardsByFormatId(id.longValue(), pagesize != null ? pagesize : 10,
				pagenumber != null ? pagenumber : 0);
		final var cardDtos = cards.stream().map(this.cardMapper::toDto).collect(Collectors.toList());
		return ResponseEntity.ok(cardDtos);
	}
}