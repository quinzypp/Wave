package dev.adapt.v3.feature.helper.projectile;

public final class Box {
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    public Box set(double value, double value2, double value3, double value4, double value5, double value6) {
        this.minX = value;
        this.minY = value2;
        this.minZ = value3;
        this.maxX = value4;
        this.maxY = value5;
        this.maxZ = value6;
        return this;
    }

    public Box inflate(double value) {
        return this.set(this.minX - value, this.minY - value, this.minZ - value, this.maxX + value, this.maxY + value, this.maxZ + value);
    }

    public double intersectRay(double value, double value2, double value3, double value4, double value5, double value6) {
        double[] adouble = new double[]{0.0, Double.MAX_VALUE};
        return clipAxis(value, value4, this.minX, this.maxX, adouble)
                && clipAxis(value2, value5, this.minY, this.maxY, adouble)
                && clipAxis(value3, value6, this.minZ, this.maxZ, adouble)
            ? adouble[0]
            : -1.0;
    }

    public static boolean clipAxis(double value, double value2, double value3, double value4, double[] doubles) {
        if (!(Math.abs(value2) < 1.0E-9)) {
            double d0 = (value3 - value) / value2;
            double d1 = (value4 - value) / value2;
            doubles[0] = Math.max(doubles[0], Math.min(d0, d1));
            doubles[1] = Math.min(doubles[1], Math.max(d0, d1));
            return doubles[0] <= doubles[1];
        } else {
            return value >= value3 && value <= value4;
        }
    }

    public double distanceTo(double range, double range2, double range3) {
        double d0 = range - Math.max(this.minX, Math.min(this.maxX, range));
        double d1 = range2 - Math.max(this.minY, Math.min(this.maxY, range2));
        double d2 = range3 - Math.max(this.minZ, Math.min(this.maxZ, range3));
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }
}
