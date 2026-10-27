package net.zhaiji.ae2feedingcard.datagen;

import appeng.core.definitions.AEItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardItems;

import java.util.concurrent.CompletableFuture;

public class AE2FeedingCardRecipeProvider extends RecipeProvider {
    public AE2FeedingCardRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AE2FeedingCardItems.FEEDING_CARD.get())
                .requires(AEItems.ADVANCED_CARD)
                .requires(Items.BREAD)
                .unlockedBy("has_bread", has(Items.BREAD))
                .save(recipeOutput);
    }
}
