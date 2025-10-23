package net.oxcodsnet.beltborne_lanterns.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;

import java.util.concurrent.CompletableFuture;

public class BLLampTagProvider extends FabricTagProvider.ItemTagProvider {
    public BLLampTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(LampRegistry.EXTRA_LAMPS_TAG)
                .add(Items.LANTERN)
                .add(Items.SOUL_LANTERN);
    }
}
