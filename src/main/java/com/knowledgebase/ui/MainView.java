package com.knowledgebase.ui;

import com.knowledgebase.model.KnowledgeEntry;
import com.knowledgebase.service.KnowledgeEntryService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Route("")
@PageTitle("Knowledge Base")
public class MainView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final KnowledgeEntryService service;

    private TreeGrid<KnowledgeEntry> treeGrid;
    private TextField searchField;
    private TextField titleField;
    private TextArea contentArea;
    private Span viewCountBadge;
    private Span createdAtSpan;
    private Span updatedAtSpan;
    private HorizontalLayout breadcrumbBar;
    private VerticalLayout contentPanel;
    private Div emptyStatePanel;
    private String currentSearchQuery = "";

    private KnowledgeEntry currentEntry;

    @Autowired
    public MainView(KnowledgeEntryService service) {
        this.service = service;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildHeader(), buildMainLayout());
    }

    // ─── Header ──────────────────────────────────────────────────────────────

    private HorizontalLayout buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("background-color", "#1565C0")
                .set("color", "white")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.2)");

        Icon kbIcon = VaadinIcon.BOOK.create();
        kbIcon.getStyle().set("color", "white");

        H2 title = new H2("Knowledge Base");
        title.getStyle()
                .set("color", "white")
                .set("margin", "0")
                .set("font-size", "1.4rem");

        Span subtitle = new Span("Standalone Edition");
        subtitle.getStyle()
                .set("color", "rgba(255,255,255,0.7)")
                .set("font-size", "0.85rem")
                .set("margin-left", "8px")
                .set("align-self", "flex-end")
                .set("padding-bottom", "2px");

        header.add(kbIcon, title, subtitle);
        return header;
    }

    // ─── Main two-panel layout ────────────────────────────────────────────────

    private HorizontalLayout buildMainLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSizeFull();
        layout.setSpacing(false);
        layout.setPadding(false);

        VerticalLayout leftPanel = buildLeftPanel();
        VerticalLayout rightPanel = buildRightPanel();

        layout.add(leftPanel, rightPanel);
        layout.setFlexGrow(1, leftPanel);
        layout.setFlexGrow(3, rightPanel);

        return layout;
    }

    // ─── Left panel (navigation tree + search) ────────────────────────────────

    private VerticalLayout buildLeftPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.getStyle()
                .set("border-right", "1px solid #e0e0e0")
                .set("background-color", "#fafafa")
                .set("min-width", "240px")
                .set("max-width", "320px");

        // Panel header
        HorizontalLayout panelHeader = new HorizontalLayout();
        panelHeader.setWidthFull();
        panelHeader.setPadding(true);
        panelHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        panelHeader.getStyle()
                .set("border-bottom", "1px solid #e0e0e0")
                .set("background-color", "#f5f5f5");

        Span topicsLabel = new Span("Topics");
        topicsLabel.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem")
                .set("color", "#424242");

        panelHeader.add(topicsLabel);

        // Search field
        searchField = new TextField();
        searchField.setPlaceholder("Search topics...");
        searchField.setWidthFull();
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.getStyle().set("padding", "8px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> {
            currentSearchQuery = e.getValue();
            treeGrid.getDataProvider().refreshAll();
        });

        // Tree grid
        treeGrid = new TreeGrid<>();
        treeGrid.setSizeFull();
        treeGrid.addHierarchyColumn(KnowledgeEntry::getTitle)
                .setHeader("Title")
                .setFlexGrow(1);
        treeGrid.addColumn(entry -> service.getChildEntries(entry.getId()).size() + " children")
                .setHeader("Children")
                .setWidth("90px")
                .setFlexGrow(0);
        treeGrid.setDataProvider(buildTreeDataProvider());
        treeGrid.addSelectionListener(event -> event.getFirstSelectedItem().ifPresent(this::loadEntry));

        // Action buttons row
        Button newRootBtn = new Button("New Topic", VaadinIcon.PLUS.create(), e -> createNewEntry(null));
        newRootBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        newRootBtn.setWidthFull();

        Button newChildBtn = new Button("New Child", VaadinIcon.LEVEL_DOWN.create(), e -> {
            KnowledgeEntry selected = treeGrid.getSelectedItems().stream().findFirst().orElse(null);
            createNewEntry(selected);
        });
        newChildBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        newChildBtn.setWidthFull();

        HorizontalLayout actionButtons = new HorizontalLayout(newRootBtn, newChildBtn);
        actionButtons.setWidthFull();
        actionButtons.setPadding(true);
        actionButtons.getStyle().set("border-top", "1px solid #e0e0e0");

        panel.add(panelHeader, searchField, treeGrid, actionButtons);
        panel.expand(treeGrid);

        return panel;
    }

    private AbstractBackEndHierarchicalDataProvider<KnowledgeEntry, Void> buildTreeDataProvider() {
        return new AbstractBackEndHierarchicalDataProvider<>() {
            @Override
            protected Stream<KnowledgeEntry> fetchChildrenFromBackEnd(HierarchicalQuery<KnowledgeEntry, Void> query) {
                KnowledgeEntry parent = query.getParent();
                if (parent == null) {
                    return service.searchRootEntries(currentSearchQuery).stream();
                }
                return service.getChildEntries(parent.getId()).stream();
            }

            @Override
            public int getChildCount(HierarchicalQuery<KnowledgeEntry, Void> query) {
                KnowledgeEntry parent = query.getParent();
                if (parent == null) {
                    return service.searchRootEntries(currentSearchQuery).size();
                }
                return service.getChildEntries(parent.getId()).size();
            }

            @Override
            public boolean hasChildren(KnowledgeEntry item) {
                return !service.getChildEntries(item.getId()).isEmpty();
            }
        };
    }

    // ─── Right panel (content + empty state) ─────────────────────────────────

    private VerticalLayout buildRightPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);
        panel.setSpacing(false);

        emptyStatePanel = buildEmptyState();
        contentPanel = buildContentPanel();
        contentPanel.setVisible(false);

        panel.add(emptyStatePanel, contentPanel);
        panel.expand(contentPanel);

        return panel;
    }

    private Div buildEmptyState() {
        Div div = new Div();
        div.setSizeFull();
        div.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "#9e9e9e")
                .set("gap", "16px");

        Icon icon = VaadinIcon.BOOK.create();
        icon.setSize("64px");
        icon.getStyle().set("color", "#bdbdbd");

        H3 heading = new H3("Select a topic");
        heading.getStyle().set("color", "#757575").set("margin", "0");

        Paragraph hint = new Paragraph("Choose a topic from the tree on the left, or create a new one.");
        hint.getStyle().set("margin", "0").set("text-align", "center").set("max-width", "300px");

        div.add(icon, heading, hint);
        return div;
    }

    private VerticalLayout buildContentPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);
        panel.setSpacing(false);

        // Breadcrumb bar
        breadcrumbBar = new HorizontalLayout();
        breadcrumbBar.setWidthFull();
        breadcrumbBar.setPadding(true);
        breadcrumbBar.setSpacing(false);
        breadcrumbBar.setAlignItems(FlexComponent.Alignment.CENTER);
        breadcrumbBar.getStyle()
                .set("border-bottom", "1px solid #e0e0e0")
                .set("background-color", "#f5f5f5")
                .set("font-size", "0.85rem")
                .set("color", "#616161")
                .set("flex-wrap", "wrap");

        // Content area
        VerticalLayout editArea = new VerticalLayout();
        editArea.setSizeFull();
        editArea.setPadding(true);
        editArea.setSpacing(true);

        // Toolbar: save + delete
        Button saveBtn = new Button("Save", VaadinIcon.CHECK.create(), e -> saveCurrentEntry());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button deleteBtn = new Button("Delete", VaadinIcon.TRASH.create(), e -> confirmDeleteEntry());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        // View count badge
        viewCountBadge = new Span("Views: 0");
        viewCountBadge.getStyle()
                .set("background-color", "#E3F2FD")
                .set("color", "#1565C0")
                .set("border-radius", "12px")
                .set("padding", "2px 10px")
                .set("font-size", "0.8rem")
                .set("font-weight", "500");

        HorizontalLayout toolbar = new HorizontalLayout(saveBtn, deleteBtn, viewCountBadge);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Title field
        titleField = new TextField("Title");
        titleField.setWidthFull();
        titleField.setValueChangeMode(ValueChangeMode.LAZY);
        titleField.setValueChangeTimeout(1000);
        titleField.addValueChangeListener(e -> autoSave());

        // Content textarea
        contentArea = new TextArea("Content");
        contentArea.setWidthFull();
        contentArea.setMinHeight("350px");
        contentArea.setValueChangeMode(ValueChangeMode.LAZY);
        contentArea.setValueChangeTimeout(1000);
        contentArea.addValueChangeListener(e -> autoSave());

        // Metadata row (timestamps)
        createdAtSpan = new Span();
        updatedAtSpan = new Span();
        styleMetaSpan(createdAtSpan);
        styleMetaSpan(updatedAtSpan);

        HorizontalLayout metaRow = new HorizontalLayout(createdAtSpan, updatedAtSpan);
        metaRow.setSpacing(true);
        metaRow.getStyle().set("font-size", "0.8rem").set("color", "#9e9e9e");

        editArea.add(toolbar, titleField, contentArea, metaRow);
        editArea.expand(contentArea);

        // Keyboard shortcut: Ctrl+S to save
        Shortcuts.addShortcutListener(editArea, this::saveCurrentEntry, Key.KEY_S)
                .withModifiers(com.vaadin.flow.component.KeyModifier.CONTROL);

        panel.add(breadcrumbBar, editArea);
        panel.expand(editArea);

        return panel;
    }

    private void styleMetaSpan(Span span) {
        span.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#9e9e9e");
    }

    // ─── Entry operations ─────────────────────────────────────────────────────

    private void loadEntry(KnowledgeEntry entry) {
        currentEntry = entry;

        // Show content panel, hide empty state
        emptyStatePanel.setVisible(false);
        contentPanel.setVisible(true);

        // Populate fields
        titleField.setValue(entry.getTitle() != null ? entry.getTitle() : "");
        contentArea.setValue(entry.getContent() != null ? entry.getContent() : "");
        viewCountBadge.setText("Views: " + entry.getViewCount());

        // Timestamps
        if (entry.getCreatedAt() != null) {
            createdAtSpan.setText("Created: " + entry.getCreatedAt().format(DATE_FORMATTER));
        }
        if (entry.getUpdatedAt() != null) {
            updatedAtSpan.setText("Last updated: " + entry.getUpdatedAt().format(DATE_FORMATTER));
        }

        // Build breadcrumb
        buildBreadcrumb(entry);

        // Increment view count
        service.incrementViewCount(entry.getId());
        treeGrid.getDataProvider().refreshItem(entry);
    }

    private void buildBreadcrumb(KnowledgeEntry entry) {
        breadcrumbBar.removeAll();

        // Walk up the tree to collect ancestors
        java.util.Deque<KnowledgeEntry> path = new java.util.ArrayDeque<>();
        KnowledgeEntry current = entry;
        while (current != null) {
            path.addFirst(current);
            current = current.getParent();
        }

        boolean first = true;
        for (KnowledgeEntry node : path) {
            if (!first) {
                Span sep = new Span(" › ");
                sep.getStyle().set("color", "#bdbdbd").set("margin", "0 4px");
                breadcrumbBar.add(sep);
            }
            if (node.getId().equals(entry.getId())) {
                Span crumb = new Span(node.getTitle());
                crumb.getStyle().set("font-weight", "600").set("color", "#424242");
                breadcrumbBar.add(crumb);
            } else {
                Button crumb = new Button(node.getTitle());
                crumb.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                crumb.getStyle().set("padding", "0").set("min-width", "0");
                KnowledgeEntry nodeRef = node;
                crumb.addClickListener(e -> {
                    treeGrid.select(nodeRef);
                    loadEntry(nodeRef);
                });
                breadcrumbBar.add(crumb);
            }
            first = false;
        }
    }

    private void createNewEntry(KnowledgeEntry parent) {
        KnowledgeEntry newEntry = new KnowledgeEntry();
        newEntry.setTitle("New Topic");
        newEntry.setContent("");
        if (parent != null) {
            newEntry.setParent(parent);
        }
        newEntry = service.createEntry(newEntry);
        treeGrid.getDataProvider().refreshAll();
        treeGrid.select(newEntry);
        loadEntry(newEntry);

        showNotification("Topic created successfully", NotificationVariant.LUMO_SUCCESS);
    }

    private void autoSave() {
        if (currentEntry != null) {
            currentEntry.setTitle(titleField.getValue());
            currentEntry.setContent(contentArea.getValue());
            service.updateEntry(currentEntry);
            treeGrid.getDataProvider().refreshItem(currentEntry);

            // Update breadcrumb title if it changed
            buildBreadcrumb(currentEntry);

            // Update timestamp display
            service.getEntry(currentEntry.getId()).ifPresent(refreshed -> {
                currentEntry = refreshed;
                if (refreshed.getUpdatedAt() != null) {
                    updatedAtSpan.setText("Last updated: " + refreshed.getUpdatedAt().format(DATE_FORMATTER));
                }
            });
        }
    }

    private void saveCurrentEntry() {
        if (currentEntry != null) {
            currentEntry.setTitle(titleField.getValue());
            currentEntry.setContent(contentArea.getValue());
            service.updateEntry(currentEntry);
            treeGrid.getDataProvider().refreshItem(currentEntry);
            buildBreadcrumb(currentEntry);

            service.getEntry(currentEntry.getId()).ifPresent(refreshed -> {
                currentEntry = refreshed;
                if (refreshed.getUpdatedAt() != null) {
                    updatedAtSpan.setText("Last updated: " + refreshed.getUpdatedAt().format(DATE_FORMATTER));
                }
            });

            showNotification("Saved successfully", NotificationVariant.LUMO_SUCCESS);
        }
    }

    private void confirmDeleteEntry() {
        if (currentEntry == null) return;

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Topic");
        dialog.setText("Are you sure you want to delete \"" + currentEntry.getTitle()
                + "\"? This will also delete all child topics.");
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelText("Cancel");
        dialog.setCancelable(true);

        dialog.addConfirmListener(e -> {
            Long deletedId = currentEntry.getId();
            service.deleteEntry(deletedId);
            currentEntry = null;
            treeGrid.getDataProvider().refreshAll();
            treeGrid.deselectAll();

            // Show empty state
            contentPanel.setVisible(false);
            emptyStatePanel.setVisible(true);

            showNotification("Topic deleted", NotificationVariant.LUMO_CONTRAST);
        });

        dialog.open();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
