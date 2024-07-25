package mr.tracktrace.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SongItem {
    private String trackID;
    private String trackName;
}
