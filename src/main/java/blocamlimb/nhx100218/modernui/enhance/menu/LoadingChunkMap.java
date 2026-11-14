package blocamlimb.nhx100218.modernui.enhance.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

/**
 * Builds the 26.3-style chunk-status grid from the chunks 1.7.10 already has.
 *
 * <p>26.3 asks its server for a {@code ChunkLoadStatusView}; 1.7.10 has no such
 * view. This class therefore samples the loaded chunk map without calling
 * {@code provideChunk}, so drawing the loading screen never generates world
 * data. The observed states are mapped onto the same palette and cell size as
 * {@code LevelLoadingScreen}.</p>
 */
public final class LoadingChunkMap {

    /** {@code ChunkStatus.EMPTY} in 26.3. */
    public static final int EMPTY = 0xFF545454;
    /** {@code ChunkStatus.TERRAIN}. */
    public static final int TERRAIN = 0xFF303572;
    /** {@code ChunkStatus.FEATURES}. */
    public static final int FEATURES = 0xFF21C600;
    /** {@code ChunkStatus.LIGHT}. */
    public static final int LIGHT = 0xFFFFE0A0;
    /** {@code ChunkStatus.FULL}. */
    public static final int FULL = 0xFFFFFFFF;

    /** LevelLoadingScreen renders every chunk in a 2x2 cell. */
    public static final int CELL_SIZE = 2;
    /** The distance beyond the view ring that 26.3 includes in its status view. */
    private static final int GENERATION_RADIUS = 8;

    private LoadingChunkMap() {
    }

    /**
     * Samples the map around the player, the spawn point or the map origin.
     *
     * @param mc client
     * @return an immutable snapshot, or {@code null} when no world exists yet
     */
    public static Snapshot sample(Minecraft mc) {
        if (mc == null) {
            return null;
        }
        World world = selectWorld(mc);
        if (world == null) {
            return null;
        }
        EntityPlayer player = mc.thePlayer;
        int centerX;
        int centerZ;
        if (player != null) {
            centerX = floorChunk(player.posX);
            centerZ = floorChunk(player.posZ);
        } else {
            ChunkCoordinates spawn = world.getSpawnPoint();
            if (spawn == null) {
                centerX = floorChunk(world.getWorldInfo().getSpawnX());
                centerZ = floorChunk(world.getWorldInfo().getSpawnZ());
            } else {
                centerX = floorChunk(spawn.posX);
                centerZ = floorChunk(spawn.posZ);
            }
        }
        int radius = radius(mc);
        int diameter = radius * 2 + 1;
        int[] colors = new int[diameter * diameter];
        java.util.Arrays.fill(colors, EMPTY);

        try {
            if (world instanceof WorldServer) {
                sampleServer((WorldServer) world, centerX, centerZ, radius, diameter, colors);
            } else {
                sampleClient(world, centerX, centerZ, radius, diameter, colors);
            }
        } catch (Throwable ignored) {
            // A world may be replaced while the loading screen is still up. The
            // already-allocated EMPTY grid is a valid frame, so keep drawing it.
        }
        return new Snapshot(radius, diameter, colors);
    }

    private static World selectWorld(Minecraft mc) {
        int dimension = 0;
        if (mc.theWorld != null && mc.theWorld.provider != null) {
            dimension = mc.theWorld.provider.dimensionId;
        } else if (mc.thePlayer != null) {
            dimension = mc.thePlayer.dimension;
        }
        IntegratedServer server = mc.getIntegratedServer();
        if (server != null && server.worldServers != null) {
            for (WorldServer candidate : server.worldServers) {
                if (candidate != null && candidate.provider != null
                        && candidate.provider.dimensionId == dimension) {
                    return candidate;
                }
            }
            for (WorldServer candidate : server.worldServers) {
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return mc.theWorld;
    }

    private static void sampleServer(WorldServer world, int centerX, int centerZ, int radius, int diameter,
            int[] colors) {
        ChunkProviderServer provider = world.theChunkProviderServer;
        if (provider == null) {
            provider = (ChunkProviderServer) world.getChunkProvider();
        }
        if (provider == null) {
            return;
        }
        for (int z = 0; z < diameter; z++) {
            for (int x = 0; x < diameter; x++) {
                int chunkX = centerX + x - radius;
                int chunkZ = centerZ + z - radius;
                Object value = provider.loadedChunkHashMap.getValueByKey(
                        ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
                if (value instanceof Chunk) {
                    colors[x + z * diameter] = colorOf((Chunk) value);
                }
            }
        }
    }

    private static void sampleClient(World world, int centerX, int centerZ, int radius, int diameter,
            int[] colors) {
        if (!(world.getChunkProvider() instanceof ChunkProviderClient)) {
            return;
        }
        ChunkProviderClient provider = (ChunkProviderClient) world.getChunkProvider();
        for (int z = 0; z < diameter; z++) {
            for (int x = 0; x < diameter; x++) {
                int chunkX = centerX + x - radius;
                int chunkZ = centerZ + z - radius;
                if (provider.chunkExists(chunkX, chunkZ)) {
                    Chunk chunk = provider.provideChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        colors[x + z * diameter] = colorOf(chunk);
                    }
                }
            }
        }
    }

    private static int colorOf(Chunk chunk) {
        if (chunk == null) {
            return EMPTY;
        }
        if (!chunk.isTerrainPopulated) {
            return TERRAIN;
        }
        if (!chunk.isLightPopulated) {
            return LIGHT;
        }
        return FULL;
    }

    private static int radius(Minecraft mc) {
        int view = 8;
        if (mc.gameSettings != null && mc.gameSettings.renderDistanceChunks > 0) {
            view = mc.gameSettings.renderDistanceChunks;
        }
        return Math.max(10, Math.min(32, view + GENERATION_RADIUS + 1));
    }

    private static int floorChunk(double coordinate) {
        return ((int) Math.floor(coordinate)) >> 4;
    }

    private static int floorChunk(int coordinate) {
        return coordinate >> 4;
    }

    /** Immutable chunk-status grid copied for one frame. */
    public static final class Snapshot {

        public final int radius;
        public final int diameter;
        private final int[] colors;

        Snapshot(int radius, int diameter, int[] colors) {
            this.radius = radius;
            this.diameter = diameter;
            this.colors = colors == null ? new int[0] : colors;
        }

        /**
         * @param x column
         * @param z row
         * @return ARGB colour, or {@code EMPTY} outside the grid
         */
        public int color(int x, int z) {
            if (x < 0 || z < 0 || x >= diameter || z >= diameter) {
                return EMPTY;
            }
            int index = x + z * diameter;
            return index < colors.length ? colors[index] : EMPTY;
        }
    }
}
