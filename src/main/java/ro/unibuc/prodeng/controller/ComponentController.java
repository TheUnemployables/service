package ro.unibuc.prodeng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.CreateComponentRequest;
import ro.unibuc.prodeng.response.ComponentResponse; 
import ro.unibuc.prodeng.service.ComponentService;
import ro.unibuc.prodeng.request.CreateComponentRequest;
import ro.unibuc.prodeng.request.EditComponentRequest;

import org.springframework.http.HttpStatus;
//import org.springframework.security.access.prepost.PreAuthorize;



@RestController
@RequestMapping("/api/components") 
public class ComponentController {

    @Autowired
    private ComponentService componentService;

    @GetMapping
    public ResponseEntity<Page<ComponentResponse>> getComponents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isConsumable,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ComponentResponse> components = componentService.getComponents(
                category, isConsumable, available, search, page, size
        );
        return ResponseEntity.ok(components);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComponentResponse> getComponentById(@PathVariable String id) throws EntityNotFoundException {
        ComponentResponse component = componentService.getComponentById(id);
        return ResponseEntity.ok(component);
    }

    @PostMapping
    //@PreAuthorize("hasRole('STAFF')") //After staff role
    public ResponseEntity<ComponentResponse> createComponent(@Valid @RequestBody CreateComponentRequest request) {
        ComponentResponse component = componentService.createComponent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(component);
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasRole('STAFF')") 
    public ResponseEntity<ComponentResponse> updateComponent(
            @PathVariable String id,
            @Valid @RequestBody EditComponentRequest request) throws EntityNotFoundException {
        ComponentResponse updatedComponent = componentService.updateComponent(id, request);
        return ResponseEntity.ok(updatedComponent);
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('STAFF')") 
    public ResponseEntity<Void> deleteComponent(@PathVariable String id) throws EntityNotFoundException {
        componentService.deleteComponent(id);
        return ResponseEntity.noContent().build();
    }
}