package com.chandu.search.service.service;

import com.chandu.search.service.dto.DishDto;
import com.chandu.search.service.dto.SearchResultDto;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;


public interface DishService {
    DishDto add(DishDto dishDto) throws IOException;
    SearchResultDto search(String query) throws ParseException, IOException;
}
