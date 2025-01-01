package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import mr.tracktrace.adapter.internal.AuthTokenDDBItem;
import mr.tracktrace.adapter.internal.DDBItem;
import mr.tracktrace.adapter.internal.SongItemDDBItem;
import mr.tracktrace.adapter.internal.SongTableReadDDBItem;
import mr.tracktrace.model.SongItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;

@Singleton
public class SongTableDynamoAdapter {
    private static final Logger log = LoggerFactory.getLogger(SongTableDynamoAdapter.class);

    private static final RetryConfig retryPolicyConfig = RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(5L, 2))
            .build();

    private final DynamoDBMapper dynamoDBMapper;
    private final Retry writeItemRetryPolicy;
    private final Retry readItemRetryPolicy;
    private final Retry readRefreshTokenPolicy;

    @Inject
    public SongTableDynamoAdapter(DynamoDBMapper dynamoDBMapper,
                                  @Named("get-refresh-token-retry") RetryConfig refreshTokenRetryPolicyConfig) {

        this.dynamoDBMapper = dynamoDBMapper;

        this.writeItemRetryPolicy = Retry.of("write-item-retry", retryPolicyConfig);
        this.readItemRetryPolicy = Retry.of("read-item-retry", retryPolicyConfig);
        this.readRefreshTokenPolicy = Retry.of("read-refresh-token-retry", refreshTokenRetryPolicyConfig);

        Retry.EventPublisher publisher = readRefreshTokenPolicy.getEventPublisher();
        publisher.onRetry(event -> log.warn(event.toString()));
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
        SongItemDDBItem currentSongItem = getSongItemFromTable(songItem);
        int newListenCount = currentSongItem.getListens() + 1;

        SongItemDDBItem songItemDDBItem = SongItemDDBItem.builder()
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .timestamp(currentSongItem.getTimestamp())
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

    public void writeRefreshTokenToTable(String token) {
        AuthTokenDDBItem authTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("refresh-token")
                .tokenType("auth")
                .token(token)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        saveItemToTable(authTokenDDBItem);
    }

    public Optional<String> tryGetRefreshToken() {
        AuthTokenDDBItem queryItem = AuthTokenDDBItem.builder()
                .tokenName("refresh-token")
                .tokenType("auth")
                .build();

        DynamoDBQueryExpression<AuthTokenDDBItem> queryExpression = new DynamoDBQueryExpression<AuthTokenDDBItem>()
                .withHashKeyValues(queryItem);

        Callable<PaginatedQueryList<AuthTokenDDBItem>> queryCallable = Retry.decorateCallable(
                readRefreshTokenPolicy, () -> dynamoDBMapper.query(AuthTokenDDBItem.class, queryExpression));

        PaginatedQueryList<AuthTokenDDBItem> result;
        try {
            result = queryCallable.call();
            return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst().getToken());
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean songInTable(SongItem songItem) {
        try {
            getSongItemFromTable(songItem);
        } catch (NoSuchElementException ex) {
            return false;
        }

        return true;
    }

    private SongItemDDBItem getSongItemFromTable(SongItem songItem) {
        SongTableReadDDBItem queryItem = SongTableReadDDBItem.builder()
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .build();

        DynamoDBQueryExpression<SongTableReadDDBItem> queryExpression = new DynamoDBQueryExpression<SongTableReadDDBItem>()
                .withHashKeyValues(queryItem);

        Callable<PaginatedQueryList<SongTableReadDDBItem>> queryCallable = Retry.decorateCallable(
                readItemRetryPolicy, () -> dynamoDBMapper.query(SongTableReadDDBItem.class, queryExpression));

        PaginatedQueryList<SongTableReadDDBItem> result;
        try {
            result = queryCallable.call();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }

        return result.getFirst().toSongItemDDBItem();
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
