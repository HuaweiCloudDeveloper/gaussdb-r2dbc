package io.r2dbc.gaussdb.codec;

import io.r2dbc.gaussdb.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Value object that maps to the {@code polygon} datatype in GaussDB.
 * <p>
 * Uses {@code double} to represent the coordinates.
 *
 * @since 0.8.5
 */
public final class Polygon {

    private final List<Point> points;

    private Polygon(List<Point> points) {
        Assert.requireNonNull(points, "points must not be null");
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    /**
     * Create a new {@link Polygon} given {@link List list of points}.
     *
     * @param points the points
     * @return the new {@link Polygon} object
     * @throws IllegalArgumentException if {@code points} is {@code null}
     */
    public static Polygon of(List<Point> points) {
        return new Polygon(points);
    }

    /**
     * Create a new {@link Polygon} given {@code points}.
     *
     * @param points the points
     * @return the new {@link Polygon} object
     * @throws IllegalArgumentException if {@code points} is {@code null}
     */
    public static Polygon of(Point... points) {
        Assert.requireNonNull(points, "points must not be null");

        return new Polygon(Arrays.asList(points));
    }

    public List<Point> getPoints() {
        return this.points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Polygon polygon = (Polygon) o;
        return this.points.equals(polygon.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.points);
    }

    @Override
    public String toString() {
        return "(" + this.points.stream().map(Point::toString).collect(Collectors.joining(", ")) + ")";
    }

}
