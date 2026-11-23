package ecommerce_backend;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(Principal principal, @Valid @RequestBody OrderRequest request) {
        return OrderResponse.from(orderService.placeOrder(principal.getName(), request));
    }

    @GetMapping("/mine")
    public List<OrderResponse> myOrders(Principal principal) {
        return orderService.getOrdersFor(principal.getName()).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/reports/sales")
    public List<ProductSalesSummary> salesReport() {
        return orderService.salesReport();
    }
}
