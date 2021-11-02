package xyz.coolsa.biosphere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;

import javax.swing.plaf.synth.Region;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

public class BiospheresChunkGenerator extends ChunkGenerator {
	protected final long seed;
	protected final int sphereDistance;
	protected final int sphereRadius;
	protected final int oreSphereRadius;
	protected final int lakeRadius;
	protected final int shoreRadius;
	protected final BiomeSource biomeSource;
	protected final ChunkRandom chunkRandom;
	protected final OctavePerlinNoiseSampler noiseSampler;
	protected final BlockState defaultBlock;
	protected final BlockState defaultNetherBlock;
	protected final BlockState defaultFluid;
	protected final BlockState defaultBridge;
	protected final BlockState defaultEdge;
	protected double generatedSphereHeight;

	public static final Codec<BiospheresChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
					Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed),
					Codec.INT.fieldOf("sphere_distance").forGetter((generator) -> generator.sphereDistance),
					Codec.INT.fieldOf("sphere_radius").forGetter((generator) -> generator.sphereRadius),
					Codec.INT.fieldOf("lake_radius").forGetter((generator) -> generator.lakeRadius),
					Codec.INT.fieldOf("shore_radius").forGetter((generator) -> generator.shoreRadius))
			.apply(instance, instance.stable(BiospheresChunkGenerator::new)));

	public BiospheresChunkGenerator(BiomeSource biomeSource, long seed, int sphereDistance, int sphereRadius,
			int lakeRadius, int shoreRadius) {
		super(biomeSource, new StructuresConfig(Optional.of(StructuresConfig.DEFAULT_STRONGHOLD), Collections.emptyMap()));
		this.biomeSource = biomeSource;
		this.seed = seed;
		this.sphereDistance = sphereRadius * 4;
		this.sphereRadius = sphereRadius;
		this.oreSphereRadius = 8; // TODO: add in ore spheres. also set to -ve to do no ore spheres
		this.lakeRadius = lakeRadius;
		this.shoreRadius = shoreRadius;
		this.defaultBlock = Blocks.STONE.getDefaultState();
		this.defaultNetherBlock = Blocks.NETHERRACK.getDefaultState();
		this.defaultFluid = Blocks.WATER.getDefaultState();
		this.defaultBridge = Blocks.OAK_PLANKS.getDefaultState();
		this.defaultEdge = Blocks.OAK_FENCE.getDefaultState();
		this.chunkRandom = new ChunkRandom(this.seed);
		this.chunkRandom.skip(1000);
		this.noiseSampler = new OctavePerlinNoiseSampler(this.chunkRandom, IntStream.rangeClosed(-3, 0));
		this.generatedSphereHeight = this.sphereRadius;

	}

	@Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
		BlockPos centerPos = this.getNearestCenterSphere(chunk.getPos().getStartPos());
		BlockPos.Mutable current = new BlockPos.Mutable();
		for (BlockPos pos : BlockPos.iterate(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(),
				chunk.getPos().getEndX(), 0, chunk.getPos().getEndZ())) {

			if (region.getBiome(centerPos).getCategory() == Biome.Category.NETHER){
				region.getBiome(current.set(pos)).buildSurface((Random) this.chunkRandom, chunk, pos.getX(), pos.getZ(),
						centerPos.getY() * 2, 0.0625, this.defaultNetherBlock, this.defaultFluid, -10, 0, this.seed);
			} else {
				region.getBiome(current.set(pos)).buildSurface((Random) this.chunkRandom, chunk, pos.getX(), pos.getZ(),
						centerPos.getY() * 4, 0.0625, this.defaultBlock, this.defaultFluid, -10, 0, this.seed);
			}
		}
	}

	@Override
	public int getHeight(int x, int z, Type heightmap, HeightLimitView world) {
		int sphereY = this.getNearestCenterSphere(new BlockPos(x, 0, z)).getY();
		double sphereX = this.getNearestCenterSphere(new BlockPos(x, 0, z)).getX();
		double sphereZ = this.getNearestCenterSphere(new BlockPos(x, 0, z)).getZ();
		double distanceToNearestSphere = Math.sqrt((Math.pow(sphereX, 2) + Math.pow(sphereZ, 2)) - Math.pow(x, 2) + Math.pow(z, 2));
		if (distanceToNearestSphere < sphereRadius) return sphereY;
		else return world.getBottomY();
		//return (int) this.generatedSphereHeight;
	}

	// TODO: Get structure gen working, probably need to make a new heightmap.

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
		BlockState[] states = new BlockState[(int) this.generatedSphereHeight];
		return new VerticalBlockSample(this.getHeight(x, z, Type.WORLD_SURFACE_WG, world), states);
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Executor world, StructureAccessor accessor, Chunk chunk) {
		// get the starting position of the chunk we will generate.
		ChunkPos chunkPos = chunk.getPos();
		// also get the current working block.
		BlockPos.Mutable current = new BlockPos.Mutable();
		// get the actual starting x position of the chunk.
		int xPos = chunkPos.getStartX();
		// get the actual starting z position of the chunk.
		int zPos = chunkPos.getStartZ();
		// find the center of the nearest sphere.
		BlockPos centerPos = this.getNearestCenterSphere(chunkPos.getStartPos());
		// TODO: ore sphere conditional generator.
		BlockPos oreCenterPos = this.getNearestOreSphere(chunkPos.getStartPos());
		// get the block that should be at the center.
		BlockState fluidBlock = this.getLakeBlock(centerPos, chunk.getBiomeArray().getBiomeForNoiseGen(chunkPos));
		// begin keeping track of the heightmap.
		Heightmap oceanHeight = chunk.getHeightmap(Type.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getHeightmap(Type.WORLD_SURFACE_WG);
		// now lets iterate over every column in the chunk.
		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, 0, zPos + 15)) {
			// we set our current position to the current column.
			current.set(pos);
			// now get the 2d distance to the center block.
			double radialDistance = Math
					.sqrt(pos.getSquaredDistance(centerPos.getX(), pos.getY(), centerPos.getZ(), false));
			double oreRadialDistance = Math
					.sqrt(pos.getSquaredDistance(oreCenterPos.getX(), pos.getY(), oreCenterPos.getZ(), false));
			// if we are inside of said distance, we know we can generate at some positions
			// inside this chunk.
			if (radialDistance <= this.sphereRadius) {
				// so we sample the noise height.
				double noise = this.noiseSampler.sample(pos.getX() / 8.0, pos.getZ() / 8.0, 1 / 16.0, 1 / 16.0) / 16;
				// we also calculate the "height" of the column of the sphere.
				double sphereHeight = Math.sqrt(this.sphereRadius * this.sphereRadius
						- (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX())
						- (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));
				generatedSphereHeight = sphereHeight;
				// now lets iterate over ever position inside of this sphere.
				for (int y = centerPos.getY() - (int) sphereHeight; y <= sphereHeight + centerPos.getY(); y++) {
					// calculate the radial distance for lake gen.
					double lakeDistance = Math.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					// calculate the radial distance for lake gen in 2d space.
					double lakeDistance2d = Math
							.sqrt(centerPos.getSquaredDistance(pos.getX(), centerPos.getY(), pos.getZ(), false));

					// also lets do some math for our noise generator.
					double noiseTemp = (noise + y / centerPos.getY());
					// by default, the block is air.
					BlockState blockState = Blocks.AIR.getDefaultState();
					// if we are below the noise gradient, we can set this block to stone!
					if (y * noiseTemp < centerPos.getY()) {
						if (chunk.getBiomeArray().getBiomeForNoiseGen(chunkPos).getCategory() == Biome.Category.NETHER) {
							blockState = this.defaultNetherBlock;
						} else {
							blockState = this.defaultBlock;
						}
					}
					// now lets check if we can do our lake gen.
					if (((blockState.equals(this.defaultBlock) || blockState.equals(this.defaultNetherBlock))
							&& (lakeDistance2d <= this.lakeRadius))	&& !fluidBlock.equals(Blocks.AIR.getDefaultState())) {
						// if we are above the height and noise value, we will generate air.
						if (y >= centerPos.getY() && (!fluidBlock.equals(Blocks.STONE.getDefaultState()) || !fluidBlock.equals(Blocks.NETHERRACK.getDefaultState()))) {
							blockState = Blocks.AIR.getDefaultState();
						}
						// otherwise, we are inside of a valid position, so we go ahead and generate our
						// fluids.
						else if (lakeDistance <= this.lakeRadius) {
							blockState = fluidBlock;
						}
					}
					chunk.setBlockState(current.set(pos.getX(), y, pos.getZ()), blockState, false);
					oceanHeight.trackUpdate(pos.getX() & 0xF, y & 0xF, pos.getZ() & 0xF, blockState);
					worldSurface.trackUpdate(pos.getX() & 0xF, y & 0xF, pos.getZ() & 0xF, blockState);
				}
			}
			if (oreRadialDistance <= this.oreSphereRadius) {
				double oreNoise = this.noiseSampler.sample(pos.getX() / 8.0, pos.getZ() / 8.0, 1 / 16.0, 1 / 16.0) / 16;
				double oreSphereHeight = Math.sqrt(this.oreSphereRadius * this.oreSphereRadius
						- (oreCenterPos.getX() - pos.getX()) * (oreCenterPos.getX() - pos.getX())
						- (pos.getZ() - oreCenterPos.getZ()) * (pos.getZ() - oreCenterPos.getZ()));
				generatedSphereHeight = oreSphereHeight;
				BlockState oreState;
				for (int y = oreCenterPos.getY() - (int) oreSphereHeight; y <= oreSphereHeight + oreCenterPos.getY(); y++) {
					oreState = defaultBlock;

					chunk.setBlockState(current.set(pos.getX(), y, pos.getZ()), randomBlock(), false);

				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}
	private BlockState randomBlock() {
		int rng = this.chunkRandom.nextInt(784);
		if (rng == 1) {
			return Blocks.EMERALD_ORE.getDefaultState();
		} else if (rng > 1 && rng <= 3) {
			return Blocks.DIAMOND_ORE.getDefaultState();
		} else if (rng > 3 && rng <= 5) {
			return Blocks.LAPIS_ORE.getDefaultState();
		} else if (rng > 5 && rng <= 7) {
			return Blocks.REDSTONE_ORE.getDefaultState();
		} else if (rng > 7 && rng <= 10) {
			return Blocks.GOLD_ORE.getDefaultState();
		} else if (rng > 10 && rng <= 16) {
			return Blocks.IRON_ORE.getDefaultState();
		} else return defaultBlock;
	}
	/*@Override
	public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
	}*/

	public BlockPos getNearestCenterSphere(BlockPos pos) {
		int xPos = pos.getX();
		int zPos = pos.getZ();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int centerY = (int) ((Math.pow((this.chunkRandom.nextFloat() % 0.7) - 0.5, 3) + 0.5)
				* (this.sphereRadius * 2 - this.sphereRadius * 4)) + this.sphereRadius * 2;
		return new BlockPos(centerX, centerY, centerZ);
	}

	public BlockPos getNearestOreSphere(BlockPos pos) {
		int xPos = pos.getX();
		int zPos = pos.getZ();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance - 0.5) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance - 0.5) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int centerY = this.chunkRandom.nextInt(256 - this.oreSphereRadius * 4) + this.oreSphereRadius * 2;
		return new BlockPos(centerX + this.sphereDistance / 2, centerY, centerZ + this.sphereDistance / 2);
	}

	public BlockState getLakeBlock(BlockPos center, Biome biome) {
		this.chunkRandom.setTerrainSeed(center.getX(), center.getZ());
		int rng = this.chunkRandom.nextInt(10);
		BlockState state;
		if (biome.getCategory() == Biome.Category.NETHER) {
			state = Blocks.NETHERRACK.getDefaultState();
		} else {
			state = Blocks.STONE.getDefaultState();
		}
		if (rng >= 2 && rng <= 8)
			if (biome.getCategory() == Biome.Category.NETHER) state = Blocks.LAVA.getDefaultState();
			else state = this.defaultFluid;
		else if (rng == 9) {
			state = Blocks.LAVA.getDefaultState();
		}
		return state;
	}

	@Override
	public ChunkGenerator withSeed(long seed) {
		return new BiospheresChunkGenerator(this.biomeSource.withSeed(seed), seed, this.sphereRadius * 4,
				this.sphereRadius, this.lakeRadius, this.shoreRadius);
	}

	@Override
	public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
		this.finishBiospheres(region);
		//TODO: rewrite *THIS* method to fix https://github.com/coolsa/Biospheres/issues/5 (Generator Features generate inconsistently)
		super.generateFeatures(region, accessor);
	}

	public void generateGlass(ChunkRegion region, BlockPos centerPos, BlockPos pos) {
		BlockState blockState;
		if (region.getBiome(centerPos).getCategory() == Biome.Category.UNDERGROUND) {
			blockState = Blocks.TINTED_GLASS.getDefaultState();
		} else if (region.getBiome(centerPos).getCategory() == Biome.Category.NETHER) {
			blockState = Blocks.RED_STAINED_GLASS.getDefaultState();
		} else {
			blockState = Blocks.GLASS.getDefaultState();
		}
		region.setBlockState(pos, blockState, 0);
	}
	public void generateStone(ChunkRegion region, BlockPos centerPos, BlockPos pos) {
		BlockState blockState;
		if (region.getBiome(centerPos).getCategory() == Biome.Category.NETHER) {
			blockState = this.defaultNetherBlock;
		} else {
			blockState = this.defaultBlock;
		}
		region.setBlockState(pos, blockState, 0);
	}

	public void finishBiospheres(ChunkRegion region) {
		BlockPos chunkCenter = new BlockPos(region.getCenterPos().x * 16, 0, region.getCenterPos().z * 16);
		BlockPos.Mutable current = new BlockPos.Mutable();
		BlockPos centerPos = this.getNearestCenterSphere(chunkCenter);
		boolean isGlassGenerated;
		for (final BlockPos pos : BlockPos.iterate(chunkCenter.getX() - 7, 0, chunkCenter.getZ() - 7,
				chunkCenter.getX() + 8, 0, chunkCenter.getZ() + 8)) {
			current.set(pos);
			double radialDistance = Math
					.sqrt(centerPos.getSquaredDistance(pos.getX(), centerPos.getY(), pos.getZ(), false));
			double noise = this.noiseSampler.sample(pos.getX() / 8.0, pos.getZ() / 8.0, 1 / 16.0, 1 / 16.0) / 8;
			double sphereHeight = Math.sqrt(this.sphereRadius * this.sphereRadius
					- (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX())
					- (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));
			if (radialDistance <= this.sphereRadius + 16) {
				for (int y = centerPos.getY() - (int) sphereHeight; y <= sphereHeight + centerPos.getY(); y++) {
					double newRadialDistance = Math
							.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					double noiseTemp = (noise + y / centerPos.getY());
					BlockState blockState;
					if (newRadialDistance <= this.sphereRadius - 1) {
						continue;
					}
					if (y * noiseTemp >= centerPos.getY()) {
						generateGlass(region, centerPos, current.set(pos.getX(), y, pos.getZ()));
					} else {
						generateStone(region, centerPos, current.set(pos.getX(), y, pos.getZ()));
					}
				}
				double largerSphereHeight = Math.sqrt((this.sphereRadius + 16) * (this.sphereRadius + 16)
						- (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX())
						- (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));
				for (int y = 0; y <= largerSphereHeight + centerPos.getY(); y++) {
					double newRadialDistance = Math
							.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					if (newRadialDistance >= this.sphereRadius) {
						region.setBlockState(current.set(pos.getX(), y, pos.getZ()), Blocks.AIR.getDefaultState(), 0);
					}
				}
			}
			if (pos.getX() == centerPos.getX() && pos.getZ() == centerPos.getZ()) {
				if (region.getBiome(centerPos).getCategory() == Biome.Category.UNDERGROUND) {
					region.setBlockState(current.set(centerPos.getX(), centerPos.getY() + sphereRadius - 1, centerPos.getZ()), Blocks.TINTED_GLASS.getDefaultState(), 0);
				} else if (region.getBiome(centerPos).getCategory() == Biome.Category.NETHER) {
					region.setBlockState(current.set(centerPos.getX(), centerPos.getY() + sphereRadius - 1, centerPos.getZ()), Blocks.RED_STAINED_GLASS.getDefaultState(), 0);
				} else {
					region.setBlockState(current.set(centerPos.getX(), centerPos.getY() + sphereRadius - 1, centerPos.getZ()), Blocks.GLASS.getDefaultState(), 0);
				}
			}

			this.makeBridges(pos, centerPos, this.getClosestSpheres(centerPos), region, current);
		}
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

	public void makeBridges(BlockPos pos, BlockPos centerPos, BlockPos[] nesw, ChunkRegion region,
			BlockPos.Mutable current) {
		// generating bridges!
		double radialDistance = Math
				.sqrt(centerPos.getSquaredDistance(pos.getX(), centerPos.getY(), pos.getZ(), false));
//		if (radialDistance < this.sphereRadius) {
//			return;
//		}
		for (int i = 0; i < 4; i++) {
			if (radialDistance > this.sphereRadius - 2) {
				double slope = nesw[i].getY() - centerPos.getY();
//				BlockState blockState = Blocks.AIR.getDefaultState();
				double currentPos = 0;
				switch (i) {
				case (0):
					slope /= Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) - 2 * this.sphereRadius;
//					blockState = Blocks.BLUE_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getX() - pos.getX() + this.sphereRadius;
					if (pos.getZ() <= centerPos.getZ() + 2 && pos.getZ() >= centerPos.getZ() - 2) {
						if (pos.getX() > centerPos.getX()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, true, true);
							// x axis
						}
					}
					break;
				case (1):
					slope /= -Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) + 2 * this.sphereRadius;
//					blockState = Blocks.PURPLE_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getX() - pos.getX() - this.sphereRadius;
					if (pos.getZ() <= centerPos.getZ() + 2 && pos.getZ() >= centerPos.getZ() - 2) {
						if (pos.getX() < centerPos.getX()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, true, false);
							// x axis
						}
					}
					break;
				case (2):
					slope /= -Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) + 2 * this.sphereRadius;
