# Spring AI RAG Application

A Retrieval-Augmented Generation (RAG) application built with **Spring Boot** and **Spring AI**.

The application reads a PDF document, splits it into smaller chunks, generates vector embeddings using **Ollama**, stores those embeddings in **PostgreSQL with PGVector**, and uses similarity search to retrieve relevant information before generating an answer with a local **Mistral** LLM.

The goal of this project is to demonstrate how to build a locally running RAG application without depending on a cloud-based LLM API.

---

## Architecture

```text
                    ┌──────────────────────┐
                    │      User Query      │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │    Spring Boot App   │
                    │      Spring AI      │
                    └──────────┬───────────┘
                               │
                 ┌─────────────┴─────────────┐
                 │                           │
                 ▼                           ▼
       ┌──────────────────┐        ┌──────────────────┐
       │    PGVector      │        │      Ollama      │
       │   Vector Store   │        │                  │
       └────────┬─────────┘        │ Mistral          │
                │                  │ mxbai-embed-large│
                │                  └────────┬─────────┘
                │                           │
                └──────────┬────────────────┘
                           ▼
                 ┌──────────────────────┐
                 │ Relevant Documents   │
                 │ + User Question      │
                 └──────────┬───────────┘
                            │
                            ▼
                 ┌──────────────────────┐
                 │   Mistral Response   │
                 └──────────────────────┘
```

---

# Technology Stack

| Technology        | Purpose                                   |
| ----------------- | ----------------------------------------- |
| Java              | Application development                   |
| Spring Boot       | Application framework                     |
| Spring AI         | AI/RAG integration                        |
| Ollama            | Local LLM and embedding runtime           |
| Mistral           | Chat / generation model                   |
| mxbai-embed-large | Text embedding model                      |
| PostgreSQL        | Database                                  |
| PGVector          | Vector storage and similarity search      |
| Apache Tika       | PDF/document text extraction              |
| TokenTextSplitter | Document chunking                         |
| Docker            | PostgreSQL/PGVector and Ollama containers |
| Maven             | Dependency management                     |

---

# How RAG Works in This Application

The application follows the typical RAG pipeline:

```text
PDF
 │
 ▼
Read document
 │
 ▼
Split into chunks
 │
 ▼
Generate embeddings
 │
 ▼
Store embeddings in PGVector
 │
 ▼
User asks a question
 │
 ▼
Generate embedding for question
 │
 ▼
Similarity search in PGVector
 │
 ▼
Retrieve relevant chunks
 │
 ▼
Send question + retrieved context to Mistral
 │
 ▼
Generate answer
```

The important distinction is that the LLM does not need to contain the PDF information in its original training data. Relevant information is retrieved from the vector database at query time.

---

# Project Structure

The project follows a typical Spring Boot structure.

```text
src
└── main
    ├── java
    │   └── ...
    │       └── rag
    │           └── springai
    │               ├── ...
    │               └── DocIngestionSevice.java
    │
    └── resources
        ├── application.properties
        └── pdf
            └── spring-boot-reference.pdf
```

> The exact package/class structure may vary depending on the latest version of the project.

---

# Prerequisites

Install the following before running the application:

* Java
* Maven
* Docker Desktop
* PostgreSQL/PGVector container
* Ollama

The application is designed to run the LLM locally through Ollama.

---

# Ollama Models

This project currently uses two Ollama models.

## Chat Model

```text
mistral:latest
```

This model is responsible for generating the final answer.

## Embedding Model

```text
mxbai-embed-large:latest
```

This model converts document text and user questions into vector embeddings.

Check installed models:

```bash
ollama list
```

For a native Ollama installation, models can be downloaded using:

```bash
ollama pull mistral
ollama pull mxbai-embed-large
```

If Ollama is running inside Docker, execute the equivalent command against the container.

---

# PostgreSQL + PGVector

The application uses PostgreSQL with the PGVector extension as its vector database.

A typical Docker container can be started with:

```bash
docker run -d \
  --name pgvector \
  -e POSTGRES_DB=vectordb \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=123 \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

Verify that the container is running:

```bash
docker ps
```

The application connects to:

```text
localhost:5432
```

with:

```text
Database: vectordb
Username: test
Password: 123
```

---

# Ollama Docker Container

Ollama can also be run using Docker.

For example:

```bash
docker run -d \
  --name ollama \
  -p 11434:11434 \
  ollama/ollama
```

Verify:

```bash
docker ps
```

Test the Ollama API:

```bash
curl http://localhost:11434/api/tags
```

The Spring Boot application connects to:

```text
http://localhost:11434
```

---

# Application Configuration

The application uses the following configuration:

```properties
spring.application.name=rag.springai

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.init.pull-model-strategy=when_missing
spring.ai.ollama.init.timeout=5m

# Chat model
spring.ai.ollama.chat.options.model=mistral

# Embedding model
spring.ai.ollama.embedding.options.model=mxbai-embed-large

