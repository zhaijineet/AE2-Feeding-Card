package net.zhaiji.ae2feedingcard.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;

public class AE2FeedingCardItemModelProvider extends ItemModelProvider {
    public AE2FeedingCardItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, AE2FeedingCard.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent("feeding_card", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/feeding_card"));
    }
}
