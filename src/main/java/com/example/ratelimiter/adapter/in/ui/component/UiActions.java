package com.example.ratelimiter.adapter.in.ui.component;

import com.example.ratelimiter.domain.exception.DomainException;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

/**
 * Runs a UI action that calls a use case and reports the outcome as a notification. Business
 * errors are shown to the admin exactly as the REST API reports them.
 */
public final class UiActions {

    private static final Logger LOG = LoggerFactory.getLogger(UiActions.class);
    private static final int DURATION_MILLIS = 4000;

    private UiActions() {
    }

    /** @return {@code true} when the action completed without error */
    public static boolean run(Runnable action, String successMessage) {
        try {
            action.run();
            show(successMessage, NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (DomainException exception) {
            LOG.warn("Admin UI action rejected: {}", exception.getMessage(), exception);
            show(exception.getMessage(), NotificationVariant.LUMO_ERROR);
        } catch (DataAccessException exception) {
            LOG.error("Admin UI action failed in the database", exception);
            show("Data integrity violation: " + exception.getMostSpecificCause().getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
        return false;
    }

    private static void show(String message, NotificationVariant variant) {
        Notification notification =
                Notification.show(message, DURATION_MILLIS, Notification.Position.TOP_END);
        notification.addThemeVariants(variant);
    }
}
