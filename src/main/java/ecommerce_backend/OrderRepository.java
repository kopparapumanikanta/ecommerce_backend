package ecommerce_backend;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByCustomerUsernameOrderByCreatedAtDesc(String customerUsername);

    @Query("""
            select new ecommerce_backend.ProductSalesSummary(p.name, sum(oi.quantity), sum(oi.quantity * oi.unitPrice))
            from OrderItem oi join oi.product p
            group by p.name
            order by sum(oi.quantity) desc
            """)
    List<ProductSalesSummary> salesByProduct();
}
