package com.knowledgebase.repository;

import com.knowledgebase.model.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {

    @Query("SELECT k FROM KnowledgeEntry k WHERE k.parent IS NULL")
    List<KnowledgeEntry> findRootEntries();

    List<KnowledgeEntry> findByParentId(Long parentId);

    @Query("SELECT k FROM KnowledgeEntry k WHERE LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(k.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<KnowledgeEntry> searchEntries(@Param("query") String query);

    @Query("SELECT k FROM KnowledgeEntry k WHERE k.parent IS NULL AND (LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(k.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<KnowledgeEntry> searchRootEntries(@Param("query") String query);
}