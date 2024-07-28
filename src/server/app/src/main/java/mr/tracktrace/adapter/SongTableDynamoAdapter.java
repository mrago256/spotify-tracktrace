package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import mr.tracktrace.adapter.internal.AuthTokenDDBItem;
import mr.tracktrace.adapter.internal.SongItemDDBItem;
import mr.tracktrace.model.SongItem;

import java.time.Instant;

@Singleton
public class SongTableDynamoAdapter {
    private final DynamoDBMapper dynamoDBMapper;

    @Inject
    public SongTableDynamoAdapter(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void writeSongToTable(SongItem songItem, Instant firstListened) {
        SongItemDDBItem songItemDDBItem = SongItemDDBItem.builder()
                .trackId(songItem.getTrackID())
                .trackName(songItem.getTrackName())
                .timestamp(firstListened.getEpochSecond())
                .build();

        dynamoDBMapper.save(songItemDDBItem);
    }

    public void writeAccessTokenToTable(String token) {
        AuthTokenDDBItem authTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("auth-token")
                .authToken(token)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        dynamoDBMapper.save(authTokenDDBItem);
    }

    public boolean songInTable(SongItem songItem) {
        SongItemDDBItem songItemDDBItem = dynamoDBMapper.load(SongItemDDBItem.class, songItem.getTrackID());

        return songItemDDBItem != null;
    }

    public void writeErrorToTable(String error) {
        try {
            SongItemDDBItem toWrite = SongItemDDBItem.builder()
                    .trackId("Error")
                    .trackName("Error: " + error)
                    .timestamp(Instant.now().getEpochSecond())
                    .build();

            dynamoDBMapper.save(toWrite);
        } catch (Exception ex) {
            System.out.println(Instant.now() + "Service error table write error: " + ex);
        }
    }
}
