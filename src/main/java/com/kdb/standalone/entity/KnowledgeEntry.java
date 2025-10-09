package com.kdb.standalone.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_entries")
public class KnowledgeEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "parent_id")
    private Long parentId;
    
    @Column(name = "text_content", length = 20000)
    private String textContent;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "like_counter")
    private Integer likeCounter;
    
    public KnowledgeEntry() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.likeCounter = 0;
    }
    
    public KnowledgeEntry(Long parentId, String textContent) {
        this();
        this.parentId = parentId;
        this.textContent = textContent;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public String getTextContent() {
        return textContent;
    }
    
    public void setTextContent(String textContent) {
        this.textContent = textContent;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getLikeCounter() {
        return likeCounter;
    }
    
    public void setLikeCounter(Integer likeCounter) {
        this.likeCounter = likeCounter;
    }
    
    public void incrementLikes() {
        this.likeCounter++;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isTopLevel() {
        return parentId == null;
    }
}
