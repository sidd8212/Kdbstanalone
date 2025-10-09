package com.knowledgebase.ui;

import com.knowledgebase.model.KnowledgeEntry;
import com.knowledgebase.service.KnowledgeEntryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

@Route("")
public class MainView extends VerticalLayout {

    private final KnowledgeEntryService service;
    private final TreeGrid<KnowledgeEntry> treeGrid;
    private final TextField titleField;
    private final TextArea contentArea;
    private final H4 viewCountLabel;
    private KnowledgeEntry currentEntry;

    @Autowired
    public MainView(KnowledgeEntryService service) {
        this.service = service;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        treeGrid = new TreeGrid<>();
        treeGrid.setSizeFull();
        treeGrid.addHierarchyColumn(KnowledgeEntry::getTitle).setHeader("Knowledge Base");
        
        treeGrid.setDataProvider(new AbstractBackEndHierarchicalDataProvider<KnowledgeEntry, Void>() {
            @Override
            protected Stream<KnowledgeEntry> fetchChildrenFromBackEnd(HierarchicalQuery<KnowledgeEntry, Void> query) {
                KnowledgeEntry parent = query.getParent();
                if (parent == null) {
                    return service.getRootEntries().stream();
                }
                return service.getChildEntries(parent.getId()).stream();
            }

            @Override
            public int getChildCount(HierarchicalQuery<KnowledgeEntry, Void> query) {
                KnowledgeEntry parent = query.getParent();
                if (parent == null) {
                    return service.getRootEntries().size();
                }
                return service.getChildEntries(parent.getId()).size();
            }

            @Override
            public boolean hasChildren(KnowledgeEntry item) {
                return !service.getChildEntries(item.getId()).isEmpty();
            }
        });

        VerticalLayout contentPanel = new VerticalLayout();
        contentPanel.setSizeFull();
        contentPanel.setPadding(true);
        contentPanel.setSpacing(true);

        titleField = new TextField("Title");
        titleField.setWidthFull();

        contentArea = new TextArea("Content");
        contentArea.setWidthFull();
        contentArea.setHeight("500px");

        viewCountLabel = new H4("Views: 0");

        Button newButton = new Button("New Entry", e -> createNewEntry());
        Button saveButton = new Button("Save", e -> saveCurrentEntry());
        HorizontalLayout buttons = new HorizontalLayout(newButton, saveButton);

        contentPanel.add(titleField, contentArea, viewCountLabel, buttons);

        mainLayout.add(treeGrid, contentPanel);
        mainLayout.setFlexGrow(1, treeGrid);
        mainLayout.setFlexGrow(2, contentPanel);

        add(new H2("Knowledge Base"), mainLayout);

        treeGrid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(this::loadEntry);
        });

        titleField.addValueChangeListener(e -> saveCurrentEntry());
        contentArea.addValueChangeListener(e -> saveCurrentEntry());
    }

    private void loadEntry(KnowledgeEntry entry) {
        currentEntry = entry;
        titleField.setValue(entry.getTitle());
        contentArea.setValue(entry.getContent());
        viewCountLabel.setText("Views: " + entry.getViewCount());
        
        service.incrementViewCount(entry.getId());
        treeGrid.getDataProvider().refreshItem(entry);
    }

    private void createNewEntry() {
        KnowledgeEntry parentEntry = treeGrid.getSelectedItems().stream().findFirst().orElse(null);
        
        KnowledgeEntry newEntry = new KnowledgeEntry();
        newEntry.setTitle("New Entry");
        if (parentEntry != null) {
            newEntry.setParent(parentEntry);
        }
        
        newEntry = service.createEntry(newEntry);
        treeGrid.getDataProvider().refreshAll();
        treeGrid.select(newEntry);
    }

    private void saveCurrentEntry() {
        if (currentEntry != null) {
            currentEntry.setTitle(titleField.getValue());
            currentEntry.setContent(contentArea.getValue());
            service.updateEntry(currentEntry);
            treeGrid.getDataProvider().refreshItem(currentEntry);
        }
    }
}