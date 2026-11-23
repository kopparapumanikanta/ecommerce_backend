package ecommerce_backend;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, String status, BigDecimal totalAmount, Instant createdAt, List<Line> items) {

    public record Line(Long productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order order) {
        List<Line> lines = order.getItems().stream()
                .map(i -> new Line(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt(), lines);
    }
}
