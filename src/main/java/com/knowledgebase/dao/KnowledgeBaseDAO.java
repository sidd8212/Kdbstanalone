package com.knowledgebase.dao;

import com.knowledgebase.model.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseDAO extends JpaRepository<KnowledgeEntry, Long> {
    
    @Query("SELECT k FROM KnowledgeEntry k WHERE k.parent IS NULL")
    List<KnowledgeEntry> findRootEntries();
    
    List<KnowledgeEntry> findByParentId(Long parentId);
}