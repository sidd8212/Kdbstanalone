package com.knowledgebase.ui;

import com.knowledgebase.model.KnowledgeEntry;
import com.knowledgebase.service.KnowledgeEntryService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Route("")
@PageTitle("Knowledge Base")
public class MainView extends VerticalLayout implements HasUrlParameter<Long> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final KnowledgeEntryService service;

    // Left panel components
    private TreeGrid<KnowledgeEntry> treeGrid;
    private TextField searchField;
    private VerticalLayout searchResultsPanel;
    private Span searchResultCountLabel;
    private List<KnowledgeEntry> currentSearchResults = new ArrayList<>();
    private int searchResultFocusIndex = -1;

    // Right panel components
    private TextField titleField;
    private TextArea contentArea;
    private Span viewCountBadge;
    private Span createdAtSpan;
    private Span updatedAtSpan;
    private Span saveStatusSpan;
    private HorizontalLayout breadcrumbBar;
    private VerticalLayout contentPanel;
    private Div emptyStatePanel;

    // State
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

    // ─── URL parameter: navigate directly to a topic by ID ───────────────────

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long topicId) {
        if (topicId != null) {
            service.getEntry(topicId).ifPresent(entry -> {
                treeGrid.select(entry);
                loadEntry(entry);
            });
        }
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

    // ─── Left panel ───────────────────────────────────────────────────────────

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
        searchField.addValueChangeListener(e -> onSearchChanged(e.getValue()));

        // Keyboard navigation in search results
        searchField.addKeyDownListener(Key.ARROW_DOWN, e -> moveFocus(1));
        searchField.addKeyDownListener(Key.ARROW_UP, e -> moveFocus(-1));
        searchField.addKeyDownListener(Key.ENTER, e -> selectFocusedResult());
        searchField.addKeyDownListener(Key.ESCAPE, e -> {
            searchField.clear();
            onSearchChanged("");
        });

        // Tree grid
        treeGrid = new TreeGrid<>();
        treeGrid.setSizeFull();
        treeGrid.addHierarchyColumn(KnowledgeEntry::getTitle)
                .setHeader("Title")
                .setFlexGrow(1);
        treeGrid.addColumn(entry -> service.getChildEntries(entry.getId()).size() + " child(ren)")
                .setHeader("")
                .setWidth("90px")
                .setFlexGrow(0);
        treeGrid.setDataProvider(buildTreeDataProvider());
        treeGrid.addSelectionListener(event -> event.getFirstSelectedItem().ifPresent(this::loadEntry));

        // Search results panel (hidden when search is empty)
        searchResultCountLabel = new Span();
        searchResultCountLabel.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#757575")
                .set("padding", "4px 8px");

        searchResultsPanel = new VerticalLayout();
        searchResultsPanel.setSizeFull();
        searchResultsPanel.setPadding(false);
        searchResultsPanel.setSpacing(false);
        searchResultsPanel.setVisible(false);
        searchResultsPanel.getStyle().set("overflow-y", "auto");

        // Action buttons
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

        panel.add(panelHeader, searchField, searchResultCountLabel, searchResultsPanel, treeGrid, actionButtons);
        panel.expand(treeGrid);

        return panel;
    }

    private void onSearchChanged(String query) {
        currentSearchQuery = query;
        if (query == null || query.isBlank()) {
            // Show tree, hide search results
            searchResultCountLabel.setVisible(false);
            searchResultsPanel.setVisible(false);
            treeGrid.setVisible(true);
            treeGrid.getDataProvider().refreshAll();
            currentSearchResults.clear();
            searchResultFocusIndex = -1;
        } else {
            // Show search results, hide tree
            treeGrid.setVisible(false);
            searchResultCountLabel.setVisible(true);
            searchResultsPanel.setVisible(true);
            populateSearchResults(query.trim());
        }
    }

    private void populateSearchResults(String query) {
        try {
            currentSearchResults = service.searchEntries(query);
            searchResultFocusIndex = -1;
            searchResultsPanel.removeAll();

            if (currentSearchResults.isEmpty()) {
                searchResultCountLabel.setText("No results for "" + query + """);
                Div empty = new Div();
                empty.getStyle()
                        .set("padding", "16px")
                        .set("text-align", "center")
                        .set("color", "#9e9e9e")
                        .set("font-size", "0.85rem");
                empty.setText("No matching topics found.");
                searchResultsPanel.add(empty);
            } else {
                searchResultCountLabel.setText(currentSearchResults.size() + " result(s) for "" + query + """);
                for (int i = 0; i < currentSearchResults.size(); i++) {
                    searchResultsPanel.add(buildResultItem(currentSearchResults.get(i), query, i));
                }
            }
        } catch (Exception ex) {
            showNotification("Search failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private Div buildResultItem(KnowledgeEntry entry, String query, int index) {
        Div item = new Div();
        item.setId("search-result-" + index);
        item.getStyle()
                .set("padding", "10px 12px")
                .set("border-bottom", "1px solid #f0f0f0")
                .set("cursor", "pointer")
                .set("transition", "background-color 0.15s");

        // Title with highlighted match
        Div titleDiv = new Div();
        titleDiv.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.88rem")
                .set("color", "#212121")
                .set("margin-bottom", "4px");
        titleDiv.getElement().setProperty("innerHTML", highlightMatch(entry.getTitle(), query));

        // Path (breadcrumb)
        String path = buildPathString(entry);
        Div pathDiv = new Div();
        pathDiv.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "#9e9e9e")
                .set("margin-bottom", "4px");
        pathDiv.setText(path);

        // Content snippet with highlight
        String snippet = buildContentSnippet(entry.getContent(), query);
        Div snippetDiv = new Div();
        snippetDiv.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#616161");
        snippetDiv.getElement().setProperty("innerHTML", highlightMatch(snippet, query));

        // Created date
        Div dateDiv = new Div();
        dateDiv.getStyle()
                .set("font-size", "0.72rem")
                .set("color", "#bdbdbd")
                .set("margin-top", "4px");
        if (entry.getCreatedAt() != null) {
            dateDiv.setText("Created: " + entry.getCreatedAt().format(DATE_FORMATTER));
        }

        item.add(titleDiv, pathDiv, snippetDiv, dateDiv);

        // Click to load
        item.addClickListener(e -> {
            searchField.clear();
            onSearchChanged("");
            treeGrid.select(entry);
            loadEntry(entry);
        });

        // Hover styling via DOM events
        item.getElement().addEventListener("mouseover",
                e -> item.getStyle().set("background-color", "#E3F2FD"));
        item.getElement().addEventListener("mouseout",
                e -> item.getStyle().remove("background-color"));

        return item;
    }

    /** Wraps matched text in a yellow <mark> */
    private String highlightMatch(String text, String query) {
        if (text == null || text.isBlank() || query == null || query.isBlank()) {
            return text == null ? "" : escapeHtml(text);
        }
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int idx;
        while ((idx = lowerText.indexOf(lowerQuery, start)) >= 0) {
            sb.append(escapeHtml(text.substring(start, idx)));
            sb.append("<mark style=\"background-color:#FFF176;color:#212121;border-radius:2px;\">")
              .append(escapeHtml(text.substring(idx, idx + query.length())))
              .append("</mark>");
            start = idx + query.length();
        }
        sb.append(escapeHtml(text.substring(start)));
        return sb.toString();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String buildContentSnippet(String content, String query) {
        if (content == null || content.isBlank()) return "";
        String lower = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int idx = lower.indexOf(lowerQuery);
        if (idx < 0) {
            return content.length() > 120 ? content.substring(0, 120) + "…" : content;
        }
        int start = Math.max(0, idx - 40);
        int end = Math.min(content.length(), idx + query.length() + 80);
        String snippet = (start > 0 ? "…" : "") + content.substring(start, end) + (end < content.length() ? "…" : "");
        return snippet;
    }

    private String buildPathString(KnowledgeEntry entry) {
        java.util.Deque<String> parts = new java.util.ArrayDeque<>();
        KnowledgeEntry cur = entry;
        while (cur != null) {
            parts.addFirst(cur.getTitle());
            cur = cur.getParent();
        }
        return String.join(" › ", parts);
    }

    // ─── Keyboard navigation in search results ───────────────────────────────

    private void moveFocus(int delta) {
        if (currentSearchResults.isEmpty()) return;
        int newIndex = searchResultFocusIndex + delta;
        newIndex = Math.max(0, Math.min(newIndex, currentSearchResults.size() - 1));
        setResultFocus(newIndex);
    }

    private void setResultFocus(int index) {
        // Clear previous focus style
        if (searchResultFocusIndex >= 0 && searchResultFocusIndex < searchResultsPanel.getComponentCount()) {
            searchResultsPanel.getComponentAt(searchResultFocusIndex)
                    .getElement().getStyle().remove("background-color");
        }
        searchResultFocusIndex = index;
        if (index >= 0 && index < searchResultsPanel.getComponentCount()) {
            searchResultsPanel.getComponentAt(index)
                    .getElement().getStyle().set("background-color", "#BBDEFB");
            // Scroll into view
            searchResultsPanel.getComponentAt(index)
                    .getElement().callJsFunction("scrollIntoView",
                            searchResultsPanel.getElement().executeJs("return {block:'nearest'}"));
        }
    }

    private void selectFocusedResult() {
        if (searchResultFocusIndex >= 0 && searchResultFocusIndex < currentSearchResults.size()) {
            KnowledgeEntry entry = currentSearchResults.get(searchResultFocusIndex);
            searchField.clear();
            onSearchChanged("");
            treeGrid.select(entry);
            loadEntry(entry);
        }
    }

    private AbstractBackEndHierarchicalDataProvider<KnowledgeEntry, Void> buildTreeDataProvider() {
        return new AbstractBackEndHierarchicalDataProvider<>() {
            @Override
            protected Stream<KnowledgeEntry> fetchChildrenFromBackEnd(
                    HierarchicalQuery<KnowledgeEntry, Void> query) {
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

    // ─── Right panel ─────────────────────────────────────────────────────────

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
                .set("flex-wrap", "wrap");

        // Edit area
        VerticalLayout editArea = new VerticalLayout();
        editArea.setSizeFull();
        editArea.setPadding(true);
        editArea.setSpacing(true);

        // Toolbar
        Button saveBtn = new Button("Save", VaadinIcon.CHECK.create(), e -> saveCurrentEntry());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button deleteBtn = new Button("Delete", VaadinIcon.TRASH.create(), e -> confirmDeleteEntry());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        Button importBtn = new Button("Import", VaadinIcon.PASTE.create(), e -> openImportDialog());
        importBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

        // Save status indicator
        saveStatusSpan = new Span();
        saveStatusSpan.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#757575");

        // View count badge
        viewCountBadge = new Span("Views: 0");
        viewCountBadge.getStyle()
                .set("background-color", "#E3F2FD")
                .set("color", "#1565C0")
                .set("border-radius", "12px")
                .set("padding", "2px 10px")
                .set("font-size", "0.8rem")
                .set("font-weight", "500");

        HorizontalLayout leftTools = new HorizontalLayout(saveBtn, deleteBtn, importBtn, saveStatusSpan);
        leftTools.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout toolbar = new HorizontalLayout(leftTools, viewCountBadge);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Title field
        titleField = new TextField("Title");
        titleField.setWidthFull();
        titleField.setValueChangeMode(ValueChangeMode.LAZY);
        titleField.setValueChangeTimeout(1500);
        titleField.addValueChangeListener(e -> autoSave());

        // Content textarea
        contentArea = new TextArea("Content");
        contentArea.setWidthFull();
        contentArea.setMinHeight("350px");
        contentArea.setValueChangeMode(ValueChangeMode.LAZY);
        contentArea.setValueChangeTimeout(1500);
        contentArea.addValueChangeListener(e -> autoSave());

        // Metadata row
        createdAtSpan = new Span();
        updatedAtSpan = new Span();
        styleMetaSpan(createdAtSpan);
        styleMetaSpan(updatedAtSpan);

        HorizontalLayout metaRow = new HorizontalLayout(createdAtSpan, updatedAtSpan);
        metaRow.setSpacing(true);

        editArea.add(toolbar, titleField, contentArea, metaRow);
        editArea.expand(contentArea);

        // Ctrl+S shortcut
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

    // ─── Import dialog ────────────────────────────────────────────────────────

    private void openImportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import Content");
        dialog.setWidth("600px");

        Paragraph instructions = new Paragraph(
                "Paste content below. A new topic will be created, or it will be appended to the current topic.");
        instructions.getStyle().set("margin-top", "0");

        TextField importTitle = new TextField("Topic Title");
        importTitle.setWidthFull();
        importTitle.setPlaceholder("Enter a title for the imported content");
        if (currentEntry != null) {
            importTitle.setValue("Import into: " + currentEntry.getTitle());
        }

        TextArea importContent = new TextArea("Content to Import");
        importContent.setWidthFull();
        importContent.setMinHeight("200px");
        importContent.setPlaceholder("Paste your content here...");

        // Options
        Button createNewBtn = new Button("Create New Topic", e -> {
            String title = importTitle.getValue().isBlank() ? "Imported Topic" : importTitle.getValue();
            String content = importContent.getValue();
            if (content.isBlank()) {
                showNotification("Content cannot be empty", NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                KnowledgeEntry newEntry = new KnowledgeEntry();
                newEntry.setTitle(title);
                newEntry.setContent(content);
                KnowledgeEntry saved = service.createEntry(newEntry);
                treeGrid.getDataProvider().refreshAll();
                treeGrid.select(saved);
                loadEntry(saved);
                dialog.close();
                showNotification("Topic imported successfully", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                showNotification("Import failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        createNewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button appendBtn = new Button("Append to Current", e -> {
            if (currentEntry == null) {
                showNotification("No topic selected to append to", NotificationVariant.LUMO_WARNING);
                return;
            }
            String content = importContent.getValue();
            if (content.isBlank()) {
                showNotification("Content cannot be empty", NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                String existing = currentEntry.getContent() != null ? currentEntry.getContent() : "";
                currentEntry.setContent(existing + (existing.isBlank() ? "" : "\n\n") + content);
                service.updateEntry(currentEntry);
                contentArea.setValue(currentEntry.getContent());
                dialog.close();
                showNotification("Content appended successfully", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                showNotification("Append failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout content = new VerticalLayout(instructions, importTitle, importContent);
        content.setPadding(false);
        dialog.add(content);
        dialog.getFooter().add(cancelBtn, appendBtn, createNewBtn);
        dialog.open();
    }

    // ─── Entry operations ─────────────────────────────────────────────────────

    private void loadEntry(KnowledgeEntry entry) {
        try {
            currentEntry = entry;

            emptyStatePanel.setVisible(false);
            contentPanel.setVisible(true);

            titleField.setValue(entry.getTitle() != null ? entry.getTitle() : "");
            contentArea.setValue(entry.getContent() != null ? entry.getContent() : "");
            viewCountBadge.setText("Views: " + entry.getViewCount());
            saveStatusSpan.setText("");

            if (entry.getCreatedAt() != null) {
                createdAtSpan.setText("Created: " + entry.getCreatedAt().format(DATE_FORMATTER));
            }
            if (entry.getUpdatedAt() != null) {
                updatedAtSpan.setText("Last updated: " + entry.getUpdatedAt().format(DATE_FORMATTER));
            }

            buildBreadcrumb(entry);

            service.incrementViewCount(entry.getId());
            treeGrid.getDataProvider().refreshItem(entry);

            // Update URL to reflect current topic
            getUI().ifPresent(ui -> ui.navigate(MainView.class, entry.getId()));
        } catch (Exception ex) {
            showNotification("Failed to load topic: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void buildBreadcrumb(KnowledgeEntry entry) {
        breadcrumbBar.removeAll();

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
        try {
            KnowledgeEntry newEntry = new KnowledgeEntry();
            newEntry.setTitle("New Topic");
            newEntry.setContent("");
            if (parent != null) {
                newEntry.setParent(parent);
            }
            KnowledgeEntry saved = service.createEntry(newEntry);
            treeGrid.getDataProvider().refreshAll();
            treeGrid.select(saved);
            loadEntry(saved);
            showNotification("Topic created successfully", NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            showNotification("Failed to create topic: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void autoSave() {
        if (currentEntry != null) {
            try {
                saveStatusSpan.setText("Saving…");
                saveStatusSpan.getStyle().set("color", "#FF8F00");

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

                saveStatusSpan.setText("Saved ✓");
                saveStatusSpan.getStyle().set("color", "#388E3C");
            } catch (Exception ex) {
                saveStatusSpan.setText("Save failed");
                saveStatusSpan.getStyle().set("color", "#D32F2F");
                showNotification("Auto-save failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private void saveCurrentEntry() {
        if (currentEntry != null) {
            try {
                saveStatusSpan.setText("Saving…");
                saveStatusSpan.getStyle().set("color", "#FF8F00");

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

                saveStatusSpan.setText("Saved ✓");
                saveStatusSpan.getStyle().set("color", "#388E3C");
                showNotification("Saved successfully", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                saveStatusSpan.setText("Save failed");
                saveStatusSpan.getStyle().set("color", "#D32F2F");
                showNotification("Save failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
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
            try {
                service.deleteEntry(currentEntry.getId());
                currentEntry = null;
                treeGrid.getDataProvider().refreshAll();
                treeGrid.deselectAll();
                contentPanel.setVisible(false);
                emptyStatePanel.setVisible(true);
                getUI().ifPresent(ui -> ui.navigate(MainView.class));
                showNotification("Topic deleted", NotificationVariant.LUMO_CONTRAST);
            } catch (Exception ex) {
                showNotification("Delete failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
