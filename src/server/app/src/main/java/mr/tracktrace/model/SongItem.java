package mr.tracktrace.model;

import lombok.Builder;
import lombok.Data;
import mr.tracktrace.adapter.internal.SongItemDDBItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

@Data
@Builder
public class SongItem {
    private String trackName;
    private String artistName;

    public static SongItem fromSongItemDDBItem(SongItemDDBItem songItemDDBItem) {
        return SongItem.builder()
                .trackName(songItemDDBItem.getTrackName())
                .artistName(songItemDDBItem.getArtistName())
                .build();
    }

    public static SongItem fromCurrentlyPlaying(CurrentlyPlaying currentlyPlaying) {
        return SongItem.builder()
                .trackName(currentlyPlaying.getItem().getName())
                .build();
    }
}
