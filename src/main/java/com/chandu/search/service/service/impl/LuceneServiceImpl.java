package com.chandu.search.service.service.impl;

import com.chandu.search.service.model.Dish;
import com.chandu.search.service.service.LuceneService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
public class LuceneServiceImpl implements LuceneService {

    @Autowired
    private IndexWriter indexWriter;

    @Autowired
    private Directory directory;

    @Autowired
    private Analyzer analyzer;

    @Override
    public void indexDish(Dish dish) throws IOException {
        Document document = new Document();
        document.add(new LongPoint("id", dish.getId()));
        document.add(new TextField("name", dish.getName(), Field.Store.YES));
        indexWriter.addDocument(document);
        indexWriter.commit();
    }

    public List<String> search(String queryString) throws ParseException, IOException {
        List<String> results = new ArrayList<>();
        
        if (queryString == null || queryString.trim().isEmpty()) {
            return results;
        }
        
        try {
            QueryParser parser = new QueryParser("name", analyzer);
            String escapedQuery = QueryParser.escape(queryString.trim());
            Query query = parser.parse(escapedQuery + "*");

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs topDocs = searcher.search(query, 40);

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = searcher.doc(scoreDoc.doc);
                    results.add(document.get("name"));
                }
            }
        } catch (Exception e) {
            throw new IOException("Search failed: " + e.getMessage(), e);
        }
        return results;
    }
}