# PGVector
spring.ai.vectorstore.pgvector.initialize-schema=true

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/vectordb
spring.datasource.username=test
spring.datasource.password=123

# Logging
logging.level.org.springframework.ai=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

For a real production deployment, database credentials should not be committed directly to the repository. Environment variables or a secrets-management solution should be used instead.

---

# PDF Ingestion

The application currently loads the PDF from:

```text
src/main/resources/pdf/spring-boot-reference.pdf
```

The ingestion service uses Apache Tika to extract text from the PDF.

Example:

```java
TikaDocumentReader reader =
        new TikaDocumentReader(resource);
```

The extracted document is then split into smaller chunks using Spring AI's `TokenTextSplitter`.

Current configuration:

```java
TokenTextSplitter splitter = TokenTextSplitter.builder()
        .withChunkSize(800)
        .withMinChunkSizeChars(350)
        .withMinChunkLengthToEmbed(5)
        .withKeepSeparator(true)
        .build();
```

The resulting chunks are then passed to the vector store:

```java
List<Document> documents = splitter.apply(reader.read());

vectorStore.accept(documents);
```

---

# Ingestion Pipeline

The ingestion process can be summarized as:

```text
spring-boot-reference.pdf
          │
          ▼
    Apache Tika
          │
          ▼
    Extracted text
          │
          ▼
   TokenTextSplitter
          │
          ▼
    Document chunks
          │
          ▼
 mxbai-embed-large
          │
          ▼
     Embeddings
          │
          ▼
      PGVector
```

During testing, the PDF was successfully split into approximately **400 chunks**.

The main processing cost during ingestion comes from generating embeddings for those chunks.

---

# Vector Store

The PGVector database stores the vector representations of the document chunks.

Conceptually:

```text
Document Chunk
      │
      ▼
Embedding Model
      │
      ▼
Vector
      │
      ▼
PostgreSQL + PGVector
```

When a user asks a question, the question is also converted into an embedding.

The application then performs similarity search against the stored vectors.

---

# Query / RAG Pipeline

The query process works approximately as follows:

```text
User Question
      │
      ▼
Question Embedding
      │
      ▼
PGVector Similarity Search
      │
      ▼
Relevant Document Chunks
      │
      ▼
RAG Context
      │
      ▼
Mistral
      │
      ▼
Generated Answer
```

This allows the application to answer questions using information retrieved from the indexed PDF.

---

# Running the Application

## 1. Start PostgreSQL/PGVector

Verify that PostgreSQL is running:

```bash
docker ps
```

You should see the PGVector container.

---

## 2. Start Ollama

If Ollama is running in Docker:

```bash
docker ps
```

Verify the API:

```bash
curl http://localhost:11434/api/tags
```

---

## 3. Verify Models

Check the models available in Ollama.

For native Ollama:

```bash
ollama list
```

For Docker:

```bash
docker exec -it ollama ollama list
```

Expected models:

```text
mistral
mxbai-embed-large
```

---

## 4. Start Spring Boot

From the project directory:

```bash
./mvnw spring-boot:run
```

Or run the application directly from IntelliJ IDEA.

---

# Testing Ollama Independently

Before troubleshooting the Spring AI application, it is useful to test Ollama directly.

```bash
curl http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "mistral",
    "prompt": "Explain in two sentences what Spring Boot is.",
    "stream": false,
    "options": {
      "num_predict": 50
    }
  }'
```

This separates Ollama performance issues from Spring Boot or PGVector issues.

---

# Debugging

The application enables Spring AI debugging:

```properties
logging.level.org.springframework.ai=DEBUG
```

Hibernate SQL logging is also enabled:

```properties
logging.level.org.hibernate.SQL=DEBUG
```

This can help identify whether time is being spent in:

* document ingestion
* embedding generation
* PGVector queries
* LLM generation

---

# Performance Considerations

The application uses local AI models, which provides privacy and removes dependency on external LLM APIs.

However, local inference performance depends heavily on the hardware and how Ollama is deployed.

The current development environment uses:

```text
Apple Silicon Mac
M3 Pro
18 GB RAM
```

Ollama is currently running inside Docker.

A direct test of the Mistral model showed that model loading and generation can take several seconds even for a small prompt.

For example, the Ollama response showed approximately:

```text
Model load:       ~7.6 seconds
Prompt evaluation: ~0.4 seconds
Token generation: ~4.6 seconds
```

Therefore, Docker-based Ollama performance should be considered separately from Spring AI performance.

For Apple Silicon Macs, native Ollama can provide a different acceleration path than a Linux container running through Docker Desktop.

---

# Important: Avoid Re-Embedding the Same PDF

The ingestion service implements `CommandLineRunner`, which means the ingestion code executes whenever the Spring Boot application starts.

If the application is restarted and the ingestion code always executes:

```java
vectorStore.accept(documents);
```

the same document can potentially be embedded and stored again.

