package com.app.features.ai.search.integration.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.embedding.service.AiEmbeddingClient;
import com.app.features.ai.embedding.service.AiEmbeddingHealthClient;
import com.app.features.ai.search.exceptions.AiSearchRuntimeException;
import com.app.features.ai.search.schema.model.PostVectorDocument;
import com.app.features.ai.search.schema.model.PostVectorSearchHit;
import com.app.features.ai.search.service.AiSearchHealthClient;
import com.app.features.ai.search.service.PostVectorIndex;
import com.app.features.post.enums.PostType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai.search",
        name = "enabled",
        havingValue = "true")
public class LucenePostVectorIndex
        implements PostVectorIndex, AiSearchHealthClient {

    private static final String INDEX_SCHEMA_VERSION = "2";
    private static final String COMMIT_SCHEMA_VERSION = "schema_version";
    private static final String COMMIT_MODEL_VERSION = "model_version";
    private static final String COMMIT_VECTOR_DIMENSION = "vector_dimension";
    private static final String COMMIT_INDEX_GENERATION = "index_generation";
    private static final String FIELD_POST_ID = "post_id";
    private static final String FIELD_POST_TYPE = "post_type";
    private static final String FIELD_MODEL_VERSION = "model_version";
    private static final String FIELD_SOURCE_UPDATED_AT = "source_updated_at";
    private static final String FIELD_CONTENT_VECTOR = "content_vector";

    private final AppProperties appProperties;
    private final ObjectProvider<AiEmbeddingClient>
            aiEmbeddingClientProvider;
    private final ObjectProvider<AiEmbeddingHealthClient>
            aiEmbeddingHealthClientProvider;
    private final ReentrantReadWriteLock lifecycleLock =
            new ReentrantReadWriteLock();
    private final ReentrantLock mutationLock = new ReentrantLock();

    private volatile RuntimeResources resources;
    private volatile String statusDetail = "Lucene search index is not initialized.";

    @PostConstruct
    public void start() {
        AiEmbeddingClient embeddingClient =
                aiEmbeddingClientProvider.getIfAvailable();
        AiEmbeddingHealthClient embeddingHealthClient =
                aiEmbeddingHealthClientProvider.getIfAvailable();
        if (embeddingClient == null
                || embeddingHealthClient == null
                || !embeddingHealthClient.isReady()) {
            statusDetail = "Lucene search index requires a ready embedding runtime.";
            log.warn(statusDetail);
            return;
        }

        Directory initializedDirectory = null;
        IndexWriter initializedWriter = null;
        SearcherManager initializedSearcherManager = null;
        try {
            String modelVersion = embeddingClient.getModelVersion();
            int vectorDimension = embeddingClient.getDimension();
            Path indexDirectory = resolveIndexDirectory();
            Files.createDirectories(indexDirectory);

            initializedDirectory = FSDirectory.open(indexDirectory);
            IndexOpenPlan openPlan = resolveOpenPlan(
                    initializedDirectory,
                    modelVersion,
                    vectorDimension);
            if (openPlan.recreate()) {
                log.warn(
                        "Recreating Lucene post index at [{}] because its model metadata no longer matches [{}] with dimension [{}].",
                        indexDirectory,
                        modelVersion,
                        vectorDimension);
            }

            IndexWriterConfig writerConfig = new IndexWriterConfig()
                    .setOpenMode(openPlan.openMode());
            initializedWriter = new IndexWriter(
                    initializedDirectory,
                    writerConfig);
            initializedWriter.setLiveCommitData(commitMetadata(
                    modelVersion,
                    vectorDimension,
                    openPlan.indexGeneration()).entrySet());
            initializedWriter.commit();
            initializedSearcherManager = new SearcherManager(
                    initializedWriter,
                    null);

            RuntimeResources initializedResources = new RuntimeResources(
                    initializedDirectory,
                    initializedWriter,
                    initializedSearcherManager,
                    indexDirectory,
                    modelVersion,
                    vectorDimension,
                    openPlan.indexGeneration());
            lifecycleLock.writeLock().lock();
            try {
                resources = initializedResources;
                statusDetail = "READY";
            } finally {
                lifecycleLock.writeLock().unlock();
            }

            initializedDirectory = null;
            initializedWriter = null;
            initializedSearcherManager = null;
            log.info(
                    "Lucene post vector index is ready at [{}] for model [{}] with dimension [{}].",
                    indexDirectory,
                    modelVersion,
                    vectorDimension);
        } catch (Exception | LinkageError exception) {
            statusDetail = "Unable to initialize Lucene post vector index: "
                    + exception.getMessage();
            log.error(statusDetail, exception);
        } finally {
            closeQuietly(
                    initializedSearcherManager,
                    initializedWriter,
                    initializedDirectory);
        }
    }

    @Override
    public void upsert(PostVectorDocument vectorDocument) {
        mutate(currentResources -> {
            float[] vector = vectorDocument.vector();
            requireVectorDimension(vector, currentResources.vectorDimension());

            Document document = new Document();
            document.add(new StringField(
                    FIELD_POST_ID,
                    vectorDocument.postId().toString(),
                    Field.Store.YES));
            document.add(new StringField(
                    FIELD_POST_TYPE,
                    vectorDocument.postType().name(),
                    Field.Store.YES));
            document.add(new StringField(
                    FIELD_MODEL_VERSION,
                    currentResources.modelVersion(),
                    Field.Store.YES));
            document.add(new StoredField(
                    FIELD_SOURCE_UPDATED_AT,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                            vectorDocument.sourceUpdatedAt())));
            document.add(new KnnFloatVectorField(
                    FIELD_CONTENT_VECTOR,
                    vector,
                    VectorSimilarityFunction.DOT_PRODUCT));

            currentResources.indexWriter().updateDocument(
                    new Term(
                            FIELD_POST_ID,
                            vectorDocument.postId().toString()),
                    document);
        });
    }

    @Override
    public void delete(UUID postId) {
        mutate(currentResources -> currentResources.indexWriter()
                .deleteDocuments(new Term(FIELD_POST_ID, postId.toString())));
    }

    @Override
    public List<PostVectorSearchHit> search(
            float[] queryVector,
            int limit) {
        return read(currentResources -> {
            requireVectorDimension(
                    queryVector,
                    currentResources.vectorDimension());
            int effectiveLimit = Math.min(
                    limit,
                    appProperties.getAi().getSearch().getMaxLimit());
            Query modelFilter = new TermQuery(new Term(
                    FIELD_MODEL_VERSION,
                    currentResources.modelVersion()));
            Query vectorQuery = new KnnFloatVectorQuery(
                    FIELD_CONTENT_VECTOR,
                    queryVector.clone(),
                    effectiveLimit,
                    modelFilter);

            IndexSearcher searcher = currentResources.searcherManager()
                    .acquire();
            try {
                TopDocs topDocs = searcher.search(
                        vectorQuery,
                        effectiveLimit);
                StoredFields storedFields = searcher.storedFields();
                List<PostVectorSearchHit> hits = new ArrayList<>(
                        topDocs.scoreDocs.length);
                for (ScoreDoc scoreDocument : topDocs.scoreDocs) {
                    Document storedDocument = storedFields.document(
                            scoreDocument.doc);
                    hits.add(new PostVectorSearchHit(
                            UUID.fromString(storedDocument.get(FIELD_POST_ID)),
                            PostType.valueOf(
                                    storedDocument.get(FIELD_POST_TYPE)),
                            scoreDocument.score));
                }
                return List.copyOf(hits);
            } finally {
                currentResources.searcherManager().release(searcher);
            }
        });
    }

    @Override
    public boolean isReady() {
        return resources != null;
    }

    @Override
    public String getModelVersion() {
        RuntimeResources currentResources = resources;
        return currentResources == null
                ? "UNAVAILABLE"
                : currentResources.modelVersion();
    }

    @Override
    public UUID getIndexGeneration() {
        RuntimeResources currentResources = requireResources();
        return currentResources.indexGeneration();
    }

    @Override
    public String getIndexDirectory() {
        RuntimeResources currentResources = resources;
        return currentResources == null
                ? resolveIndexDirectory().toString()
                : currentResources.indexDirectory().toString();
    }

    @Override
    public int getDocumentCount() {
        if (!isReady()) {
            return 0;
        }
        return read(currentResources -> {
            IndexSearcher searcher = currentResources.searcherManager()
                    .acquire();
            try {
                return searcher.getIndexReader().numDocs();
            } finally {
                currentResources.searcherManager().release(searcher);
            }
        });
    }

    @Override
    public String getStatusDetail() {
        return statusDetail;
    }

    @PreDestroy
    public void close() {
        RuntimeResources currentResources;
        lifecycleLock.writeLock().lock();
        try {
            currentResources = resources;
            resources = null;
        } finally {
            lifecycleLock.writeLock().unlock();
        }

        if (currentResources == null) {
            return;
        }
        closeQuietly(
                currentResources.searcherManager(),
                currentResources.indexWriter(),
                currentResources.directory());
        statusDetail = "Lucene search index is closed.";
        log.info(
                "Lucene post vector index at [{}] was closed.",
                currentResources.indexDirectory());
    }

    private void mutate(IndexMutation mutation) {
        lifecycleLock.readLock().lock();
        mutationLock.lock();
        try {
            RuntimeResources currentResources = requireResources();
            mutation.apply(currentResources);
            currentResources.indexWriter().commit();
            currentResources.searcherManager().maybeRefreshBlocking();
        } catch (IOException exception) {
            throw new AiSearchRuntimeException(
                    "Unable to update Lucene post vector index.",
                    exception);
        } finally {
            mutationLock.unlock();
            lifecycleLock.readLock().unlock();
        }
    }

    private <T> T read(IndexOperation<T> operation) {
        lifecycleLock.readLock().lock();
        try {
            return operation.execute(requireResources());
        } catch (IOException exception) {
            throw new AiSearchRuntimeException(
                    "Unable to read Lucene post vector index.",
                    exception);
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    private RuntimeResources requireResources() {
        RuntimeResources currentResources = resources;
        if (currentResources == null) {
            throw new AiSearchRuntimeException(statusDetail);
        }
        return currentResources;
    }

    private IndexOpenPlan resolveOpenPlan(
            Directory directory,
            String modelVersion,
            int vectorDimension) throws IOException {
        if (!DirectoryReader.indexExists(directory)) {
            return new IndexOpenPlan(
                    IndexWriterConfig.OpenMode.CREATE_OR_APPEND,
                    false,
                    UUID.randomUUID());
        }

        Map<String, String> expectedMetadata = compatibilityMetadata(
                modelVersion,
                vectorDimension);
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            Map<String, String> currentMetadata = reader
                    .getIndexCommit()
                    .getUserData();
            boolean matches = expectedMetadata.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(
                            currentMetadata.get(entry.getKey())));
            String currentGeneration = currentMetadata.get(
                    COMMIT_INDEX_GENERATION);
            if (matches && currentGeneration != null) {
                try {
                    return new IndexOpenPlan(
                            IndexWriterConfig.OpenMode.CREATE_OR_APPEND,
                            false,
                            UUID.fromString(currentGeneration));
                } catch (IllegalArgumentException exception) {
                    log.warn(
                            "Lucene post index contains an invalid generation [{}].",
                            currentGeneration);
                }
            }
            return new IndexOpenPlan(
                    IndexWriterConfig.OpenMode.CREATE,
                    true,
                    UUID.randomUUID());
        }
    }

    private Map<String, String> commitMetadata(
            String modelVersion,
            int vectorDimension,
            UUID indexGeneration) {
        return Map.of(
                COMMIT_SCHEMA_VERSION,
                INDEX_SCHEMA_VERSION,
                COMMIT_MODEL_VERSION,
                modelVersion,
                COMMIT_VECTOR_DIMENSION,
                Integer.toString(vectorDimension),
                COMMIT_INDEX_GENERATION,
                indexGeneration.toString());
    }

    private Map<String, String> compatibilityMetadata(
            String modelVersion,
            int vectorDimension) {
        return Map.of(
                COMMIT_SCHEMA_VERSION,
                INDEX_SCHEMA_VERSION,
                COMMIT_MODEL_VERSION,
                modelVersion,
                COMMIT_VECTOR_DIMENSION,
                Integer.toString(vectorDimension));
    }

    private void requireVectorDimension(
            float[] vector,
            int expectedDimension) {
        if (vector == null || vector.length != expectedDimension) {
            throw new AiSearchRuntimeException(
                    "Expected vector dimension ["
                            + expectedDimension
                            + "] but received ["
                            + (vector == null ? 0 : vector.length)
                            + "].");
        }
    }

    private Path resolveIndexDirectory() {
        return Path.of(appProperties.getAi()
                        .getSearch()
                        .getIndexDirectory())
                .toAbsolutePath()
                .normalize();
    }

    private void closeQuietly(
            SearcherManager searcherManager,
            IndexWriter indexWriter,
            Directory directory) {
        closeQuietly(searcherManager, "searcher manager");
        closeQuietly(indexWriter, "index writer");
        closeQuietly(directory, "index directory");
    }

    private void closeQuietly(
            AutoCloseable resource,
            String resourceName) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception exception) {
            log.warn(
                    "Unable to close Lucene {} cleanly.",
                    resourceName,
                    exception);
        }
    }

    @FunctionalInterface
    private interface IndexMutation {

        void apply(RuntimeResources resources) throws IOException;
    }

    @FunctionalInterface
    private interface IndexOperation<T> {

        T execute(RuntimeResources resources) throws IOException;
    }

    private record IndexOpenPlan(
            IndexWriterConfig.OpenMode openMode,
            boolean recreate,
            UUID indexGeneration) {
    }

    private record RuntimeResources(
            Directory directory,
            IndexWriter indexWriter,
            SearcherManager searcherManager,
            Path indexDirectory,
            String modelVersion,
            int vectorDimension,
            UUID indexGeneration) {
    }
}
