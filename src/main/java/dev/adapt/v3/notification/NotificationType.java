package dev.adapt.v3.notification;

public enum NotificationType {
    SUCCESS("\ue5ca", -14498421),
    ERROR("\ue000", -1027478),
    WARN("\ue002", -678620),
    INFO("\ue88f", -11760129),
    DISABLED("\ue5cd", -1027478);

    public final String icon;
    public final int color;

    NotificationType(String text, int value) {
        this.icon = text;
        this.color = value;
    }

    public String getIcon() {
        return this.icon;
    }

    public int getColor() {
        return this.color;
    }
}
