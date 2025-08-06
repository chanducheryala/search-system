package com.chandu.search.service.controller;

import com.chandu.search.service.dto.DishDto;
import com.chandu.search.service.dto.SearchResultDto;
import com.chandu.search.service.service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DishController {

    private static final Logger log = LoggerFactory.getLogger(DishController.class);
    @Autowired
    private DishService dishService;

    @PostMapping("/v1/dishes")
    public ResponseEntity<DishDto> index(@RequestBody DishDto dishDto) {
        try {
            DishDto response = dishService.add(dishDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/v1/dishes")
    public ResponseEntity<SearchResultDto> search(@RequestParam String query) {
        try {
            log.info("Query : {}", query);
            SearchResultDto result = dishService.search(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new SearchResultDto());
        }
    }
}