package net.oxcodsnet.beltborne_lanterns.neoforge.datagen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.oxcodsnet.beltborne_lanterns.BLMod;

@EventBusSubscriber(modid = BLMod.MOD_ID)
public class BLDataGenerators {

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        // Language providers
        event.createProvider(output -> new BLLanguageProvider(output, "en_us"));
        event.createProvider(output -> new BLLanguageProvider(output, "ru_ru"));
        event.createProvider(output -> new BLLanguageProvider(output, "es_es"));
        event.createProvider(output -> new BLLanguageProvider(output, "fr_fr"));
        event.createProvider(output -> new BLLanguageProvider(output, "de_de"));
        event.createProvider(output -> new BLLanguageProvider(output, "zh_cn"));
        event.createProvider(output -> new BLLanguageProvider(output, "uk_ua"));
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        // Block and item tags
        event.createBlockAndItemTags(
                (output, lookup) -> new BLBlockTagProvider(output, lookup),
                (output, lookup, blockTags) -> new BLItemTagProvider(output, lookup, blockTags)
        );
    }
}
