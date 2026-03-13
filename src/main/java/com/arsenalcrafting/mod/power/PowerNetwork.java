package com.arsenalcrafting.mod.power;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents one connected component of the power cable network.
 * Contains all cable positions plus all endpoint positions (generators, banks).
 *
 * Networks are rebuilt from scratch on cable place/break — they are never
 * mutated in place. This keeps the logic simple and bug-free.
 */
public class PowerNetwork {

    private final int id;
    private final Set<BlockPos> cables    = new HashSet<>();
    private final Set<BlockPos> endpoints = new HashSet<>(); // generators + banks

    public PowerNetwork(int id) {
        this.id = id;
    }

    public int getId()                    { return id; }
    public Set<BlockPos> getCables()      { return cables; }
    public Set<BlockPos> getEndpoints()   { return endpoints; }

    public void addCable(BlockPos pos)    { cables.add(pos); }
    public void addEndpoint(BlockPos pos) { endpoints.add(pos); }

    public boolean isEmpty() {
        return cables.isEmpty() && endpoints.isEmpty();
    }

    @Override
    public String toString() {
        return "PowerNetwork[id=" + id + ", cables=" + cables.size()
                + ", endpoints=" + endpoints.size() + "]";
    }
}