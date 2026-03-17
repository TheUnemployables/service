package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateComponentRequest(
    @NotBlank(message = "Numele este obligatoriu")
    String name,
    
    String description,
    
    @NotBlank(message = "Categoria este obligatorie")
    String category,
    
    List<String> photoUrls,
    
    @NotNull(message = "Cantitatea este obligatorie")
    @Min(value = 0, message = "Cantitatea nu poate fi negativă")
    Integer quantity,
    
    @NotNull(message = "isConsumable este obligatoriu")
    Boolean isConsumable,
    
    List<String> tags,
    
    String infoMarkdown
) {
}