package com.knowledgebase.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_entries")
@Data
public class KnowledgeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private KnowledgeEntry parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<KnowledgeEntry> children = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Column(length = 20000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "view_count")
    private int viewCount = 0;

    public void incrementViewCount() {
        this.viewCount++;
    }

    // Helper method to manage bidirectional relationship
    public void addChild(KnowledgeEntry child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(KnowledgeEntry child) {
        children.remove(child);
        child.setParent(null);
    }
}