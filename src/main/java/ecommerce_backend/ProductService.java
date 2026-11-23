package ecommerce_backend;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;
    private final EventStore eventStore;

    public ProductService(ProductRepository productRepository, EventPublisher eventPublisher, EventStore eventStore) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.eventStore = eventStore;
    }

    @Cacheable("products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Product " + id + " not found"));
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(ProductRequest request) {
        Product saved = productRepository.save(new Product(request.name(), request.price(), request.stock()));
        eventPublisher.publish(KafkaConfig.PRODUCT_CREATED, "PRODUCT_CREATED", String.valueOf(saved.getId()), saved.getName());
        return saved;
    }

    public List<Product> getLowStock(int threshold) {
        return productRepository.findLowStock(threshold);
    }

    public List<EventRecord> getEvents(Long productId) {
        getProduct(productId);
        return eventStore.findByEntityId(String.valueOf(productId));
    }
}
