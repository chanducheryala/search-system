package com.chandu.search.service.service.impl;

import com.chandu.search.service.dto.DishDto;
import com.chandu.search.service.model.Dish;
import com.chandu.search.service.service.DishService;
import com.chandu.search.service.service.LuceneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final LuceneService luceneService;

    @Override
    public DishDto add(DishDto dishDto) throws IOException {
        log.info("Creating new dish: {}", dishDto.getName());
        Dish dish = Dish.builder()
                    .name(dishDto.getName())
                    .id(1L)
                    .build();
        luceneService.indexDish(dish);
        dishDto.setId(1L);
        return dishDto;
    }


    @Override
    public List<String> search(String query) throws ParseException, IOException {
        log.debug("Searching dishes with query: {}", query);
        return luceneService.search(query);
    }
}
