package ecommerce_backend;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String timestamp, int status, String error, String message, Map<String, String> fieldErrors) {
}
