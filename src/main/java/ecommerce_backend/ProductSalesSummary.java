package ecommerce_backend;

import java.math.BigDecimal;

public record ProductSalesSummary(String productName, Long unitsSold, BigDecimal revenue) {
}
