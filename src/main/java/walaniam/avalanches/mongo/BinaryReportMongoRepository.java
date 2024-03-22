package walaniam.avalanches.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import walaniam.avalanches.persistence.BinaryReport;
import walaniam.avalanches.persistence.BinaryReportRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;

public class BinaryReportMongoRepository implements BinaryReportRepository {

    private static final Set<IndexConfig> REQUIRED_INDEXES = Collections.emptySet();

    private static final String DB_NAME = "avalanche";
    private static final String COLLECTION_NAME = "binaries";

    private final ExecutionContext context;
    private final MongoClientExecutor<BinaryReport> mongoExecutor;

    public BinaryReportMongoRepository(ExecutionContext context, String connectionString) {
        this.context = context;
        this.mongoExecutor = new MongoClientExecutor<>(connectionString, DB_NAME, COLLECTION_NAME, BinaryReport.class);
        this.mongoExecutor.execute(collection -> {

            Set<String> existingIndexes = collection.listIndexes()
                .map(Document::toBsonDocument)
                .map(it -> it.getString("name"))
                .map(BsonString::getValue)
                .into(new TreeSet<>());

            logInfo(context, "Existing indexes=%s", existingIndexes);

            Set<IndexConfig> missingIndexes = REQUIRED_INDEXES.stream()
                .filter(it -> !existingIndexes.contains(it.mongoName()))
                .collect(Collectors.toSet());

            missingIndexes.forEach(index -> {
                String name = index.name();
                boolean unique = index.unique();
                var indexOptions = new IndexOptions().unique(unique);
                if (index.ascending()) {
                    var createdName = collection.createIndex(Indexes.ascending(name), indexOptions);
                    logInfo(context, "created ascending index: %s, unique=%s", createdName, unique);
                } else {
                    var createdName = collection.createIndex(Indexes.descending(name), indexOptions);
                    logInfo(context, "created descending index: %s, unique=%s", createdName, unique);
                }
            });
        });
    }

    @Override
    public void upsert(BinaryReport document) {
        logInfo(context, "Saving binary: %s", document);
        mongoExecutor.execute(collection -> {
            InsertOneResult insertResult = collection.insertOne(document);
            logInfo(context, "Inserted: %s", insertResult);
        });
    }

    @Override
    public Optional<BinaryReport> findByDay(LocalDate day) {
        return mongoExecutor.executeWithResult(collection -> {
            Bson filter = Filters.eq("day", day);
            logInfo(context, "find pdf report by filter=%s", filter);
            FindIterable<BinaryReport> documents = collection.find(filter);
            BinaryReport binaryReport = documents.first();
            logInfo(context, "Found report: %s", binaryReport);
            return Optional.ofNullable(binaryReport);
        });
    }

    @Override
    public List<BinaryReport> getLatest(int limit) {
        logInfo(context, "Getting %s latest reports", limit);
        if (limit < 0 || limit > 10) {
            throw new IllegalArgumentException("Limit must be in <0, 1000>");
        }
        return mongoExecutor.executeWithResult(collection -> collection
            .find(BinaryReport.class)
            .sort(Sorts.descending("day"))
            .limit(limit)
            .into(new ArrayList<>()));
    }
}
