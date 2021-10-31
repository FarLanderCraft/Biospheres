package xyz.coolsa.biosphere;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.heightprovider.UniformHeightProvider;
import xyz.coolsa.biosphere.mixin.GeneratorTypeMixin;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class Biospheres implements ModInitializer {
	public static BiosphereConfig bsconfig = new BiosphereConfig(48, 5, 12);
	public Gson daData = new GsonBuilder().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
	Path configPath = Paths.get("config/biospheres.json");

	//public static ConfiguredFeature<?, ?> ORE_IRON_BIOSPHERE = Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.IRON_ORE.getDefaultState(), 9)).range(new RangeDecoratorConfig(UniformHeightProvider.create(YOffset.aboveBottom(0), YOffset.fixed(192)))).spreadHorizontally().repeat(35);
	//public static RegistryKey<ConfiguredFeature<?, ?>> oreIronBiosphere = RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY, new Identifier("biosphere", "ore_iron_biosphere"));
	public void saveDaData() {
		try{
			if (configPath.toFile().exists()) {
				bsconfig = daData.fromJson(new String(Files.readAllBytes(configPath)), BiosphereConfig.class);
			} else {
				Files.write(configPath, Collections.singleton(daData.toJson(bsconfig)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onInitialize() {
		saveDaData();
		Registry.register(Registry.CHUNK_GENERATOR, new Identifier("coolsa","biosphere"), BiospheresChunkGenerator.CODEC);
		Registry.register(Registry.CHUNK_GENERATOR, new Identifier("coolsa","naturalbiosphere"), OverworldBiosphereGen.CODEC);
		Registry.register(Registry.BIOME_SOURCE, new Identifier("coolsa","biosphere_biomes"), BiospheresBiomeSource.CODEC);

		//Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, oreIronBiosphere.getValue(), ORE_IRON_BIOSPHERE);
		System.out.println("Loaded Biospheres Mod!");
	}
}
