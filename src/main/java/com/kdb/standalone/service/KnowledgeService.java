package com.kdb.standalone.service;

import com.kdb.standalone.entity.KnowledgeEntry;
import com.kdb.standalone.repository.KnowledgeEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class KnowledgeService {
    
    @Autowired
    private KnowledgeEntryRepository repository;
    
    @PostConstruct
    public void init() {
        // Add sample data if database is empty
        if (repository.count() == 0) {
            KnowledgeEntry topic1 = new KnowledgeEntry(null, "Java Programming");
            topic1 = repository.save(topic1);
            
            repository.save(new KnowledgeEntry(topic1.getId(), "Object-Oriented Programming concepts"));
            repository.save(new KnowledgeEntry(topic1.getId(), "Java Collections Framework"));
            
            KnowledgeEntry topic2 = new KnowledgeEntry(null, "Web Development");
            topic2 = repository.save(topic2);
            
            repository.save(new KnowledgeEntry(topic2.getId(), "HTML, CSS, and JavaScript"));
            repository.save(new KnowledgeEntry(topic2.getId(), "RESTful API design"));
            
            KnowledgeEntry topic3 = new KnowledgeEntry(null, "Database Management");
            topic3 = repository.save(topic3);
            
            repository.save(new KnowledgeEntry(topic3.getId(), "SQL basics and queries"));
            repository.save(new KnowledgeEntry(topic3.getId(), "Database normalization"));
        }
    }
    
    public List<KnowledgeEntry> getAllEntries() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<KnowledgeEntry> getTopLevelEntries() {
        return repository.findByParentIdIsNull();
    }
    
    public List<KnowledgeEntry> getChildEntries(Long parentId) {
        return repository.findByParentId(parentId);
    }
    
    public KnowledgeEntry saveEntry(KnowledgeEntry entry) {
        return repository.save(entry);
    }
    
    public void deleteEntry(Long id) {
        repository.deleteById(id);
    }
    
    public void incrementLikes(Long id) {
        repository.findById(id).ifPresent(entry -> {
            entry.incrementLikes();
            repository.save(entry);
        });
    }
}
