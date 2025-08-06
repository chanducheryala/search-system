package com.chandu.search.service.service.impl;

import com.chandu.search.service.dto.DishDto;
import com.chandu.search.service.dto.SearchResultDto;
import com.chandu.search.service.service.LuceneService;
import com.fasterxml.jackson.databind.util.ArrayBuilders.BooleanBuilder;

import io.micrometer.common.util.StringUtils;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (StringUtils.isBlank(queryString)) {
            return new SearchResultDto(0, Collections.emptyList());
        }

        String processedQuery = preprocessQuery(queryString);
        BooleanQuery.Builder finalQuery = buildQuery(processedQuery);
        TopDocs topDocs = executeSearch(finalQuery.build());

        return processResults(topDocs);
    }

    private BooleanQuery.Builder buildQuery(String query) throws ParseException{
        BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();

        String escapedQuery = QueryParser.escape(query.toLowerCase().trim());

        Query nameQuery = buildFieldQuery(NAME_FIELD, escapedQuery, 2.0f);
        Query categoryQuery = buildFieldQuery(CATEGORY_FIELD, escapedQuery, 1.0f);
        
        finalQuery.add(nameQuery, BooleanClause.Occur.SHOULD);
        finalQuery.add(categoryQuery, BooleanClause.Occur.SHOULD);

        Query phraseQuery = new PhraseQuery.Builder().add(new Term(NAME_FIELD, escapedQuery), 0).build();       
        Query boostedPrefixQuery = new BoostQuery(phraseQuery, 3.0f);
        finalQuery.add(boostedPrefixQuery, BooleanClause.Occur.SHOULD);

        return finalQuery;
    }

    private Query buildFieldQuery(String field, String query, float boost) throws ParseException{
        BooleanQuery.Builder fieldQuery = new BooleanQuery.Builder();

        Term fuzzyTerm = new Term(field, query);
        FuzzyQuery fuzzyQuery = new FuzzyQuery(fuzzyTerm);
        fieldQuery.add(fuzzyQuery, BooleanClause.Occur.SHOULD);

        String wildcard = "*" + query + "*";
        QueryParser parser = new QueryParser(field, analyzer);
        parser.setAllowLeadingWildcard(true);

        Query wildcardQuery = parser.parse(wildcard);
        fieldQuery.add(wildcardQuery, BooleanClause.Occur.SHOULD);

        PrefixQuery prefixQuery = new PrefixQuery(new Term(field, query));
        Query boostedPrefixQuery = new BoostQuery(prefixQuery, 1.5f);

        fieldQuery.add(boostedPrefixQuery, BooleanClause.Occur.SHOULD);

        BooleanQuery queryWithBoost = fieldQuery.build();

        return new BoostQuery(queryWithBoost, boost);
    }

    private String preprocessQuery(String query) {
        if(StringUtils.isEmpty(query)) {
            return "";
        }
        
        String normalized = query.trim().replaceAll("\\s+", " ").toLowerCase();
        String processed = normalized.replaceAll("[^\\p{L}\\p{N}\\s'-]", "");

        Map<String, String> replacements = Map.of(
            "&", "and",
            "vs", "versus",
            "w/", "with",
            "w/o", "without"
        );
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }
        Set<String> stopWords = Set.of("a", "an", "the", "and", "or", "in", "on", "at");
        processed = Arrays.stream(processed.split("\\s+"))
                     .filter(word -> !stopWords.contains(word))
                     .collect(Collectors.joining(" "));
        if (processed.endsWith("s") && processed.length() > 3) {
            processed = processed.substring(0, processed.length() - 1) + "?";
        }
        return query;
    }

    private TopDocs executeSearch(Query query) throws IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            return searcher.search(query, MAX_SEARCH_RESULTS);
        } finally {
            searcherManager.release(searcher);
        }
    }

    private SearchResultDto processResults(TopDocs docs) throws IOException {
        if (docs == null || docs.scoreDocs == null || docs.scoreDocs.length == 0) {
            return new SearchResultDto(0, Collections.emptyList());
        }
        List<DishDto> results = new ArrayList<>(docs.scoreDocs.length);
        IndexSearcher searcher = searcherManager.acquire();
        try {
            for (ScoreDoc sd : docs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                results.add(new DishDto(
                        doc.get(NAME_FIELD),
                        doc.get(CATEGORY_FIELD)
                ));
            }
            return new SearchResultDto(results.size(), results);
        } finally {
            searcherManager.release(searcher);
        }
   
    }

    private Document createDocument(DishDto dishDto) {
        Document doc = new Document();
        doc.add(new TextField(NAME_FIELD, dishDto.getName(), Field.Store.YES));
        doc.add(new TextField(CATEGORY_FIELD, dishDto.getCategory(), Field.Store.YES));
        return doc;
    }
}
