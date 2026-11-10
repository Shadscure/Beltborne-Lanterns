package net.oxcodsnet.beltborne_lanterns.neoforge.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.oxcodsnet.beltborne_lanterns.BLMod;

import java.util.concurrent.CompletableFuture;

public class BLBlockTagProvider extends BlockTagsProvider {

    public BLBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BLMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // No block tags needed
    }
}
