package com.kdb.standalone.repository;

import com.kdb.standalone.entity.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {
    
    List<KnowledgeEntry> findByParentIdIsNull();
    
    List<KnowledgeEntry> findByParentId(Long parentId);
    
    List<KnowledgeEntry> findAllByOrderByCreatedAtDesc();
}
