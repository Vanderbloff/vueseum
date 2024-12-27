package com.mvp.artplatform.controller;

import com.mvp.artplatform.dto.MuseumDTO;
import com.mvp.artplatform.entity.Museum;
import com.mvp.artplatform.service.museum.MuseumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/museums")
@RequiredArgsConstructor
public class MuseumController {
    private final MuseumService museumService;

    @GetMapping
    public ResponseEntity<List<Museum>> getMuseums() {
        return ResponseEntity.ok(museumService.findAllMuseums());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MuseumDTO> getMuseum(@PathVariable Long id) {
        return museumService.findMuseumById(id)
                .map(MuseumDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
