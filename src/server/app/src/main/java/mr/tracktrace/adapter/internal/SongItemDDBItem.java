package mr.tracktrace.adapter.internal;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@DynamoDBTable(tableName = DDBItem.TABLE_NAME)
public class SongItemDDBItem extends DDBItem {
    @NonNull
    @DynamoDBHashKey(attributeName = TRACK_NAME_KEY)
    private String trackName;

    @NonNull
    @DynamoDBRangeKey(attributeName = TRACK_ARTIST_KEY)
    private String artistName;

    @NonNull
    @DynamoDBAttribute(attributeName = TIMESTAMP_KEY)
    private Long timestamp;

    @NonNull
    @DynamoDBAttribute(attributeName = TRACK_LISTEN_COUNT_KEY)
    private Integer listens;
}