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
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.AvalancheReportRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;

public class AvalancheReportMongoRepository implements AvalancheReportRepository {

    private static final Set<IndexConfig> REQUIRED_INDEXES = Set.of(
        new IndexConfig("date", false, false),
        new IndexConfig("expirationDate", false, false)
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
    public List<AvalancheReport> getLatest(int skip, int limit) {
        logInfo(context, "Getting %s latest reports", limit);
        if (limit < 0 || limit > 1000) {
            throw new IllegalArgumentException("Limit must be in <0, 1000>");
        }
        return mongoExecutor.executeWithResult(collection -> collection
            .find(AvalancheReport.class)
            .sort(Sorts.descending("expirationDate"))
            .skip(skip)
            .limit(limit)
            .into(new ArrayList<>()));
    }

    @Override
    public List<AvalancheReport> getLatest(String region, int skip, int limit) {
        logInfo(context, "Getting %s latest reports for region %s", limit, region);
        if (limit < 0 || limit > 1000) {
            throw new IllegalArgumentException("Limit must be in <0, 1000>");
        }
        return mongoExecutor.executeWithResult(collection -> collection
            .find(Filters.eq("_id.regionId", region), AvalancheReport.class)
            .sort(Sorts.descending("expirationDate"))
            .skip(skip)
            .limit(limit)
            .into(new ArrayList<>()));
    }

    @Override
    public Optional<AvalancheReport> find(String region, LocalDate day) {
        return mongoExecutor.executeWithResult(collection -> {
            Bson filter = Filters.and(
                Filters.eq("_id.regionId", region),
                Filters.gte("reportExpirationDate", day.atStartOfDay()),
                Filters.lt("reportExpirationDate", day.plusDays(1).atStartOfDay())
            );
            logInfo(context, "find report by filter=%s", filter);
            FindIterable<AvalancheReport> documents = collection.find(filter);
            AvalancheReport report = documents.first();
            logInfo(context, "Found report: %s", report);
            return Optional.ofNullable(report);
        });
    }
}
