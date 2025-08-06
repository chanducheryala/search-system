package com.chandu.search.service.service.impl;

import com.chandu.search.service.dto.DishDto;
import com.chandu.search.service.dto.SearchResultDto;
import com.chandu.search.service.service.LuceneService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class LuceneServiceImpl implements LuceneService {

    private final Directory directory;
    private final Analyzer analyzer;
    private final IndexWriter indexWriter;
    private SearcherManager searcherManager;

    private static final String NAME_FIELD = "name";
    private static final int MAX_SEARCH_RESULTS = 50;
    private static final String CATEGORY_FIELD = "category";

    @PostConstruct
    public void init() throws IOException {
        this.searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
        log.info("Lucene initialized");
    }

    @PreDestroy
    public void close() throws IOException {
        if (searcherManager != null) searcherManager.close();
        if (indexWriter != null) indexWriter.close();
        if (directory != null) directory.close();
    }

    @Override
    public void indexDish(DishDto dishDto) throws IOException {
        if (dishDto == null || dishDto.getName() == null || dishDto.getName().isBlank()) return;
        Document doc = createDocument(dishDto);
        indexWriter.addDocument(doc);
        indexWriter.commit();
        searcherManager.maybeRefresh();
    }

    @Override
    public SearchResultDto search(String queryString) throws ParseException, IOException {
        if (queryString == null || queryString.isBlank()) {
            return new SearchResultDto(0, Collections.emptyList());
        }

        String escapedQuery = QueryParser.escape(queryString.toLowerCase().trim());
        List<String> fieldsToSearch = List.of(NAME_FIELD, CATEGORY_FIELD);

        BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();

        for (String field : fieldsToSearch) {
            Term fuzzyTerm = new Term(field, queryString.toLowerCase());
            Query fuzzyQuery = new FuzzyQuery(fuzzyTerm, 2);

            String wildcard = "*" + escapedQuery + "*";
            QueryParser parser = new QueryParser(field, analyzer);
            parser.setAllowLeadingWildcard(true);
            Query wildcardQuery = parser.parse(wildcard);

            BooleanQuery.Builder fieldQueryBuilder = new BooleanQuery.Builder();
            fieldQueryBuilder.add(fuzzyQuery, BooleanClause.Occur.SHOULD);
            fieldQueryBuilder.add(wildcardQuery, BooleanClause.Occur.SHOULD);

            finalQueryBuilder.add(fieldQueryBuilder.build(), BooleanClause.Occur.SHOULD);
        }

        Query finalQuery = finalQueryBuilder.build();

        searcherManager.maybeRefresh();
        IndexSearcher searcher = null;

        try {
            searcher = searcherManager.acquire();
            TopDocs topDocs = searcher.search(finalQuery, MAX_SEARCH_RESULTS);
            log.info("Total documents found: {}", topDocs.totalHits.value);

            List<DishDto> results = new ArrayList<>(topDocs.scoreDocs.length);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                results.add(new DishDto(
                        doc.get(NAME_FIELD),
                        doc.get(CATEGORY_FIELD)
                ));
            }

            return new SearchResultDto(results.size(), results);
        } finally {
            if (searcher != null) {
                searcherManager.release(searcher);
            }
        }
    }


    private Document createDocument(DishDto dishDto) {
        Document doc = new Document();
        doc.add(new TextField(NAME_FIELD, dishDto.getName(), Field.Store.YES));
        doc.add(new TextField(CATEGORY_FIELD, dishDto.getCategory(), Field.Store.YES));
        return doc;
    }
}
