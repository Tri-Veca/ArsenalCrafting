package com.arsenalcrafting.mod.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.*;

/**
 * Persistent power network graph attached to a ServerLevel via SavedData.
 *
 * Responsibilities:
 * - Track all cable positions and which network they belong to
 * - Track all endpoint positions (generators, banks) adjacent to cables
 * - Rebuild networks via BFS when cables are placed or broken
 * - Distribute energy across each network once per tick
 *
 * One instance per dimension. Survives server restarts via NBT.
 */
public class PowerNetworkSavedData extends SavedData {

    private static final String DATA_NAME = "arsenal_power_networks";

    // pos → network ID for cables
    private final Map<BlockPos, Integer> cableToNetwork  = new HashMap<>();

    // network ID → network
    private final Map<Integer, PowerNetwork> networks = new HashMap<>();

    private int nextNetworkId = 1;

    // ── FACTORY ───────────────────────────────────────────────────────────────

    public static final SavedData.Factory<PowerNetworkSavedData> FACTORY =
            new SavedData.Factory<>(
                    PowerNetworkSavedData::new,
                    PowerNetworkSavedData::load,
                    null
            );

    public static PowerNetworkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    // ── NETWORK REBUILD ───────────────────────────────────────────────────────

    /**
     * Called when a cable is placed or broken at pos.
     * Clears all networks touching pos and rebuilds from scratch via BFS.
     */
    public void onCableChanged(ServerLevel level, BlockPos pos) {
        // Collect all cable positions that were in networks touching this pos
        Set<BlockPos> affectedCables = new HashSet<>();

        // Include existing neighbors of pos that are cables
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (cableToNetwork.containsKey(neighbor)) {
                int nid = cableToNetwork.get(neighbor);
                PowerNetwork net = networks.get(nid);
                if (net != null) affectedCables.addAll(net.getCables());
            }
        }
        affectedCables.add(pos); // include pos itself

        // Remove all affected networks
        Set<Integer> removedIds = new HashSet<>();
        for (BlockPos cp : affectedCables) {
            Integer nid = cableToNetwork.remove(cp);
            if (nid != null) removedIds.add(nid);
        }
        removedIds.forEach(networks::remove);

