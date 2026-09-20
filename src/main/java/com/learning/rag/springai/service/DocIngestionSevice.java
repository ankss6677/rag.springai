package com.learning.rag.springai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocIngestionSevice implements CommandLineRunner {

    @Value("classpath:/pdf/spring-boot-reference.pdf")
    private Resource resource;

    private final VectorStore vectorStore;

    public DocIngestionSevice(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("===== INGESTION START =====");
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        List<Document> rawDocuments = reader.read();

        // Check whether vectors already exist
        List<Document> existingDocuments =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("spring boot")
                                .topK(1)
                                .build()
                );

        if (existingDocuments != null && !existingDocuments.isEmpty()) {
            System.out.println("Vector store already contains data.");
            System.out.println("Skipping PDF ingestion.");
            return;
        }

        System.out.println("Vector store is empty. Starting ingestion...");

        TextSplitter splitter =  TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withKeepSeparator(true)
                .build();

        List<Document> documents = splitter.apply(rawDocuments);

        System.out.println("Chunks created: " + documents.size());

        long start = System.currentTimeMillis();

        vectorStore.accept(documents);

        long end = System.currentTimeMillis();

        System.out.println(
                "Embedding + vector insertion took: "
                        + ((end - start) / 1000)
                        + " seconds"
        );

        System.out.println("===== INGESTION COMPLETE =====");


    }
}
