package walaniam.avalanches.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.Function;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.function.Consumer;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@RequiredArgsConstructor
public class MongoClientExecutor<T> {

    private final String connectionString;
    private final String databaseName;
    private final String collectionName;
    private final Class<T> collectionType;

    public <Result> Result executeWithResult(Function<MongoCollection<T>, Result> collectionFunction) {
        try (MongoClient client = newMongoClient()) {
            MongoDatabase db = client.getDatabase(databaseName);
            MongoCollection<T> collection = db.getCollection(collectionName, collectionType);
            return collectionFunction.apply(collection);
        }
    }

    public void execute(Consumer<MongoCollection<T>> collectionConsumer) {
        try (MongoClient client = newMongoClient()) {
            MongoDatabase db = client.getDatabase(databaseName);
            MongoCollection<T> collection = db.getCollection(collectionName, collectionType);
            collectionConsumer.accept(collection);
        }
    }

    private MongoClient newMongoClient() {

        CodecRegistry codecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();

        return MongoClients.create(settings);
    }
}
