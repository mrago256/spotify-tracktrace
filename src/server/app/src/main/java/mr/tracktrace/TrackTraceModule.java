package mr.tracktrace;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import mr.tracktrace.adapter.SongTableDynamoAdapter;

public class TrackTraceModule extends AbstractModule {
    @Provides
    @Singleton
    Server getServer(SongTableDynamoAdapter songTableDynamoAdapter) {
        return new Server(songTableDynamoAdapter);
    }

    @Provides
    @Singleton
    SongTableDynamoAdapter getSongTableDynamoAdapter(DynamoDBMapper dynamoDBMapper) {
        return new SongTableDynamoAdapter(dynamoDBMapper);
    }

    @Provides
    @Singleton
    AmazonDynamoDB getAmazonDynamoDB() {
        return AmazonDynamoDBClient.builder()
                .withRegion(Regions.US_EAST_1)
                .withClientConfiguration(new ClientConfiguration())
                .build();
    }

    @Provides
    @Singleton
    DynamoDBMapper getDynamoDBMapper(AmazonDynamoDB ddb) {
        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .build();

        return new DynamoDBMapper(ddb, mapperConfig);
    }
}
