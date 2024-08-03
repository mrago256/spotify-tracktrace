package mr.tracktrace.model;

import lombok.Builder;
import lombok.Data;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

@Data
@Builder
public class SongItem {
    private String trackID;
    private String trackName;

    public static SongItem fromCurrentlyPlaying(CurrentlyPlaying currentlyPlaying) {
        return SongItem.builder()
                .trackID(currentlyPlaying.getItem().getUri())
                .trackName(currentlyPlaying.getItem().getName())
                .build();
    }
}