//					blockState = Blocks.RED_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getZ() - pos.getZ() + this.sphereRadius;
					if (pos.getX() <= centerPos.getX() + 2 && pos.getX() >= centerPos.getX() - 2) {
						if (pos.getZ() > centerPos.getZ()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, false, true);
							// z axis
						}
					}
					break;
				case (3):
					slope /= Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) - 2 * this.sphereRadius;
//					blockState = Blocks.YELLOW_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getZ() - pos.getZ() - this.sphereRadius;
					if (pos.getX() <= centerPos.getX() + 2 && pos.getX() >= centerPos.getX() - 2) {
						if (pos.getZ() < centerPos.getZ()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, false, false);
							// z axis
						}
					}
					break;
				}
			}
		}
	}



	public void fillBridgeSlice(BlockPos pos, BlockPos centerPos, ChunkRegion region, BlockPos.Mutable current, boolean isOnXAxis, boolean isPositive) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int cx = centerPos.getX();
		int cz = centerPos.getZ();
		region.setBlockState(current.set(x, y - 1, z), this.defaultBridge, 0);
		region.setBlockState(current.set(x, y, z), Blocks.AIR.getDefaultState(), 0);
		region.setBlockState(current.set(x, y + 1, z), Blocks.AIR.getDefaultState(), 0);
		if(isOnXAxis) {
			region.setBlockState(current.set(x, y, cz + 2), this.defaultEdge, 0);
			region.getChunk(pos).markBlockForPostProcessing(current.set(x, y, cz+2));
			region.setBlockState(current.set(x, y, cz - 2), this.defaultEdge, 0);
			region.getChunk(pos).markBlockForPostProcessing(current.set(x, y, cz-2));
			//region.setBlockState(current.set(x, y+1, cz+2), Blocks.OAK_LEAVES.getDefaultState().with(Properties.PERSISTENT, true), 0);
			//region.setBlockState(current.set(x, y+1, cz-2), Blocks.OAK_LEAVES.getDefaultState().with(Properties.PERSISTENT, true), 0);
		} else {
			region.setBlockState(current.set(cx+2, y, z), this.defaultEdge, 0);
			region.getChunk(pos).markBlockForPostProcessing(current.set(cx+2, y, z));
			region.setBlockState(current.set(cx-2, y, z), this.defaultEdge, 0);
			region.getChunk(pos).markBlockForPostProcessing(current.set(cx-2, y, z));
			//region.setBlockState(current.set(cx+2, y+1, z), Blocks.OAK_LEAVES.getDefaultState().with(Properties.PERSISTENT, true), 0);
			//region.setBlockState(current.set(cx-2, y+1, z), Blocks.OAK_LEAVES.getDefaultState().with(Properties.PERSISTENT, true), 0);
		}
		region.setBlockState(current.set(x, y + 2, z), Blocks.AIR.getDefaultState(), 0);
		region.setBlockState(current.set(x, y + 3, z), Blocks.AIR.getDefaultState(), 0);
	}

	@Override
	protected Codec<? extends ChunkGenerator> getCodec() {
		// TODO Auto-generated method stub
		return BiospheresChunkGenerator.CODEC;
	}

//	@Override
//	public void populateEntities(ChunkRegion region) {
//	}
}
