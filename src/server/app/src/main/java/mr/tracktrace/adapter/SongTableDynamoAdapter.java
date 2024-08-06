package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import mr.tracktrace.adapter.internal.AuthTokenDDBItem;
import mr.tracktrace.adapter.internal.DDBItem;
import mr.tracktrace.adapter.internal.SongItemDDBItem;
import mr.tracktrace.model.SongItem;

import java.time.Instant;
import java.util.concurrent.Callable;

@Singleton
public class SongTableDynamoAdapter {
    private static final RetryConfig retryPolicyConfig = RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(5L, 2))
            .build();

    private final DynamoDBMapper dynamoDBMapper;
    private final Retry writeItemRetryPolicy;
    private final Retry readItemRetryPolicy;

    @Inject
    public SongTableDynamoAdapter(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.writeItemRetryPolicy = Retry.of("write-item-retry", retryPolicyConfig);
        this.readItemRetryPolicy = Retry.of("read-item-retry", retryPolicyConfig);
    }

    public void writeSongToTable(SongItem songItem, Instant firstListened) {
        SongItemDDBItem songItemDDBItem = SongItemDDBItem.builder()
                .trackId(songItem.getTrackId())
                .trackName(songItem.getTrackName())
                .timestamp(firstListened.getEpochSecond())
                .build();

        saveItemToTable(songItemDDBItem);
    }

    public void writeAccessTokenToTable(String token) {
        AuthTokenDDBItem authTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("auth-token")
                .authToken(token)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        saveItemToTable(authTokenDDBItem);
    }

    public boolean songInTable(SongItem songItem) {
        Callable<SongItemDDBItem> readItemCallable = Retry.decorateCallable(
                readItemRetryPolicy, () -> dynamoDBMapper.load(SongItemDDBItem.class, songItem.getTrackId()));

        SongItemDDBItem songItemDDBItem;
        try {
            songItemDDBItem = readItemCallable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return songItemDDBItem != null;
    }

    private void saveItemToTable(DDBItem ddbItem) {
        Callable<Void> writeItemCallable = Retry.decorateCallable(
                writeItemRetryPolicy, () -> {
                    dynamoDBMapper.save(ddbItem);
                    return null;
                });

        try {
            writeItemCallable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
