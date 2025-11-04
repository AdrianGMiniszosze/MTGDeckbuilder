package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.SetsApi;
import com.deckbuilder.apigenerator.openapi.api.model.SetDTO;
import com.deckbuilder.Mtgdeckbuilder.application.SetService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.SetMapper;
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
        var sets = setService.findAll(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
        return ResponseEntity.ok(setMapper.toSetDTOs(sets));
    }

    @Override
    public ResponseEntity<SetDTO> getSetById(Integer id) {
        return setService.findById(id.longValue())
                        .map(set -> ResponseEntity.ok(setMapper.toSetDTO(set)))
                        .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<SetDTO> createSet(@Valid SetDTO setDTO) {
        var set = setMapper.toSet(setDTO);
        var created = setService.create(set);
        return ResponseEntity.status(HttpStatus.CREATED)
                           .body(setMapper.toSetDTO(created));
    }

    @Override
    public ResponseEntity<SetDTO> updateSet(Integer id, @Valid SetDTO setDTO) {
        var set = setMapper.toSet(setDTO);
        var updated = setService.update(id.longValue(), set);
        return ResponseEntity.ok(setMapper.toSetDTO(updated));
    }

    @Override
    public ResponseEntity<Void> deleteSet(Integer id) {
        setService.deleteById(id.longValue());
        return ResponseEntity.noContent().build();
    }
}