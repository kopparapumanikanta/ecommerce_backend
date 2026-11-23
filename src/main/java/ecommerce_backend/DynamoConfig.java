package ecommerce_backend;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.Projection;

@Configuration
public class DynamoConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(
            @Value("${dynamodb.endpoint:}") String endpoint,
            @Value("${aws.region:us-east-1}") String region) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(Region.of(region));
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    @Bean
    public DynamoDbTable<EventRecord> eventTable(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbClient client,
            @Value("${dynamodb.table:event_records}") String tableName) {
        DynamoDbTable<EventRecord> table = enhancedClient.table(tableName, TableSchema.fromBean(EventRecord.class));
        ProvisionedThroughput throughput = ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build();
        EnhancedGlobalSecondaryIndex entityIndex = EnhancedGlobalSecondaryIndex.builder()
                .indexName(EventStore.ENTITY_INDEX)
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .provisionedThroughput(throughput)
                .build();
        try {
            table.createTable(CreateTableEnhancedRequest.builder()
                    .provisionedThroughput(throughput)
                    .globalSecondaryIndices(entityIndex)
                    .build());
            client.waiter().waitUntilTableExists(r -> r.tableName(tableName));
        } catch (ResourceInUseException alreadyExists) {
        }
        return table;
    }
}
