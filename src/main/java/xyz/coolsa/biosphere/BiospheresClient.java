package xyz.coolsa.biosphere;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import xyz.coolsa.biosphere.mixin.GeneratorTypeMixin;

public class BiospheresClient implements ClientModInitializer {

    @Environment(EnvType.CLIENT)
    public static final GeneratorType BioSphere = new GeneratorType("biosphere") {
        @Override
        public ChunkGenerator getChunkGenerator(DynamicRegistryManager registryManager, long seed) {
            return new BiospheresChunkGenerator(new BiospheresBiomeSource(registryManager, seed), seed, Biospheres.bsconfig.sphereRadius * 4, Biospheres.bsconfig.sphereRadius, Biospheres.bsconfig.lakeRadius, Biospheres.bsconfig.shoreRadius);
        }
    };

    /*@Environment(EnvType.CLIENT)
    public static final GeneratorType NaturalBioSphere = new GeneratorType("naturalbiosphere") {
        @Override
        public ChunkGenerator getChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
            return new OverworldBiosphereGen(new VanillaLayeredBiomeSource(seed, false, false, biomeRegistry),
                    seed,
                    () -> { return (ChunkGeneratorSettings)chunkGeneratorSettingsRegistry.getOrThrow(ChunkGeneratorSettings.OVERWORLD);});
        }
    };*/

    @Override
    public void onInitializeClient() {
        GeneratorTypeMixin.getValues().add(BioSphere);
        //GeneratorTypeMixin.getValues().add(NaturalBioSphere);
    }
}
