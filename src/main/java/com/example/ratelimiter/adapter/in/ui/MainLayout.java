package com.example.ratelimiter.adapter.in.ui;

import com.example.ratelimiter.adapter.in.ui.view.ConsumerGroupsView;
import com.example.ratelimiter.adapter.in.ui.view.ConsumersView;
import com.example.ratelimiter.adapter.in.ui.view.PoliciesView;
import com.example.ratelimiter.adapter.in.ui.view.PolicyAssignmentsView;
import com.example.ratelimiter.adapter.in.ui.view.ResourceGroupsView;
import com.example.ratelimiter.adapter.in.ui.view.ResourcesView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * MUC6: shell of the admin UI. Every view is a thin inbound adapter that calls the same use
 * cases as the REST API, so validation and business rules are identical in both.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        H1 title = new H1("Proxy Rate Limiter - Admin");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");
        Button logout = new Button("Log out", event -> getUI()
                .ifPresent(ui -> ui.getPage().setLocation("/logout")));
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout navbar = new HorizontalLayout(new DrawerToggle(), title, logout);
        navbar.setWidthFull();
        navbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        navbar.expand(title);
        addToNavbar(navbar);
        addToDrawer(navigation());
    }

    private static SideNav navigation() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Policies", PoliciesView.class));
        nav.addItem(new SideNavItem("Consumer groups", ConsumerGroupsView.class));
        nav.addItem(new SideNavItem("Consumers", ConsumersView.class));
        nav.addItem(new SideNavItem("Resource groups", ResourceGroupsView.class));
        nav.addItem(new SideNavItem("Resources", ResourcesView.class));
        nav.addItem(new SideNavItem("Policy assignments", PolicyAssignmentsView.class));
        return nav;
    }
}
