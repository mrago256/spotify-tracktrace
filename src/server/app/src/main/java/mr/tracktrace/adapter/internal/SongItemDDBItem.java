package mr.tracktrace.adapter.internal;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@DynamoDBTable(tableName = DDBItem.TABLE_NAME)
public class SongItemDDBItem extends DDBItem {
    @DynamoDBHashKey(attributeName = TRACK_URI_KEY)
    private String trackURI;

    @DynamoDBAttribute(attributeName = TIMESTAMP_KEY)
    private Long timestamp;

    @DynamoDBAttribute(attributeName = TRACK_NAME_KEY)
    @DynamoDBIndexHashKey(globalSecondaryIndexName = GSI_NAME)
    private String trackName;

    @DynamoDBAttribute(attributeName = TRACK_ARTIST_KEY)
    @DynamoDBIndexRangeKey(globalSecondaryIndexName = GSI_NAME)
    private String artistName;
}