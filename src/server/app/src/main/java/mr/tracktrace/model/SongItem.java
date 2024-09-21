package mr.tracktrace.model;

import lombok.Builder;
import lombok.Data;
import mr.tracktrace.adapter.internal.SongItemDDBItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

@Data
@Builder
public class SongItem {
    private String trackURI;
    private String trackName;
    private String artistName;

    public static SongItem fromSongItemDDBItem(SongItemDDBItem songItemDDBItem) {
        return SongItem.builder()
                .trackURI(songItemDDBItem.getTrackURI())
                .trackName(songItemDDBItem.getTrackName())
                .artistName(songItemDDBItem.getArtistName())
                .build();
    }

    public static SongItem fromCurrentlyPlaying(CurrentlyPlaying currentlyPlaying) {
        return SongItem.builder()
                .trackURI(currentlyPlaying.getItem().getUri())
                .trackName(currentlyPlaying.getItem().getName())
                .build();
    }

    public static String getTrackId(SongItem songItem) {
        String prefix = "spotify:track:";

        return songItem.trackURI.replace(prefix, "");
    }
}
