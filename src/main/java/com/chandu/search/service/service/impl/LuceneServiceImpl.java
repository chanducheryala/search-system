package com.chandu.search.service.service.impl;

import com.chandu.search.service.model.Dish;
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
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class LuceneServiceImpl implements LuceneService {

    private final Directory directory;
    private final Analyzer analyzer;
    private final IndexWriter indexWriter;

    private SearcherManager searcherManager;

    private static final String ID_FIELD = "id";
    private static final String NAME_FIELD = "name";
    private static final int MAX_SEARCH_RESULTS = 50;

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
    public void indexDish(Dish dish) throws IOException {
        if (dish == null || dish.getId() == null || dish.getName() == null || dish.getName().isBlank()) return;

        Document doc = createDocument(dish);
        indexWriter.updateDocument(new Term(ID_FIELD, dish.getId().toString()), doc);
        indexWriter.commit();
        searcherManager.maybeRefresh();
    }

    @Override
    public List<String> search(String queryString) throws ParseException, IOException {
        if (queryString == null || queryString.isBlank()) return List.of();

        String escaped = QueryParser.escape(queryString.trim());
        String wildcardQuery = "*" + escaped + "*";

        QueryParser parser = new QueryParser(NAME_FIELD, analyzer);
        parser.setAllowLeadingWildcard(true);
        Query query = parser.parse(wildcardQuery);

        searcherManager.maybeRefresh();
        IndexSearcher searcher = null;

        try {
            searcher = searcherManager.acquire();
            TopDocs topDocs = searcher.search(query, MAX_SEARCH_RESULTS);

            List<String> results = new ArrayList<>(topDocs.scoreDocs.length);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                results.add(doc.get(NAME_FIELD));
            }
            return results;
        } finally {
            if (searcher != null) {
                searcherManager.release(searcher);
            }
        }
    }

    private Document createDocument(Dish dish) {
        Document doc = new Document();
        doc.add(new StringField(ID_FIELD, dish.getId().toString(), Field.Store.YES));
        doc.add(new TextField(NAME_FIELD, dish.getName(), Field.Store.YES));
        return doc;
    }
}
