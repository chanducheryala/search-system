package com.chandu.search.service.service;

import com.chandu.search.service.dto.DishDto;
import com.chandu.search.service.dto.SearchResultDto;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public interface LuceneService {
    void indexDish(DishDto dishDto) throws IOException;
    SearchResultDto search(String queryString) throws ParseException, IOException;
}
