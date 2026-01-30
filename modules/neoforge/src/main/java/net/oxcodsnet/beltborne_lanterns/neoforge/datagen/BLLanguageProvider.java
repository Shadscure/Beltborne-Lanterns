package net.oxcodsnet.beltborne_lanterns.neoforge.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.datagen.BLLanguage;

public class BLLanguageProvider extends LanguageProvider {
    private final String code;

    public BLLanguageProvider(PackOutput output, String code) {
        super(output, BLMod.MOD_ID, code);
        this.code = code;
    }

    @Override
    protected void addTranslations() {
        BLLanguage.fill(this.code, this::add);
    }
}
