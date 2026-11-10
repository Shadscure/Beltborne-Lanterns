package net.oxcodsnet.beltborne_lanterns.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;

public class BLDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) ->
                        new BLLanguageProvider(output, registries, "en_us")
        );
        pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) ->
                        new BLLanguageProvider(output, registries, "ru_ru")
        );
        pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) ->
                        new BLLanguageProvider(output, registries, "es_es")
        );
        pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) ->
                        new BLLanguageProvider(output, registries, "fr_fr")
        );
        pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) ->
                        new BLLanguageProvider(output, registries, "de_de")
        );
        pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) ->
                        new BLLanguageProvider(output, registries, "zh_cn")
        );
        pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) ->
                        new BLLanguageProvider(output, registries, "uk_ua")
        );
        pack.addProvider(BLLampTagProvider::new);
    }
}
