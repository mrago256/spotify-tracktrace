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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@DynamoDBTable(tableName = DDBItem.TABLE_NAME)
public class AuthTokenDDBItem extends DDBItem {
    @DynamoDBHashKey(attributeName = TRACK_NAME_KEY)
    private String tokenName;

    @DynamoDBRangeKey(attributeName = TRACK_ARTIST_KEY)
    private String tokenType;

    @DynamoDBAttribute(attributeName = "token")
    private String token;

    @DynamoDBAttribute(attributeName = TIMESTAMP_KEY)
    private Long timestamp;
}
