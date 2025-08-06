package com.chandu.search.service.config;



import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class LuceneConfig{

    private static final String LUCENE_PATH = "lucene-index";

    @Bean
    public Directory directory() throws IOException {
        return FSDirectory.open(Paths.get(LUCENE_PATH));
    }


    @Bean
    public Analyzer analyzer() {
        return new StandardAnalyzer();
    }


    /*
    *       CREATE_OR_APPEND
    *           -> if index exists, append new documents
    *           -> else create new index
    * */
    @Bean
    public IndexWriter indexWriter(Directory directory, Analyzer analyzer) throws IOException{
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(directory, config);
    }
}
