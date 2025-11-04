package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.FormatsApi;
import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
import com.deckbuilder.Mtgdeckbuilder.application.FormatService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.FormatMapper;
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
        var formats = formatService.getAll();
        return ResponseEntity.ok(formatMapper.toDtoList(formats));
    }

    @Override
    public ResponseEntity<FormatDTO> getFormatById(Integer id) {
        return formatService.findById(id.longValue())
                .map(format -> ResponseEntity.ok(formatMapper.toDto(format)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<FormatDTO> createFormat(FormatDTO formatDTO) {
        var format = formatMapper.toModel(formatDTO);
        var created = formatService.create(format);
        return ResponseEntity.status(201).body(formatMapper.toDto(created));
    }

    @Override
    public ResponseEntity<FormatDTO> updateFormat(Integer id, FormatDTO formatDTO) {
        var format = formatMapper.toModel(formatDTO);
        var updated = formatService.update(id.longValue(), format);
        return ResponseEntity.ok(formatMapper.toDto(updated));
    }

    @Override
    public ResponseEntity<Void> deleteFormat(Integer id) {
        boolean deleted = formatService.deleteById(id.longValue());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<List<CardDTO>> listCardsForFormat(Integer id, Integer pagesize, Integer pagenumber) {
        var cards = formatService.findCardsByFormatId(
            id.longValue(),
            pagesize != null ? pagesize : 10,
            pagenumber != null ? pagenumber : 0
        );
        var cardDtos = cards.stream()
                .map(cardMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cardDtos);
    }
}