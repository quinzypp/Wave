package dev.adapt.v3.event.impl;

public final class StepEvent {
    public final StepEvent.Stage stage;
    public double stepHeight;
    public final double steppedHeight;

    public StepEvent(StepEvent.Stage stage, double first, double second) {
        this.stage = stage;
        this.stepHeight = first;
        this.steppedHeight = second;
    }

    public StepEvent.Stage getStage() {
        return this.stage;
    }

    public double getStepHeight() {
        return this.stepHeight;
    }

    public void setStepHeight(double stepHeight) {
        this.stepHeight = stepHeight;
    }

    public double getSteppedHeight() {
        return this.steppedHeight;
    }

    public enum Stage {
        PRE,
        POST;
    }
}
