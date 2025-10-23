package net.oxcodsnet.beltborne_lanterns.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;

import java.util.concurrent.CompletableFuture;

public class BLLampTagProvider extends FabricTagProvider.ItemTagProvider {
    public BLLampTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Fabric API 0.128+ uses getTagBuilder; older used getOrCreateTagBuilder
        // Try to call getTagBuilder; if not available in this environment, the method will still be resolved at compile time per dependency.
        this.getOrCreateRawBuilder(LampRegistry.EXTRA_LAMPS_TAG)
                .addElement(BuiltInRegistries.ITEM.getKey(Items.LANTERN))
                .addElement(BuiltInRegistries.ITEM.getKey(Items.SOUL_LANTERN));
    }
}
