package ro.unibuc.prodeng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.unibuc.prodeng.exception.EntityNotFoundException;

import ro.unibuc.prodeng.model.ComponentEntity;
import ro.unibuc.prodeng.repository.ComponentRepository;
import ro.unibuc.prodeng.request.CreateComponentRequest;
import ro.unibuc.prodeng.request.EditComponentRequest;
import ro.unibuc.prodeng.response.ComponentResponse;
import ro.unibuc.prodeng.request.CreateComponentRequest;


import java.time.Instant;
import java.util.List;

@Service
public class ComponentService {

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private MongoTemplate mongoTemplate; 

    public Page<ComponentResponse> getComponents(
            String category, 
            Boolean isConsumable, 
            Boolean available, 
            String search, 
            int page, 
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Query query = new Query().with(pageable);
        query.addCriteria(Criteria.where("active").is(true));

        if (category != null && !category.isBlank()) {
            query.addCriteria(Criteria.where("category").is(category));
        }
        
        if (isConsumable != null) {
            query.addCriteria(Criteria.where("isConsumable").is(isConsumable));
        }
        
        if (Boolean.TRUE.equals(available)) {
            query.addCriteria(Criteria.where("availableQuantity").gt(0));
        }
  
        if (search != null && !search.isBlank()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("name").regex(search, "i"),
                    Criteria.where("description").regex(search, "i")
            );
            query.addCriteria(searchCriteria);
        }

        List<ComponentEntity> entities = mongoTemplate.find(query, ComponentEntity.class);
        
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ComponentEntity.class);

        List<ComponentResponse> responses = entities.stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, total);
    }

 
    private ComponentResponse toResponse(ComponentEntity entity) {
        return new ComponentResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getPhotoUrls(), 
                entity.getQuantity(),
                entity.getAvailableQuantity(),
                entity.getIsConsumable(), 
                entity.getTags(),
                entity.getInfoMarkdown(),
                entity.getCreatedAt()
        );
    }

    public ComponentResponse getComponentById(String id) throws EntityNotFoundException {
        ComponentEntity entity = componentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
        
        return toResponse(entity);
    }
   

    public ComponentResponse createComponent(CreateComponentRequest request) {
        
        ComponentEntity newEntity = new ComponentEntity();
   
        newEntity.setName(request.name());
        newEntity.setDescription(request.description());
        newEntity.setCategory(request.category());
        newEntity.setPhotoUrls(request.photoUrls());
        newEntity.setQuantity(request.quantity());
        
        newEntity.setAvailableQuantity(request.quantity()); 
        newEntity.setIsConsumable(request.isConsumable()); 
        newEntity.setTags(request.tags());
        newEntity.setInfoMarkdown(request.infoMarkdown());
        newEntity.setCreatedAt(Instant.now());

       ComponentEntity savedEntity = componentRepository.save(newEntity);

        return toResponse(savedEntity);
    }

    public ComponentResponse updateComponent(String id, EditComponentRequest request) throws EntityNotFoundException {
        ComponentEntity existing = componentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
        if (!existing.getIsConsumable().equals(request.isConsumable())) {
            
           //TODO LOAN 
           // boolean hasActiveLoans = loanRepository.existsByComponentIdAndStatus(id, "ACTIVE");
            boolean hasActiveLoans = false; 
            if (hasActiveLoans) {
                throw new IllegalArgumentException("Cannot change isConsumable status because the component has active loans.");
            }
        }
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setCategory(request.category());
        existing.setPhotoUrls(request.photoUrls());
        existing.setIsConsumable(request.isConsumable());
        existing.setTags(request.tags());
        existing.setInfoMarkdown(request.infoMarkdown());

        ComponentEntity savedEntity = componentRepository.save(existing);

        return toResponse(savedEntity);
    }
    public void deleteComponent(String id) throws EntityNotFoundException {

        ComponentEntity existing = componentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        // TODO boolean hasActiveLoans = loanRepository.existsByComponentIdAndStatus(id, "ACTIVE");
        boolean hasActiveLoans = false; 

        if (hasActiveLoans) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete component with active loans.");
        }
        existing.setActive(false);
        componentRepository.save(existing);
    }
}