        // Rebuild — BFS from every unvisited cable in the affected set
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos seed : affectedCables) {
            if (visited.contains(seed)) continue;
            if (!isCable(level, seed)) continue;

            PowerNetwork network = new PowerNetwork(nextNetworkId++);
            bfsFromSeed(level, seed, visited, network);

            if (!network.isEmpty()) {
                networks.put(network.getId(), network);
                for (BlockPos cp : network.getCables()) {
                    cableToNetwork.put(cp, network.getId());
                }
                System.out.println("[PowerNetwork] Built network #" + network.getId()
                        + " cables=" + network.getCables().size()
                        + " endpoints=" + network.getEndpoints().size());
            }
        }

        setDirty();
    }

    /**
     * BFS from seed cable position. Adds all reachable cables and their
     * adjacent endpoints (generators, banks) to the network.
     */
    private void bfsFromSeed(ServerLevel level, BlockPos seed,
                             Set<BlockPos> visited, PowerNetwork network) {
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(seed);
        visited.add(seed);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            network.addCable(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;

                if (isCable(level, neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                } else if (isEnergyEndpoint(level, neighbor)) {
                    // Endpoints aren't traversed but are registered
                    network.addEndpoint(neighbor);
                }
            }
        }
    }

    // ── TICK / DISTRIBUTION ───────────────────────────────────────────────────

    /**
     * Called once per server tick from PowerNetworkEvents.
     * For each network: collect from producers, distribute to consumers.
     */
    public void tickNetworks(ServerLevel level) {
        if (networks.isEmpty()) return; // skip log spam when no networks
        // Debug every 100 ticks
        if (level.getGameTime() % 100 == 0) {
            System.out.println("[PowerNetwork] Networks: " + networks.size()
                    + " | Cables: " + cableToNetwork.size());
            for (PowerNetwork net : networks.values()) {
                System.out.println("  Network #" + net.getId()
                        + " cables=" + net.getCables().size()
                        + " endpoints=" + net.getEndpoints().size());
            }
        }
        for (PowerNetwork network : networks.values()) {
            tickNetwork(level, network);
        }
    }

    private void tickNetwork(ServerLevel level, PowerNetwork network) {
        if (network.getEndpoints().isEmpty()) return;

        // Phase 1: collect available energy — only from generators (not power banks)
        // Power banks are consumers/storage only in the cable network
        int available = 0;
        Map<BlockPos, IEnergyStorage> extractable = new HashMap<>();

        for (BlockPos ep : network.getEndpoints()) {
            if (!level.isLoaded(ep)) continue;
            net.minecraft.world.level.block.entity.BlockEntity be =
                    level.getBlockEntity(ep);
            // Extract from generators always
            // Extract from power banks only if they have energy and can extract
            boolean isGenerator = be instanceof com.arsenalcrafting.mod.blocks.entity.GeneratorBlockEntity;
            boolean isPowerBank = be instanceof com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity;
            if (!isGenerator && !isPowerBank) continue;
            IEnergyStorage storage = isPowerBank
                    ? getEnergyForBank((com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity) be, level, ep)
                    : getEnergyAt(level, ep);
            if (storage != null && storage.canExtract() && storage.getEnergyStored() > 0) {
                extractable.put(ep, storage);
                available += storage.getEnergyStored();
            }
        }

        if (level.getGameTime() % 100 == 0) {
            System.out.println("  [Network #" + network.getId() + "] available=" + available
                    + " extractable=" + extractable.size());
        }

        if (available <= 0) return;

        // Phase 2: find endpoints that can receive
        Map<BlockPos, IEnergyStorage> receivable = new HashMap<>();
        int totalWanted = 0;

        for (BlockPos ep : network.getEndpoints()) {
            if (!level.isLoaded(ep)) continue;
            net.minecraft.world.level.block.entity.BlockEntity be2 = level.getBlockEntity(ep);
            IEnergyStorage storage = (be2 instanceof com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity bank2)
                    ? getEnergyForBank(bank2, level, ep)
                    : getEnergyAt(level, ep);
            if (storage != null && storage.canReceive()) {
                int space = storage.getMaxEnergyStored() - storage.getEnergyStored();
                if (space > 0) {
                    receivable.put(ep, storage);
                    totalWanted += space;
                }
            }
        }

        if (receivable.isEmpty()) return;

        // Phase 3: distribute — pull from extractable, push to receivable
        // Simple round-robin: extract what receivers need, up to available
        int toDistribute = Math.min(available, totalWanted);

        // Extract from producers proportionally
        int extracted = 0;
        for (Map.Entry<BlockPos, IEnergyStorage> entry : extractable.entrySet()) {
            if (extracted >= toDistribute) break;
            int want = toDistribute - extracted;
            extracted += entry.getValue().extractEnergy(want, false);
        }

        // Push to receivers proportionally
        int remaining = extracted;
        for (Map.Entry<BlockPos, IEnergyStorage> entry : receivable.entrySet()) {
            if (remaining <= 0) break;
            int sent = entry.getValue().receiveEnergy(remaining, false);
            remaining -= sent;
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private boolean isCable(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        return level.getBlockState(pos).getBlock()
                instanceof com.arsenalcrafting.mod.blocks.CableBlock;
    }

    private boolean isEnergyEndpoint(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        return getEnergyAt(level, pos) != null;
    }

    private IEnergyStorage getEnergyAt(ServerLevel level, BlockPos pos) {
        // Use null side first — returns raw storage for generators
        IEnergyStorage nullSide = level.getCapability(
                Capabilities.EnergyStorage.BLOCK, pos,
                level.getBlockState(pos),
                level.getBlockEntity(pos),
                null);
        if (nullSide != null) return nullSide;

        // Fallback: try each side
        for (Direction dir : Direction.values()) {
            IEnergyStorage storage = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, pos,
                    level.getBlockState(pos),
                    level.getBlockEntity(pos),
                    dir);
            if (storage != null) return storage;
        }
        return null;
    }

    /**
     * Gets energy storage for a power bank, checking face config.
     * Returns extractable storage if any cable-facing side is OUT.
     * Returns receivable storage if any cable-facing side is IN.
     */
    private IEnergyStorage getEnergyForBank(
            com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity bank,
            ServerLevel level, BlockPos pos) {
        // Check which sides face cables — use that face's mode
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            if (!(level.getBlockState(neighbor).getBlock()
                    instanceof com.arsenalcrafting.mod.blocks.CableBlock)) continue;
            // This side faces a cable — return the face-aware storage for that side
            return bank.getEnergyForSide(dir);
        }
        // No cable adjacent — return raw storage
        return bank.getEnergyStorage();
    }

    public int getNetworkCount()  { return networks.size(); }
    public int getCableCount()    { return cableToNetwork.size(); }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextId", nextNetworkId);

        ListTag networkList = new ListTag();
        for (PowerNetwork network : networks.values()) {
            CompoundTag netTag = new CompoundTag();
            netTag.putInt("Id", network.getId());

            ListTag cables = new ListTag();
            for (BlockPos pos : network.getCables()) {
                cables.add(LongTag.valueOf(pos.asLong()));
            }
            netTag.put("Cables", cables);

            ListTag endpoints = new ListTag();
            for (BlockPos pos : network.getEndpoints()) {
                endpoints.add(LongTag.valueOf(pos.asLong()));
            }
            netTag.put("Endpoints", endpoints);
            networkList.add(netTag);
        }
        tag.put("Networks", networkList);
        return tag;
    }

    public static PowerNetworkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PowerNetworkSavedData data = new PowerNetworkSavedData();
        data.nextNetworkId = tag.getInt("NextId");

        ListTag networkList = tag.getList("Networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < networkList.size(); i++) {
            CompoundTag netTag = networkList.getCompound(i);
            int id = netTag.getInt("Id");
            PowerNetwork network = new PowerNetwork(id);

            ListTag cables = netTag.getList("Cables", Tag.TAG_LONG);
            for (int j = 0; j < cables.size(); j++) {
                BlockPos pos = BlockPos.of(((LongTag) cables.get(j)).getAsLong());
                network.addCable(pos);
                data.cableToNetwork.put(pos, id);
            }

            ListTag endpoints = netTag.getList("Endpoints", Tag.TAG_LONG);
            for (int j = 0; j < endpoints.size(); j++) {
                BlockPos pos = BlockPos.of(((LongTag) endpoints.get(j)).getAsLong());
                network.addEndpoint(pos);
            }

            data.networks.put(id, network);
        }
        return data;
    }
}