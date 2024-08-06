package mr.tracktrace.adapter.internal;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
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
public class AuthTokenDDBItem extends DDBItem {
    @NonNull
    @DynamoDBHashKey(attributeName = TRACK_ID_KEY)
    private String tokenName;

    @NonNull
    @DynamoDBAttribute(attributeName = TRACK_NAME_KEY)
    private String authToken;

    @NonNull
    @DynamoDBAttribute(attributeName = TIMESTAMP_KEY)
    private Long timestamp;
}
