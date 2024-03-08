package walaniam.avalanches.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonString;
import org.bson.Document;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.AvalancheReportRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;

public class AvalancheReportMongoRepository implements AvalancheReportRepository {

    private static final Set<IndexConfig> REQUIRED_INDEXES = Set.of(
        new IndexConfig("date", true, true),
        new IndexConfig("expirationDate", true, true),
        new IndexConfig("date", false, true),
        new IndexConfig("expirationDate", false, true)
    );

    private static final String DB_NAME = "avalanche";
    private static final String COLLECTION_NAME = "reports";

    private final ExecutionContext context;
    private final MongoClientExecutor<AvalancheReport> mongoExecutor;

    public AvalancheReportMongoRepository(ExecutionContext context, String connectionString) {
        this.context = context;
        this.mongoExecutor = new MongoClientExecutor<>(connectionString, DB_NAME, COLLECTION_NAME, AvalancheReport.class);
        this.mongoExecutor.execute(collection -> {

//            dropIndexes(collection);

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

//    private static void dropIndexes(MongoCollection<AvalancheReport> collection) {
//        if (collection.countDocuments() == 0) {
//            Set<String> indexes = collection.listIndexes()
//                .map(Document::toBsonDocument)
//                .map(it -> it.getString("name"))
//                .map(BsonString::getValue)
//                .into(new TreeSet<>());
//            for (String existingIndex : indexes) {
//                for (IndexConfig indexConfig : REQUIRED_INDEXES) {
//                    if (existingIndex.contains(indexConfig.name())) {
//                        collection.dropIndex(existingIndex);
//                        break;
//                    }
//                }
//            }
//        }
//    }

    @Override
    public void save(AvalancheReport report) {
        logInfo(context, "Saving report: %s", report);
        mongoExecutor.execute(collection -> {
            InsertOneResult insertResult = collection.insertOne(report);
            logInfo(context, "Inserted: %s", insertResult);
        });
    }

    @Override
    public List<AvalancheReport> getLatest(int limit) {
        logInfo(context, "Getting %s latest reports", limit);
        if (limit < 0 || limit > 1000) {
            throw new IllegalArgumentException("Limit must be in <0, 1000>");
        }
        return mongoExecutor.executeWithResult(collection -> collection
            .find(AvalancheReport.class)
            .sort(Sorts.descending("expirationDate"))
            .limit(limit)
            .into(new ArrayList<>()));
    }

    private record IndexConfig(
        String name,
        boolean ascending,
        boolean unique
    ) {

        String mongoName() {
            var suffix = ascending ? "_1" : "_-1";
            return name + suffix;
        }

    }
}
