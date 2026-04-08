package ro.unibuc.prodeng.request;

public record ComponentRequest(
    String id,
    String name,
    String description,
    String category,
    Integer stock,
    Boolean isConsumable
) {}