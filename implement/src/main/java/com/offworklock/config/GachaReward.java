package com.offworklock.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representation of a gacha reward entry loaded from configuration.
 */
public final class GachaReward {
    private final String id;
    private final String name;
    private final String description;
    private final double weight;
    private final List<String> effects;

    public GachaReward(String id, String name, String description, double weight, List<String> effects) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.weight = weight;
        this.effects = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(effects, "effects")));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getWeight() {
        return weight;
    }

    public List<String> getEffects() {
        return effects;
    }

    @Override
    public String toString() {
        return "GachaReward{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", weight=" + weight +
                ", effects=" + effects +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GachaReward that = (GachaReward) o;
        return Double.compare(that.weight, weight) == 0
                && id.equals(that.id)
                && name.equals(that.name)
                && description.equals(that.description)
                && effects.equals(that.effects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, weight, effects);
    }
}
