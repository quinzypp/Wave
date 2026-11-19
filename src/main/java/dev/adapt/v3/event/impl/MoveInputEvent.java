package dev.adapt.v3.event.impl;

public final class MoveInputEvent {
    public float forward;
    public float sideways;
    public boolean jump;

    public MoveInputEvent(float first, float second, boolean flag) {
        this.forward = first;
        this.sideways = second;
        this.jump = flag;
    }

    public float getForward() {
        return this.forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public void setForwardKey(boolean forwardKey) {
        boolean flag = this.forward < 0.0F;
        this.forward = forwardKey == flag ? 0.0F : (forwardKey ? 1.0F : -1.0F);
    }

    public float getSideways() {
        return this.sideways;
    }

    public void setSideways(float sideways) {
        this.sideways = sideways;
    }

    public boolean isJump() {
        return this.jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }
}
