package com.chandu.search.service.service;

import com.chandu.search.service.model.Dish;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public interface LuceneService {
    void indexDish(Dish dish) throws IOException;
    List<String> search(String queryString) throws ParseException, IOException;
}
