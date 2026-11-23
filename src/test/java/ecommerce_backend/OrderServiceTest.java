package ecommerce_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final EventPublisher publisher = mock(EventPublisher.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(productRepository, orderRepository, publisher, registry);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void placeOrder_reducesStockComputesTotalAndPublishesEvent() {
        Product product = new Product("Cable", new BigDecimal("10.00"), 5);
        product.setId(1L);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        Order order = service.placeOrder("store1", new OrderRequest(List.of(new OrderRequest.Line(1L, 3))));

        assertEquals(2, product.getStock());
        assertEquals(0, new BigDecimal("30.00").compareTo(order.getTotalAmount()));
        assertEquals("store1", order.getCustomerUsername());
        assertEquals(1, order.getItems().size());
        verify(publisher).publish(eq(KafkaConfig.ORDER_CREATED), eq("ORDER_CREATED"), any(), any());
        assertEquals(1.0, registry.counter("orders.placed").count());
    }

    @Test
    void placeOrder_withInsufficientStock_throwsAndDoesNotSave() {
        Product product = new Product("Cable", new BigDecimal("10.00"), 1);
        product.setId(1L);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThrows(ApiExceptions.InsufficientStockException.class,
                () -> service.placeOrder("store1", new OrderRequest(List.of(new OrderRequest.Line(1L, 2)))));

        verify(orderRepository, never()).save(any(Order.class));
        verify(publisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void placeOrder_withUnknownProduct_throwsNotFound() {
        when(productRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThrows(ApiExceptions.NotFoundException.class,
                () -> service.placeOrder("store1", new OrderRequest(List.of(new OrderRequest.Line(9L, 1)))));
    }
}
