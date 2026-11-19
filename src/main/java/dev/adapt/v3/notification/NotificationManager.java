package dev.adapt.v3.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NotificationManager {
    public final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public List<Notification> getNotifications() {
        return this.notifications;
    }

    public NotificationManager.Builder builder(NotificationType notificationType) {
        return new NotificationManager.Builder(this, notificationType);
    }

    public Notification post(Notification notification2) {
        for (Notification notification : this.notifications) {
            if (notification.matches(notification2)) {
                this.notifications.remove(notification);
                this.notifications.add(notification);
                notification.refresh(notification2.getDurationMs());
                return notification;
            }
        }

        this.notifications.add(notification2);

        while (this.notifications.size() > 6) {
            this.notifications.remove(0);
        }

        return notification2;
    }

    public void remove(Notification notification) {
        this.notifications.remove(notification);
    }

    public static class Builder {
        public final NotificationManager manager;
        public final NotificationType type;
        public String title;
        public String message;
        public int durationMs;

        public Builder(NotificationManager notificationManager, NotificationType notificationType) {
            this.manager = notificationManager;
            this.type = notificationType;
            this.title = "Notification";
            this.message = "";
            this.durationMs = 2000;
        }

        public NotificationManager.Builder title(String text) {
            this.title = text;
            return this;
        }

        public NotificationManager.Builder message(String message) {
            this.message = message;
            return this;
        }

        public NotificationManager.Builder duration(int value) {
            this.durationMs = value;
            return this;
        }

        public Notification post() {
            return this.manager.post(new Notification(this.type, this.title, this.message, this.durationMs));
        }
    }
}
