package com.dragonblockarcanedba.util;

import net.minecraft.world.phys.Vec3;
import java.util.Deque;

/**
 * Interface for tracking entity position history and enabling time reversal.
 * Implemented by LivingEntityMixin to attach a rolling position buffer
 * to all living entities.
 */
public interface TimeTracker {
    /**
     * Records a position into the rolling history buffer (max 100 entries = 5 seconds).
     */
    void dba$pushPosition(Vec3 pos);

    /**
     * Returns the position history deque (most recent first).
     */
    Deque<Vec3> dba$getPositionHistory();

    /**
     * Begins reversing this entity's movement for the given number of ticks.
     * While reversing, positions are popped from the history and applied,
     * with noPhysics enabled to allow phasing through blocks.
     */
    void dba$startReversing(int ticks);

    /**
     * Returns true if the entity is currently being reversed.
     */
    boolean dba$isReversing();
}
