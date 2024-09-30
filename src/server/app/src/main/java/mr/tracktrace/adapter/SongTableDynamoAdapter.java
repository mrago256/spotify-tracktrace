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
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .timestamp(firstListened.getEpochSecond())
                .listens(1)
                .build();

        saveItemToTable(songItemDDBItem);
    }

    public void incrementSongListenCount(SongItem songItem) {
        int newListenCount = getSongListenCount(songItem) + 1;
        SongItemDDBItem songItemDDBItem = SongItemDDBItem.builder()
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .listens(newListenCount)
                .build();

        saveItemToTable(songItemDDBItem);
    }

    public void writeAccessTokenToTable(String token) {
        AuthTokenDDBItem authTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("auth-token")
                .tokenType("auth")
                .token(token)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        saveItemToTable(authTokenDDBItem);
    }

    public boolean songInTable(SongItem songItem) {
        try {
            getSongItemFromTable(songItem);
        } catch (NoSuchElementException ex) {
            return false;
        }

        return true;
    }

    private int getSongListenCount(SongItem songItem) {
        SongItemDDBItem songItemDDBItem = getSongItemFromTable(songItem);

        return songItemDDBItem.getListens();
    }

    private SongItemDDBItem getSongItemFromTable(SongItem songItem) {
        SongItemDDBItem queryItem = SongItemDDBItem.builder()
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .build();

        DynamoDBQueryExpression<SongItemDDBItem> queryExpression = new DynamoDBQueryExpression<SongItemDDBItem>()
                .withHashKeyValues(queryItem);

        Callable<PaginatedQueryList<SongItemDDBItem>> queryCallable = Retry.decorateCallable(
                readItemRetryPolicy, () -> dynamoDBMapper.query(SongItemDDBItem.class, queryExpression));

        PaginatedQueryList<SongItemDDBItem> result;
        try {
            result = queryCallable.call();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }

        return result.getFirst();
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
