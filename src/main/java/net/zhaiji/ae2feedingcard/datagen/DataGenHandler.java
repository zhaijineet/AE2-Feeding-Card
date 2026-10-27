package net.zhaiji.ae2feedingcard.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public class DataGenHandler {
    public static void handlerGatherDataEvent(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new AE2FeedingCardRecipeProvider(packOutput, lookupProvider));

        generator.addProvider(event.includeClient(), new AE2FeedingCardItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new AE2FeedingCardLanguageProvider(packOutput, AE2FeedingCardLanguageProvider.EN_US));
        generator.addProvider(event.includeClient(), new AE2FeedingCardLanguageProvider(packOutput, AE2FeedingCardLanguageProvider.ZH_CN));
    }
}
