package xyz.coolsa.biosphere;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.RegistryLookupCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;

import java.util.List;

//import net.minecraft.world.biome.BuiltinBiomes;

public class BiospheresBiomeSource extends BiomeSource {

	public static final Codec<BiospheresBiomeSource> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(RegistryLookupCodec.of(Registry.BIOME_KEY).forGetter((generator) -> generator.registry),
					Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed))
			.apply(instance, instance.stable(BiospheresBiomeSource::new)));
	protected final long seed;
	protected final int sphereDistance;
	protected final int sphereRadius;
	protected final ChunkRandom chunkRandom;
	protected final Registry<Biome> registry;
////	protected final long seed;
////	protected final int squareSize 
////	protected final int curveSize;
////	private final BiomeLayerSampler biomeSampler;
	//for the biomes, should i just clone every single one, register them as "biosphere:*biome*", and then generate with those?
	//so we can do ores and stuff? that seems to be one of the better ideas i would say...
	protected static final List<RegistryKey<Biome>> BIOMES = ImmutableList.<RegistryKey<Biome>>of(
		 BiomeKeys.PLAINS
		,BiomeKeys.FOREST
		,BiomeKeys.BADLANDS
		,BiomeKeys.BEACH
		,BiomeKeys.JUNGLE
		,BiomeKeys.CRIMSON_FOREST
		,BiomeKeys.WARPED_FOREST
		,BiomeKeys.MUSHROOM_FIELDS
		,BiomeKeys.DESERT
		,BiomeKeys.DARK_FOREST
		,BiomeKeys.SNOWY_TUNDRA
		,BiomeKeys.BAMBOO_JUNGLE
		,BiomeKeys.BASALT_DELTAS
		,BiomeKeys.FLOWER_FOREST
		,BiomeKeys.SNOWY_TAIGA
		,BiomeKeys.TAIGA
		,BiomeKeys.LUSH_CAVES
		,BiomeKeys.SWAMP
		//,BiomeKeys.MOUNTAINS
		,BiomeKeys.NETHER_WASTES
		,BiomeKeys.ICE_SPIKES
		,BiomeKeys.OCEAN
	);

////	public static final Codec<BiosphereBiomeSource> CODEC = Codec.mapPair(Identifier.CODEC.flatXmap(
////			identifier -> Optional.<MultiNoiseBiomeSource.Preset>ofNullable(this.Preset.field_24724.get(identifier))
////					.map(DataResult::success).orElseGet(() -> DataResult.error("Unknown preset: " + identifier)),
//BuiltinRegistries.BIOME.get(BuiltinBiomesreset -> DataResult.success(preset.id)).fieldOf("preset"), Codec.LONG.fieldOf("seed")).stable();
//
public BiospheresBiomeSource(Registry<Biome> registry, long seed) {
		super(ImmutableList.of());
		this.seed = seed;
		this.sphereDistance = Biospheres.bsconfig.sphereRadius * 4;
		this.sphereRadius = Biospheres.bsconfig.sphereRadius;
		this.chunkRandom = new ChunkRandom(seed);
		this.registry = registry;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		if (this.getDistanceFromSphere(biomeX + 1, biomeZ + 1) < this.sphereRadius + 6) {
			return registry.get(this.getBiomeForSphere(biomeX, biomeZ));
		}
//		return BiomeSelectors.vanilla().;
		return registry.get(BiomeKeys.THE_VOID);
	}

	public double getDistanceFromSphere(int biomeX, int biomeZ) {
		int centerX = (int) Math.round(biomeX * 4 / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(biomeZ * 4 / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		BlockPos center = new BlockPos(centerX, 0, centerZ);
		return Math.sqrt(center.getSquaredDistance(biomeX * 4, 0, biomeZ * 4, true));
	}

	public RegistryKey<Biome> getBiomeForSphere(int biomeX, int biomeZ) {
		int centerX = (int) Math.round(biomeX * 4 / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(biomeZ * 4 / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int randomChoice = this.chunkRandom.nextInt(BiospheresBiomeSource.BIOMES.size());
		return BiospheresBiomeSource.BIOMES.get(randomChoice);
	}

	@Override
	public BiomeSource withSeed(long seed) {
		return new BiospheresBiomeSource(this.registry, seed);
		// TODO Auto-generated method stub
//		return new BiosphereBiomeSource(seed, this.squareSize, this.curveSize);
	}

	@Override
	protected Codec<? extends BiomeSource> getCodec() {
		// TODO Auto-generated method stub
		return BiospheresBiomeSource.CODEC;
	}
}
