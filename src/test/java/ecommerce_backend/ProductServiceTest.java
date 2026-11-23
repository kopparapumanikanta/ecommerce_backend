package ecommerce_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final EventPublisher publisher = mock(EventPublisher.class);
    private final EventStore eventStore = mock(EventStore.class);
    private final ProductService service = new ProductService(repository, publisher, eventStore);

    @Test
    void createProduct_savesAndPublishesEvent() {
        Product saved = new Product("Phone", new BigDecimal("499.99"), 10);
        saved.setId(7L);
        when(repository.save(any(Product.class))).thenReturn(saved);

        Product result = service.createProduct(new ProductRequest("Phone", new BigDecimal("499.99"), 10));

        assertEquals(7L, result.getId());
        verify(publisher).publish(eq(KafkaConfig.PRODUCT_CREATED), eq("PRODUCT_CREATED"), eq("7"), eq("Phone"));
    }

    @Test
    void getProduct_unknownId_throwsNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ApiExceptions.NotFoundException.class, () -> service.getProduct(42L));
    }

    @Test
    void getEvents_readsFromEventStore() {
        when(repository.findById(3L)).thenReturn(Optional.of(new Product("A", BigDecimal.ONE, 1)));

        service.getEvents(3L);

        verify(eventStore).findByEntityId("3");
    }
}
