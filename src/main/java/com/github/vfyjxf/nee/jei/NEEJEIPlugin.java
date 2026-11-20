package com.github.vfyjxf.nee.jei;

import appeng.container.me.items.CraftingTermContainer;
import appeng.container.me.items.PatternTermContainer;
import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.utils.WrappedTable;
import com.google.common.collect.Table.Cell;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.collect.Table;
import mezz.jei.config.Constants;
import mezz.jei.load.registration.RecipeTransferRegistration;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;


@JeiPlugin
public class NEEJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(NotEnoughEnergistics.MODID, "appeng");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        RecipeTransferRegistration recipeTransferRegistration = (RecipeTransferRegistration) registration;
        Table<Class<?>, ResourceLocation, IRecipeTransferHandler<?>> newRegistry = Table.hashBasedTable();
        boolean patternHandlerFound = false;
        boolean craftingHandlerFound = false;
        boolean wirelessCraftingRegistered = false;
        
        for (final Cell<Class<?>, ResourceLocation, IRecipeTransferHandler<?>> currentCell : recipeTransferRegistration.getRecipeTransferHandlers().cellSet()) {
            if (PatternTermContainer.class.equals(currentCell.getRowKey())) {
                patternHandlerFound = true;
                continue;
            }

            if (CraftingTermContainer.class.equals(currentCell.getRowKey())) {
                craftingHandlerFound = true;
                continue;
            }
            newRegistry.put(currentCell.getRowKey(), currentCell.getColumnKey(), currentCell.getValue());
        }

        newRegistry.put(PatternTermContainer.class, Constants.UNIVERSAL_RECIPE_TRANSFER_UID, new PatternRecipeTransferHandler());
        newRegistry.put(CraftingTermContainer.class, VanillaRecipeCategoryUid.CRAFTING, new CraftingHelperTransferHandler(registration.getTransferHelper()));
        
        // ✅ Wireless Crafting Terminal support — only registers if AE2WTLib is present
        try {
            Class<?> wirelessContainer = Class.forName("tfar.ae2wt.wirelesscraftingterminal.WirelessCraftingTerminalContainer");
            Class<?> recipeCategoryUid = Class.forName("mezz.jei.api.constants.VanillaRecipeCategoryUid");
            var craftingField = recipeCategoryUid.getField("CRAFTING");
            Object craftingUid = craftingField.get(null);
            newRegistry.put(wirelessContainer.class, (ResourceLocation) craftingUid, new WirelessCraftingHelperTransferHandler(registration.getTransferHelper()));
            wirelessCraftingRegistered = true;
            NotEnoughEnergistics.logger.info("AE2WTLib Wireless Crafting Terminal support registered successfully");
        } catch (ClassNotFoundException e) {
            // AE2WTLib not installed — skip wireless support silently
        } catch (Exception e) {
            NotEnoughEnergistics.logger.warn("Failed to register AE2WTLib Wireless Crafting Terminal support: " + e.getMessage());
        }
        
        if (patternHandlerFound && craftingHandlerFound) {
            NotEnoughEnergistics.logger.info("AE2 RecipeTransferHandler Replaced Successfully (Registered prior)");
        } else if (wirelessCraftingRegistered) {
            newRegistry = new WrappedTable<>(newRegistry, registration.getTransferHelper());
        } else {
            newRegistry = new WrappedTable<>(newRegistry, registration.getTransferHelper());
        }

        ObfuscationReflectionHelper.setPrivateValue(RecipeTransferRegistration.class, recipeTransferRegistration, newRegistry, "recipeTransferHandlers");
    }

}
