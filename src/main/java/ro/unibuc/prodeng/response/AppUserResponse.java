package ro.unibuc.prodeng.response;

public record AppUserResponse(
    String id,
    String name,
    String email,
    String group,
    Boolean isAdmin
) {}
