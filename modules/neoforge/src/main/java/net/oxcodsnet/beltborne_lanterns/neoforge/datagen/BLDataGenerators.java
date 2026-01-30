package net.oxcodsnet.beltborne_lanterns.neoforge.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.oxcodsnet.beltborne_lanterns.BLMod;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = BLMod.MOD_ID)
public class BLDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Language providers (client)
        generator.addProvider(event.includeClient(), new BLLanguageProvider(output, "en_us"));
        generator.addProvider(event.includeClient(), new BLLanguageProvider(output, "ru_ru"));
        generator.addProvider(event.includeClient(), new BLLanguageProvider(output, "es_es"));
        generator.addProvider(event.includeClient(), new BLLanguageProvider(output, "fr_fr"));
        generator.addProvider(event.includeClient(), new BLLanguageProvider(output, "de_de"));
        generator.addProvider(event.includeClient(), new BLLanguageProvider(output, "zh_cn"));
        generator.addProvider(event.includeClient(), new BLLanguageProvider(output, "uk_ua"));

        // Tag providers (server)
        BlockTagsProvider blockTagsProvider = new BlockTagsProvider(output, lookupProvider, BLMod.MOD_ID, event.getExistingFileHelper()) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                // No block tags needed
            }
        };
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), new BLItemTagProvider(
                output, lookupProvider, blockTagsProvider.contentsGetter(), event.getExistingFileHelper()
        ));
    }
}
