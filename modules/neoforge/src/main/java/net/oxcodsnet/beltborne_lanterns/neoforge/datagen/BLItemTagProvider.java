package net.oxcodsnet.beltborne_lanterns.neoforge.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;

import java.util.concurrent.CompletableFuture;

public class BLItemTagProvider extends ItemTagsProvider {

    public BLItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags
    ) {
        super(output, lookupProvider, blockTags, BLMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LampRegistry.EXTRA_LAMPS_TAG)
                .add(Items.LANTERN)
                .add(Items.SOUL_LANTERN);
    }
}
