package mr.tracktrace;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.inject.Named;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TrackTraceModule extends AbstractModule {
    @Provides
    @Singleton
    SpotifyApi getSpotifyApiClient() {
        URI redirectURI = SpotifyHttpManager.makeUri("http://localhost:8080/");
        return SpotifyApi.builder()
                .setClientId(System.getenv("spotifyClientId"))
                .setClientSecret(System.getenv("spotifyClientSecret"))
                .setRedirectUri(redirectURI)
                .build();
    }

    @Provides
    @Singleton
    AmazonDynamoDB getAmazonDynamoDB() {
        ClientConfiguration clientConfiguration = new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY);
        return AmazonDynamoDBClient.builder()
                .withRegion(Regions.US_EAST_1)
                .withClientConfiguration(clientConfiguration)
                .build();
    }

    @Provides
    @Singleton
    DynamoDBMapper getDynamoDBMapper(AmazonDynamoDB ddb) {
        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES)
                .build();

        return new DynamoDBMapper(ddb, mapperConfig);
    }

    @Provides
    @Singleton
    @Named("get-refresh-token-retry")
    RetryConfig getRefreshTokenRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(25)
                .waitDuration(Duration.ofSeconds(20))
                .build();
    }

    @Provides
    @Singleton
    ScheduledExecutorService getScheduledExecutorService() {
        return Executors.newScheduledThreadPool(2);
    }
}