For a production-ready implementation, ingestion should be made idempotent.

Possible approaches include:

1. Store a stable document/source identifier in metadata.
2. Check whether the document has already been indexed.
3. Skip ingestion when the source has not changed.
4. Use document hashes to detect changes.
5. Separate document ingestion from application startup.

A simple RAG application can start with an empty-vector-store check, but a source/hash-based strategy is preferable for a more robust implementation.

---

# Persistent PGVector Storage

When PostgreSQL runs in Docker, database persistence should be configured using a Docker volume.

Example:

```yaml
volumes:
  pgvector_data:
```

and:

```yaml
services:
  pgvector:
    image: pgvector/pgvector:pg16
    volumes:
      - pgvector_data:/var/lib/postgresql/data
```

This prevents database data from disappearing when the container itself is recreated.

### Important

Avoid:

```bash
docker compose down -v
```

when you want to preserve the database volume.

Removing the volume can delete the stored vector data.

---

# Current RAG Components

The project currently consists of the following major components:

### 1. Document Reader

Uses:

```text
Apache Tika
```

to extract content from the PDF.

### 2. Text Splitter

Uses:

```text
Spring AI TokenTextSplitter
```

to divide the document into manageable chunks.

### 3. Embedding Model

Uses:

```text
mxbai-embed-large
```

to generate vector representations.

### 4. Vector Database

Uses:

```text
PostgreSQL + PGVector
```

to store and search embeddings.

### 5. Chat Model

Uses:

```text
Mistral
```

through Ollama.

### 6. RAG Advisor

Spring AI's vector-store advisor can retrieve relevant chunks from PGVector and provide them as context to the chat model.

The current Spring AI API uses the builder pattern:

```java
QuestionAnswerAdvisor.builder(vectorStore)
        .build();
```

---

# Why Use Local RAG?

This project demonstrates a completely local RAG architecture.

```text
                 LOCAL MACHINE
┌──────────────────────────────────────────────┐
│                                              │
│  Spring Boot                                 │
│      │                                       │
│      ├──── PGVector/PostgreSQL               │
│      │                                       │
│      └──── Ollama                            │
│              │                               │
│              ├──── Mistral                   │
│              │                               │
│              └──── mxbai-embed-large         │
│                                              │
└──────────────────────────────────────────────┘
```

Benefits include:

* No external LLM API required
* Data can remain on the local machine
* No per-token cloud API cost
* Easy experimentation with different local models
* Useful for learning Spring AI and RAG architecture

---

# Future Improvements

Possible improvements for this project include:

### Document Management

* Support multiple PDFs
* Support DOCX and TXT files
* Track document versions
* Detect changed documents
* Avoid duplicate embeddings

### RAG Improvements

* Configure retrieval `topK`
* Add metadata filtering
* Improve chunking strategy
* Add citations to retrieved documents
* Experiment with different embedding models
* Evaluate retrieval quality

### Performance

* Optimize Ollama deployment
* Keep frequently used models loaded
* Reduce unnecessary context
* Tune chunk sizes
* Tune retrieval count
* Measure embedding and generation latency separately

### Application Features

* REST API for questions
* Web UI / chat interface
* Conversation history
* Streaming responses
* Source-document display
* Authentication

---

# Example RAG Interaction

Example question:

```text
What is Spring Boot?
```

The application does not simply send the question directly to Mistral.

Instead:

```text
Question
   │
   ▼
Embedding
   │
   ▼
PGVector
   │
   ▼
Find relevant Spring Boot documentation
   │
   ▼
Relevant chunks
   │
   ▼
Mistral + retrieved context
   │
   ▼
Answer
```

This is the fundamental concept behind Retrieval-Augmented Generation.

---

# Troubleshooting

## Ollama is not reachable

Check:

```bash
curl http://localhost:11434/api/tags
```

If this fails, check:

```bash
docker ps
```

and inspect the Ollama container logs:

```bash
docker logs ollama
```

---

## Check currently loaded Ollama models

```bash
curl http://localhost:11434/api/ps
```

This can also help determine whether a model is currently loaded.

---

## Check PGVector

```bash
docker ps
```

Verify that PostgreSQL is exposed on:

```text
localhost:5432
```

Then verify the application configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/vectordb
```

---

## Ingestion is very slow

The ingestion process includes embedding every document chunk.

For example:

```text
PDF
 ↓
400 chunks
 ↓
400 embedding operations
 ↓
PGVector
```

Therefore, embedding performance can dominate the ingestion time.

Test Ollama separately before investigating Spring Boot.

---

# Development Notes

This project is primarily intended as a learning and experimentation project for:

* Spring Boot
* Spring AI
* RAG
* Vector databases
* Embeddings
* Local LLMs
* Ollama
* PGVector

The architecture can later be extended into a production-ready document question-answering system.


# Author

Developed as a learning project exploring:

**Spring Boot + Spring AI + Ollama + Mistral + PGVector + RAG**

---
