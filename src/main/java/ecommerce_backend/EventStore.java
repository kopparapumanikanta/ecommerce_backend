package ecommerce_backend;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class EventStore {

    public static final String ENTITY_INDEX = "entityId-index";

    private final DynamoDbTable<EventRecord> table;

    public EventStore(DynamoDbTable<EventRecord> table) {
        this.table = table;
    }

    public boolean saveIfAbsent(EventMessage event) {
        EventRecord record = new EventRecord();
        record.setEventId(event.eventId());
        record.setType(event.type());
        record.setEntityId(event.entityId());
        record.setDetail(event.detail());
        record.setOccurredAt(event.occurredAt());
        record.setProcessedAt(Instant.now().toString());
        PutItemEnhancedRequest<EventRecord> request = PutItemEnhancedRequest.builder(EventRecord.class)
                .item(record)
                .conditionExpression(Expression.builder().expression("attribute_not_exists(eventId)").build())
                .build();
        try {
            table.putItem(request);
            return true;
        } catch (ConditionalCheckFailedException duplicate) {
            return false;
        }
    }

    public List<EventRecord> findByEntityId(String entityId) {
        QueryConditional condition = QueryConditional.keyEqualTo(Key.builder().partitionValue(entityId).build());
        return table.index(ENTITY_INDEX).query(condition).stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }
}
