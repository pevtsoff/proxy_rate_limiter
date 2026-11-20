package com.example.ratelimiter.adapter.in.ui.component;

import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.PageResult;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import java.util.function.Function;

/**
 * Grid with explicit server-side pagination that maps 1:1 onto the paged use cases (the same
 * paging the REST API exposes).
 */
public class PagedGrid<T> extends VerticalLayout {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private int page;
    private final Grid<T> grid = new Grid<>();
    private final Function<PageQuery, PageResult<T>> loader;
    private final Span pageInfo = new Span();
    private final Button previous = new Button("Previous", event -> goTo(page - 1));
    private final Button next = new Button("Next", event -> goTo(page + 1));
    private final Select<Integer> pageSize = new Select<>();

    public PagedGrid(Function<PageQuery, PageResult<T>> loader) {
        this.loader = loader;
        setPadding(false);
        setSizeFull();
        grid.setSizeFull();
        configurePageSize();
        HorizontalLayout pager = new HorizontalLayout(previous, pageInfo, next, pageSize);
        pager.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(grid, pager);
    }

    public Grid<T> grid() {
        return grid;
    }

    /** Reloads the current page, e.g. after a create / update / delete. */
    public void refresh() {
        PageResult<T> result = loader.apply(new PageQuery(page, pageSize.getValue()));
        if (result.content().isEmpty() && page > 0) {
            goTo(Math.max(0, result.totalPages() - 1));
            return;
        }
        grid.setItems(result.content());
        pageInfo.setText(String.format("Page %d of %d (%d items)",
                page + 1, Math.max(1, result.totalPages()), result.totalElements()));
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < result.totalPages());
    }

    /** Jumps back to the first page, e.g. after the filter changed. */
    public void firstPage() {
        goTo(0);
    }

    private void goTo(int newPage) {
        page = newPage;
        refresh();
    }

    private void configurePageSize() {
        pageSize.setItems(10, 20, 50, 100);
        pageSize.setValue(DEFAULT_PAGE_SIZE);
        pageSize.setWidth("6em");
        pageSize.addValueChangeListener(event -> firstPage());
    }
}
