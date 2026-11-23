package ecommerce_backend;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final Counter ordersPlaced;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository,
                        EventPublisher eventPublisher, MeterRegistry registry) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.ordersPlaced = registry.counter("orders.placed");
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Order placeOrder(String username, OrderRequest request) {
        Order order = new Order();
        order.setCustomerUsername(username);
        order.setStatus("CREATED");
        order.setCreatedAt(Instant.now());
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.Line line : request.items()) {
            Product product = productRepository.findByIdForUpdate(line.productId())
                    .orElseThrow(() -> new ApiExceptions.NotFoundException("Product " + line.productId() + " not found"));
            if (product.getStock() < line.quantity()) {
                throw new ApiExceptions.InsufficientStockException(
                        "Not enough stock for " + product.getName() + ": requested " + line.quantity() + ", available " + product.getStock());
            }
            product.setStock(product.getStock() - line.quantity());

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setUnitPrice(product.getPrice());
            order.addItem(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        publishAfterCommit(saved);
        ordersPlaced.increment();
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersFor(String username) {
        return orderRepository.findByCustomerUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<ProductSalesSummary> salesReport() {
        return orderRepository.salesByProduct();
    }

    private void publishAfterCommit(Order order) {
        Runnable publish = () -> eventPublisher.publish(
                KafkaConfig.ORDER_CREATED, "ORDER_CREATED", String.valueOf(order.getId()),
                "customer=" + order.getCustomerUsername() + " total=" + order.getTotalAmount());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }
}
