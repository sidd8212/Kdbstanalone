package com.kdb.standalone.ui;

import com.kdb.standalone.entity.KnowledgeEntry;
import com.kdb.standalone.service.KnowledgeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route("")
public class MainView extends VerticalLayout {
    
    @Autowired
    private KnowledgeService knowledgeService;
    
    private VerticalLayout contentLayout;
    
    public MainView(@Autowired KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        // Header
        H1 header = new H1("Knowledge Database");
        add(header);
        
        // Add entry form
        add(createAddEntrySection());
        
        // Content area
        contentLayout = new VerticalLayout();
        contentLayout.setWidthFull();
        contentLayout.setSpacing(true);
        add(contentLayout);
        
        // Load and display entries
        refreshContent();
    }
    
    private Div createAddEntrySection() {
        Div section = new Div();
        section.setWidthFull();
        
        H3 title = new H3("Add New Topic");
        
        TextField topicField = new TextField("Topic Title");
        topicField.setWidthFull();
        topicField.setPlaceholder("Enter topic title...");
        
        Button addButton = new Button("Add Topic", event -> {
            String topicText = topicField.getValue();
            if (!topicText.isEmpty()) {
                KnowledgeEntry entry = new KnowledgeEntry(null, topicText);
                knowledgeService.saveEntry(entry);
                topicField.clear();
                refreshContent();
            }
        });
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        section.add(title, topicField, addButton);
        return section;
    }
    
    private void refreshContent() {
        contentLayout.removeAll();
        
        List<KnowledgeEntry> topLevelEntries = knowledgeService.getTopLevelEntries();
        
        if (topLevelEntries.isEmpty()) {
            contentLayout.add(new Paragraph("No entries found. Add some topics to get started!"));
            return;
        }
        
        for (KnowledgeEntry entry : topLevelEntries) {
            contentLayout.add(createEntryCard(entry));
        }
    }
    
    private Details createEntryCard(KnowledgeEntry entry) {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);
        
        // Entry content
        Paragraph textContent = new Paragraph(entry.getTextContent());
        
        // Like button
        Button likeButton = new Button("♥ " + entry.getLikeCounter(), event -> {
            knowledgeService.incrementLikes(entry.getId());
            refreshContent();
        });
        likeButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        
        // Add subtopic form
        TextArea subtopicArea = new TextArea("Add Subtopic");
        subtopicArea.setWidthFull();
        subtopicArea.setPlaceholder("Enter subtopic content...");
        
        Button addSubtopicButton = new Button("Add Subtopic", event -> {
            String subtopicText = subtopicArea.getValue();
            if (!subtopicText.isEmpty()) {
                KnowledgeEntry subtopic = new KnowledgeEntry(entry.getId(), subtopicText);
                knowledgeService.saveEntry(subtopic);
                subtopicArea.clear();
                refreshContent();
            }
        });
        addSubtopicButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        
        // Delete button
        Button deleteButton = new Button("Delete", event -> {
            knowledgeService.deleteEntry(entry.getId());
            refreshContent();
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(likeButton, deleteButton);
        
        content.add(textContent, buttonLayout);
        
        // Display subtopics
        List<KnowledgeEntry> children = knowledgeService.getChildEntries(entry.getId());
        if (!children.isEmpty()) {
            VerticalLayout subtopicsLayout = new VerticalLayout();
            subtopicsLayout.setPadding(false);
            subtopicsLayout.getStyle().set("padding-left", "20px");
            
            for (KnowledgeEntry child : children) {
                Div subtopicDiv = new Div();
                
                Paragraph subtopicText = new Paragraph("• " + child.getTextContent());
                
                Button childLikeButton = new Button("♥ " + child.getLikeCounter(), e -> {
                    knowledgeService.incrementLikes(child.getId());
                    refreshContent();
                });
                childLikeButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
                
                Button deleteChildButton = new Button("Delete", e -> {
                    knowledgeService.deleteEntry(child.getId());
                    refreshContent();
                });
                deleteChildButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                
                HorizontalLayout childButtons = new HorizontalLayout(childLikeButton, deleteChildButton);
                
                subtopicDiv.add(subtopicText, childButtons);
                subtopicsLayout.add(subtopicDiv);
            }
            
            content.add(subtopicsLayout);
        }
        
        content.add(subtopicArea, addSubtopicButton);
        
        Details details = new Details(entry.getTextContent() + " (" + children.size() + " subtopics)", content);
        details.setWidthFull();
        
        return details;
    }
}
