package ecommerce_backend;

public record EventMessage(String eventId, String type, String entityId, String detail, String occurredAt) {
}
