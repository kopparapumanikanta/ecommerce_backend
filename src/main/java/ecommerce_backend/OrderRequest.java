package ecommerce_backend;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(@NotEmpty @Valid List<Line> items) {

    public record Line(@NotNull Long productId, @Min(1) int quantity) {
    }
}
