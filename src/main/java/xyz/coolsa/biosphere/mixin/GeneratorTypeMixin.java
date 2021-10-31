package xyz.coolsa.biosphere.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.GeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(GeneratorType.class)
public interface GeneratorTypeMixin {
    @Accessor("VALUES")
    public static List<GeneratorType> getValues() {
        throw new AssertionError();
    }

}
