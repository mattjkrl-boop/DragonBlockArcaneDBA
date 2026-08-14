package com.dragonblockarcanedba.entity;

public interface ITrackedSwarmEntity {
    /**
     * Gets the unique index of this entity within the swarm (0 to maxCount - 1).
     */
    int getSwarmIndex();

    /**
     * Gets the current health of this entity.
     */
    float getSwarmHealth();

    /**
     * Sets the health of this entity. Used when restoring the entity from the saved NBT array.
     */
    void setSwarmHealth(float health);
}
