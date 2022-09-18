package owmii.powah.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import owmii.powah.Powah;
import owmii.powah.api.PowahAPI;
import owmii.powah.block.Blcks;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SolidCoolantCategory implements IRecipeCategory<SolidCoolantCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create(Powah.MOD_ID, "solid_coolant", Recipe.class);

    public static final ResourceLocation GUI_BACK = new ResourceLocation(Powah.MOD_ID, "textures/gui/jei/misc.png");
    private final IDrawable background;
    private final IDrawable icon;

    public SolidCoolantCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(GUI_BACK, 0, 0, 160, 24).addPadding(1, 0, 0, 0).build();
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blcks.DRY_ICE.get()));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.powah.jei.category.solid.coolant");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 5)
                .addItemStack(recipe.stack);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack matrix, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(matrix, I18n.get("info.lollipop.amount") + ": " + I18n.get("info.lollipop.mb", recipe.amount), 30.0F, 3.0F, 0x444444);
        minecraft.font.draw(matrix, I18n.get("info.lollipop.temperature") + ": " + I18n.get("info.lollipop.temperature.c", "" + ChatFormatting.DARK_AQUA + recipe.coldness), 30.0F, 15.0F, 0x444444);
    }

    public static class Maker {
        public static List<Recipe> getBucketRecipes(IIngredientManager ingredientManager) {
            Collection<ItemStack> allItemStacks = ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK);
            List<Recipe> recipes = new ArrayList<>();
            allItemStacks.forEach(stack -> {
                var id = Registry.ITEM.getKey(stack.getItem());
                if (PowahAPI.SOLID_COOLANTS.containsKey(id)) {
                    Pair<Integer, Integer> pr = PowahAPI.getSolidCoolant(stack.getItem());
                    recipes.add(new Recipe(stack, pr.getLeft(), pr.getRight()));
                }
            });
            return recipes;
        }
    }

    public static class Recipe {
        private final ItemStack stack;
        private final int amount;
        private final int coldness;

        public Recipe(ItemStack stack, int amount, int coldness) {
            this.stack = stack;
            this.amount = amount;
            this.coldness = coldness;
        }

        public ItemStack getStack() {
            return this.stack;
        }

        public int getAmount() {
            return this.amount;
        }

        public int getColdness() {
            return this.coldness;
        }
    }
}