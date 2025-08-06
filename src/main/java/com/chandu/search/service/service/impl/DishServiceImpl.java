package com.chandu.search.service.service.impl;


import com.chandu.search.service.dto.DishDto;
import com.chandu.search.service.model.Dish;
import com.chandu.search.service.service.DishService;
import com.chandu.search.service.service.LuceneService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    private final LuceneService luceneService;

    public DishServiceImpl(LuceneService luceneService) {
        this.luceneService = luceneService;
    }

    @Override
    public DishDto add(DishDto dishDto) throws IOException {
        Dish dish = Dish.builder()
                .name(dishDto.getName())
                .id(1L)
                .build();
        luceneService.indexDish(dish);
        dishDto.setId(10L);
        return dishDto;
    }

    @Override
    public List<String> search(String queryString) throws ParseException, IOException {
        return luceneService.search(queryString);
    }
}
