package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
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
import java.util.NoSuchElementException;
import java.util.Optional;
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
                .trackURI(songItem.getTrackURI())
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
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
                readItemRetryPolicy, () -> dynamoDBMapper.load(SongItemDDBItem.class, songItem.getTrackURI()));

        SongItemDDBItem songItemDDBItem;
        try {
            songItemDDBItem = readItemCallable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return songItemDDBItem != null;
    }

    public Optional<Long> tryGetExistingTimestamp(SongItem songItem) {
        SongItemDDBItem queryItem = SongItemDDBItem.builder()
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .build();

        DynamoDBQueryExpression<SongItemDDBItem> queryExpression = new DynamoDBQueryExpression<SongItemDDBItem>()
                .withHashKeyValues(queryItem)
                .withIndexName(DDBItem.GSI_NAME)
                .withConsistentRead(false);

        Callable<PaginatedQueryList<SongItemDDBItem>> getExistingSongItemCallable = Retry.decorateCallable(
                readItemRetryPolicy, () -> dynamoDBMapper.query(SongItemDDBItem.class, queryExpression));

        PaginatedQueryList<SongItemDDBItem> result;
        try {
            result = getExistingSongItemCallable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        try {
            SongItemDDBItem songItemDDBItem = result.getFirst();
            return Optional.of(songItemDDBItem.getTimestamp());
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
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
