/*package xyz.coolsa.biosphere;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.GoToBedAndSleepGoal;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.lwjgl.system.CallbackI.J;

import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.random.ChunkRandom;

public class OverworldBiosphereGen extends NoiseChunkGenerator {
    // If you know enough to be playing with the source, I made it easy. Just set this to false if you want to disable bridges
    private boolean generateBridges = true;




    protected ChunkRandom chunkRandom;
    protected Supplier<ChunkGeneratorSettings> settings;
    protected int sphereDistance = 175;
    protected double sphereRadiusMax = Biospheres.bsconfig.sphereRadius * 1.5625;
    protected double sphereRadiusMin = Biospheres.bsconfig.sphereRadius / 4.8;
    protected long seed;
	protected int oreSphereRadius;
	protected int lakeRadius;
	protected int shoreRadius;
	protected BiomeSource biomeSource;
	protected double generatedSphereHeight;
    protected BlockState defaultBridge = Blocks.OAK_PLANKS.getDefaultState();
    protected BlockState defaultEdge = Blocks.OAK_FENCE.getDefaultState();

    public static final Codec<OverworldBiosphereGen> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
				   Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed),
                   ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter((generator) -> generator.settings))
			.apply(instance, instance.stable(OverworldBiosphereGen::new)));

    @Override
    protected Codec<? extends NoiseChunkGenerator> getCodec() {
        return OverworldBiosphereGen.CODEC;
    }

    private double getSphereRadius(int centerX, int centerY){
        if(centerX == 0 && centerY == 0){
            return sphereRadiusMax;
        }
        // make random sized spheres
        this.chunkRandom.setCarverSeed(this.seed, centerX, centerY);
        return this.chunkRandom.nextInt((int) (sphereRadiusMax - sphereRadiusMin)) + sphereRadiusMin;
    }

    @Override
    public NoiseChunkGenerator withSeed(long seed) {
        return new OverworldBiosphereGen(this.biomeSource.withSeed(seed), seed, settings);
    }

    public OverworldBiosphereGen(BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
        super(biomeSource,seed,settings);
        this.seed = seed;
        this.settings = settings;
        this.biomeSource = biomeSource;
        this.chunkRandom = new ChunkRandom(seed);
     }

     @Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
        super.buildSurface(region, chunk);

		ChunkPos chunkPos = chunk.getPos();
        BlockPos centerPos = this.getNearestCenterSphere(chunkPos.getStartPos());
		BlockPos.Mutable current = new BlockPos.Mutable();

        // get the actual starting x position of the chunk.
        int xPos = chunkPos.getStartX();

        // get the actual starting z position of the chunk.
        int zPos = chunkPos.getStartZ();

        // for all vertical slices in the chunk, eat the terrain leaving only the sphere behind
        for(int x = 0; x < 16; x++){
            for(int z = 0; z < 16; z++){
                carveSphere(chunk, centerPos, x + xPos, z + zPos);

                // Set last 5 levels of the world to air from bedrock. Not dealing with it, nope.
                for(int y = getMinimumY(); y < getMinimumY() + 5; y++){
                    current.set(x + xPos, y, z + zPos);
                    if(chunk.getBlockState(current) == Blocks.BEDROCK.getDefaultState()){
                        chunk.setBlockState(current, Blocks.AIR.getDefaultState(), false);
                    }
                }
            }
        }

		// current.set(current.getX(), 100, current.getZ());
        // chunk.setBlockState(current, Blocks.SEA_LANTERN.getDefaultState(), false);
	}

    public BlockPos[] getClosestSpheres(BlockPos centerPos) {
		BlockPos[] nesw = new BlockPos[4];
		for (int i = 0; i < 4; i++) {
			int xMod = centerPos.getX();
			int zMod = centerPos.getZ();
			if (i / 2 < 1) {
				xMod += (int) Math.round(Math.pow(-1, i) * this.sphereDistance);
			} else {
				zMod += (int) Math.round(Math.pow(-1, i) * this.sphereDistance);
			}
			nesw[i] = this.getNearestCenterSphere(new BlockPos(xMod, 0, zMod));
		}
		return nesw;
	}

    private void carveSphere(Chunk chunk, BlockPos centerPos, int x, int z){
        // -64 : 396 = 320
        BlockPos.Mutable pos = new BlockPos.Mutable();

        // make random sized spheres
        double sphereRadius = getSphereRadius(centerPos.getX(), centerPos.getZ());

        for(int y = getMinimumY(); y < getWorldHeight() - getMinimumY(); y++){
            // we set our current position to the current column.
            pos.set(x,y,z);

            // now get the 2d distance to the center block.
            double radialDistance = Math.sqrt(pos.getSquaredDistance(centerPos.getX(), pos.getY(), centerPos.getZ(), false));

            // if we are inside of said distance, we know we can generate at some positions
            // inside this chunk.
            if (radialDistance <= sphereRadius) {
                // we also calculate the "height" of the column of the sphere.
                double sphereHeight = Math.sqrt(sphereRadius * sphereRadius - (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX()) - (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));

                // Ignore blocks from [centerpos(y) + height, centerpos(y)-height]
                if(pos.getY() > centerPos.getY()+(int)sphereHeight || pos.getY() < centerPos.getY()-(int)sphereHeight) {
                    // Cylinder around the outer most block of the sphere from min to max height
                    generateBridges(pos, centerPos, null, chunk, 7, false);
                } else {
                    // this is our section we want to keep
                }
            } else {
                // Everything outside of the cylinder
                generateBridges(pos, centerPos, null, chunk, 7, false);
            }
        }
    }

    private void generateBridges(BlockPos pos, BlockPos centerPos, ChunkRegion region, Chunk chunk, int bridgewidth, boolean asStructures){

        if(generateBridges && asStructures){
            
            BlockPos[] nesw = getClosestSpheres(centerPos);
            double radialDistance = Math.sqrt(centerPos.getSquaredDistance(pos.getX(), centerPos.getY(), pos.getZ(), false));
            
            double centerRadius = getSphereRadius(centerPos.getX(), centerPos.getZ());
            for (int i = 0; i < 4; i++) {
                if (radialDistance > centerRadius - 2) {
                    // slope = (y-y1)/(x-x1)
                    // y is always going to be the center, y1 is always the neighbor. Regardless of radius, y will remain the same, so no calculating in this step
                    double slope = nesw[i].getY() - centerPos.getY();
                    double var = 0;

                    double neighborRadius = getSphereRadius(nesw[i].getX(), nesw[i].getZ());

                    switch (i) {
                        case (0):
                            slope /= (double) (centerPos.getX() + centerRadius) - (nesw[i].getX() - neighborRadius) - 2;
                            var = centerPos.getX() - pos.getX() + centerRadius;

                            if (pos.getZ() <= centerPos.getZ() + 2 && pos.getZ() >= centerPos.getZ() - 2) {
                                if (pos.getX() > centerPos.getX()) {
                                    this.fillBridgeSlice(new BlockPos(pos.getX(), slope * var + centerPos.getY(), pos.getZ()),
                                                         new BlockPos(centerPos.getX(), slope * var + centerPos.getY(), centerPos.getZ()), 
                                                         region, 
                                                         new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ()), 
                                                         true);
                                    // +x axis
                                }
                            }
                            break;
                        case (1):
                            slope /= (double) ((centerPos.getX() - centerRadius) - (nesw[i].getX() + neighborRadius)) - 2;
                            var = centerPos.getX() - pos.getX() - centerRadius;

                            if (pos.getZ() <= centerPos.getZ() + 2 && pos.getZ() >= centerPos.getZ() - 2) {
                                if (pos.getX() < centerPos.getX()) {
                                    this.fillBridgeSlice(new BlockPos(pos.getX(), slope * var + centerPos.getY(), pos.getZ()),
                                                         new BlockPos(centerPos.getX(), slope * var + centerPos.getY(), centerPos.getZ()), 
                                                         region, 
                                                         new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ()), 
                                                         true);
                                    // -x axis
                                }
                            }
                            break;
                        case (2):
                            slope /= (double) (centerPos.getZ() + centerRadius) - (nesw[i].getZ() - neighborRadius) - 2;
                            var = centerPos.getZ() - pos.getZ() + centerRadius;
                            if (pos.getX() <= centerPos.getX() + 2 && pos.getX() >= centerPos.getX() - 2) {
                                if (pos.getZ() > centerPos.getZ()) {
                                    this.fillBridgeSlice(new BlockPos(pos.getX(), slope * var + centerPos.getY(), pos.getZ()),
                                                         new BlockPos(centerPos.getX(), slope * var + centerPos.getY(), centerPos.getZ()), 
                                                         region, 
                                                         new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ()), 
                                                         false);
                                    // +z axis
                                }
                            }
                            break;
                        case (3):
                            slope /= (double) ((centerPos.getZ() - centerRadius) - (nesw[i].getZ() + neighborRadius)) - 2;
                            var = centerPos.getZ() - pos.getZ() - centerRadius;
                            if (pos.getX() <= centerPos.getX() + 2 && pos.getX() >= centerPos.getX() - 2) {
                                if (pos.getZ() < centerPos.getZ()) {
                                    this.fillBridgeSlice(new BlockPos(pos.getX(), slope * var + centerPos.getY(), pos.getZ()),
                                                         new BlockPos(centerPos.getX(), slope * var + centerPos.getY(), centerPos.getZ()),
                                                         region, 
                                                         new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ()), 
                                                         false);
                                    // -z axis
                                }
                            }
                            break;
                    }
                }
            }
        } else {
            // Do not generate bridges
            setBlock(null, chunk, pos, Blocks.AIR.getDefaultState());
        }
     }

     public void fillBridgeSlice(BlockPos pos, BlockPos centerPos, ChunkRegion region, BlockPos.Mutable current, boolean isOnXAxis) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int cx = centerPos.getX();
		int cz = centerPos.getZ();
        setBlock(region, null, current.set(x, y - 1, z), this.defaultBridge);
		setBlock(region, null, current.set(x, y, z), Blocks.AIR.getDefaultState());
		setBlock(region, null, current.set(x, y + 1, z), Blocks.AIR.getDefaultState());
		if(isOnXAxis) {
			setBlock(region, null, current.set(x, y, cz + 2), this.defaultEdge);
			region.getChunk(pos).markBlockForPostProcessing(current.set(x, y, cz+2));
			setBlock(region, null, current.set(x, y, cz - 2), this.defaultEdge);
			region.getChunk(pos).markBlockForPostProcessing(current.set(x, y, cz-2));
		} else {
			setBlock(region, null, current.set(cx+2, y, z), this.defaultEdge);
			region.getChunk(pos).markBlockForPostProcessing(current.set(cx+2, y, z));
			setBlock(region, null, current.set(cx-2, y, z), this.defaultEdge);
			region.getChunk(pos).markBlockForPostProcessing(current.set(cx-2, y, z));
		}
		setBlock(region, null, current.set(x, y + 2, z), Blocks.AIR.getDefaultState());
		setBlock(region, null, current.set(x, y + 3, z), Blocks.AIR.getDefaultState());
	}

     @Override
     public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
        // Generate the normal terrain features
        super.generateFeatures(region, accessor);
        
        // encapsulate our spheres
        BlockPos chunkCenter = new BlockPos(region.getCenterPos().x * 16, 0, region.getCenterPos().z * 16);
        BlockPos.Mutable current = new BlockPos.Mutable();

        BlockPos centerPos = this.getNearestCenterSphere(chunkCenter);

        // make random sized spheres
        double sphereRadius = getSphereRadius(centerPos.getX(), centerPos.getZ());
        
        // now lets iterate over every every column in the chunk.
		for (final BlockPos pos : BlockPos.iterate(chunkCenter.getX() - 7, 0, chunkCenter.getZ() - 7, chunkCenter.getX() + 8, 0, chunkCenter.getZ() + 8)) {
            
			// we set our current position to the current column.
			current.set(pos);

			// now get the 2d distance to the center block.
			double radialDistance = Math.sqrt(pos.getSquaredDistance(centerPos.getX(), pos.getY(), centerPos.getZ(), false));

			// if we are inside of said distance, we know we can generate at some positions inside this chunk.
			if (radialDistance <= sphereRadius) {
				// we also calculate the "height" of the column of the sphere.
				double sphereHeight = Math.sqrt(sphereRadius * sphereRadius - (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX()) - (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));

				// now lets iterate over ever position inside of this sphere.
				for (int y = centerPos.getY() - (int) sphereHeight; y <= sphereHeight + centerPos.getY(); y++) {
					double newRadialDistance = Math.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					
					if (newRadialDistance <= sphereRadius - 1) {
						continue;
					}

                    current.set(pos.getX(), y, pos.getZ());

					region.setBlockState(current.set(pos.getX(), y, pos.getZ()), getBorderBlockState(null, region, centerPos, current), 0);
				}
            }

            generateBridges(pos, centerPos, region, null, 7, true);
		}
     }

     private BlockState getBorderBlockState(Chunk chunk, ChunkRegion region, BlockPos centerPos, BlockPos pos){
        BlockState blockState;
        BlockState currentBlockState;

        this.chunkRandom.setDeepslateSeed(seed, pos.getX(), pos.getY(), pos.getZ());
        int rng = this.chunkRandom.nextInt(100);

        if(chunk == null){
            currentBlockState = region.getBlockState(pos);
        } else {
            currentBlockState = chunk.getBlockState(pos);
        }

        if(currentBlockState == Blocks.AIR.getDefaultState()){
            if (pos.getY() < getSeaLevel()) {
                if(rng <= 1){
                    blockState = Blocks.GLOWSTONE.getDefaultState();
                } else {
                    blockState = Blocks.TINTED_GLASS.getDefaultState();
                }
            } else {
                switch(rng){
                    case 0: blockState = Blocks.WHITE_STAINED_GLASS.getDefaultState(); break;
                    case 1: blockState = Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState(); break;
                    case 2: blockState = Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState(); break;
                    case 3: blockState = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(); break;
                    case 4: blockState = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(); break;
                    default: blockState = Blocks.GLASS.getDefaultState(); break;
                }
            }
        } else if(currentBlockState == Blocks.WATER.getDefaultState()){
            blockState = Blocks.BLUE_STAINED_GLASS.getDefaultState();
        }else if(currentBlockState == Blocks.LAVA.getDefaultState()){
            blockState = Blocks.MAGMA_BLOCK.getDefaultState();
        } else {
            if(rng <= 4){
                blockState = Blocks.CRYING_OBSIDIAN.getDefaultState();
            } else {
                blockState = Blocks.OBSIDIAN.getDefaultState();
            }
        }

        return blockState;
     }

     // Just some error handling...
     private boolean setBlock(ChunkRegion region, Chunk chunk, BlockPos pos, BlockState state){
        if(pos.getY() > getMinimumY() && pos.getY() < getWorldHeight() - 1){
            if(region == null){
                if(chunk.getBlockState(pos) != state){
                    chunk.setBlockState(pos, state, false);
                }
            } else {
                if(region.getBlockState(pos) != state){
                    region.setBlockState(pos, state, 0);
                }
            }
            return true;
        } else {
            return false;
        }
     }

     public BlockPos getNearestCenterSphere(BlockPos pos) {
		int xPos = pos.getX();
		int zPos = pos.getZ();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance) * this.sphereDistance;
        int centerY = getSeaLevel();
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
        
        double sphereRadius = getSphereRadius(centerX, centerZ);

        // Resolve Y level if not at 0,0. 0,0 must guarantee a place to spawn...
        if(centerX != 0 || centerY != 0){
            double upperbound = getWorldHeight() * 3.0 / 4.0 - sphereRadius; // 208
            double lowerbound = getMinimumY() + sphereRadius;    // 0
            double sealevel = (upperbound - lowerbound) / 2; //getSeaLevel();                          // 63
            double range;

            if(sealevel - lowerbound > upperbound - sealevel){
                // top to sea level is shorter, so that is our range
                range = upperbound - sealevel;                   // 145
            } else {
                // sea level to bottom is shorter, so that is our range
                range = sealevel - lowerbound;                   // 15
            }

            // gaussian generates a bell curve from -1 to 1.
            double gauss = chunkRandom.nextGaussian(); // [-3,3]
            if(gauss>3){
                gauss = 3;
            }
            if(gauss<-3){
                gauss=-3;
            }
            
            double ywork = (gauss-0.5) * range/3; //[-15, 15]
            ywork = ywork + sealevel; // [-64, 192]

            centerY = (int)((ywork));
            if(centerY < sphereRadius){
                String s = "";
            }
        }

		//int centerY = (int) ((Math.pow((this.chunkRandom.nextFloat() % 0.7) - 0.5, 3) + 0.5) * (sphereRadius * 2 - sphereRadius * 4)) + sphereRadius * 2;
		return new BlockPos(centerX, centerY, centerZ);
	}

}
*/