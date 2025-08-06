package com.chandu.search.service.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class LuceneConfig {

    private static final String LUCENE_INDEX_PATH = "lucene-index";

    @Bean
    public Analyzer analyzer() {
        return new StandardAnalyzer();
    }

    @Bean(destroyMethod = "close")
    public Directory directory() throws IOException {
        Path indexPath = Paths.get(LUCENE_INDEX_PATH);
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath);
        }
        return FSDirectory.open(indexPath);
    }

    @Bean(destroyMethod = "close")
    public IndexWriter indexWriter(Directory directory, Analyzer analyzer) throws IOException {
        Path lockFile = Paths.get(LUCENE_INDEX_PATH, IndexWriter.WRITE_LOCK_NAME);
        if (Files.exists(lockFile)) {
            try {
                Files.delete(lockFile);
            } catch (IOException e) {
                System.err.println("Unable to delete existing lock file: " + e.getMessage());
            }
        }

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(OpenMode.CREATE_OR_APPEND);
        config.setUseCompoundFile(true);
        config.setMaxBufferedDocs(10000);
        config.setRAMBufferSizeMB(256.0);
        return new IndexWriter(directory, config);
    }
}
