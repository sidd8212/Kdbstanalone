package com.knowledgebase.service;

import com.knowledgebase.model.KnowledgeEntry;
import com.knowledgebase.repository.KnowledgeEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class KnowledgeEntryService {

    private final KnowledgeEntryRepository repository;

    @Autowired
    public KnowledgeEntryService(KnowledgeEntryRepository repository) {
        this.repository = repository;
    }

    public List<KnowledgeEntry> getRootEntries() {
        return repository.findRootEntries();
    }

    public List<KnowledgeEntry> getChildEntries(Long parentId) {
        return repository.findByParentId(parentId);
    }

    public Optional<KnowledgeEntry> getEntry(Long id) {
        return repository.findById(id);
    }

    public KnowledgeEntry createEntry(KnowledgeEntry entry) {
        return repository.save(entry);
    }

    public KnowledgeEntry updateEntry(KnowledgeEntry entry) {
        return repository.save(entry);
    }

    public void deleteEntry(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        repository.findById(id).ifPresent(entry -> {
            entry.incrementViewCount();
            repository.save(entry);
        });
    }
}