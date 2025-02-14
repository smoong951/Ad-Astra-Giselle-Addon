package ad_astra_giselle_addon.client.compat.jei;

import java.util.List;

import ad_astra_giselle_addon.client.compat.RecipeHelper;
import ad_astra_giselle_addon.client.screen.FuelLoaderScreen;
import ad_astra_giselle_addon.common.config.MachinesConfig;
import ad_astra_giselle_addon.common.registry.AddonBlocks;
import ad_astra_giselle_addon.common.registry.ObjectRegistry;
import earth.terrarium.adastra.client.utils.GuiUtils;
import earth.terrarium.botarium.common.fluid.FluidConstants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class FuelLoaderCategory extends AddonRecipeCategory<Fluid>
{
	private IDrawable background;
	private IDrawable fluidOverlay;

	public FuelLoaderCategory(RecipeType<Fluid> recipeType)
	{
		super(recipeType);
	}

	@Override
	public void createGui(IGuiHelper guiHelper)
	{
		super.createGui(guiHelper);
		this.background = guiHelper.createDrawable(RecipeHelper.FuelLoader.BACKGROUND_LOCATION, 0, 0, RecipeHelper.FuelLoader.BACKGROUND_WIDTH, RecipeHelper.FuelLoader.BACKGROUND_HEIGHT);
		this.fluidOverlay = guiHelper.drawableBuilder(GuiUtils.FLUID_BAR, 0, 0, RecipeHelper.FuelLoader.TANK_WIDTH, RecipeHelper.FuelLoader.TANK_HEIGHT).setTextureSize(GuiUtils.FLUID_BAR_WIDTH, GuiUtils.FLUID_BAR_HEIGHT).build();
	}

	@Override
	public IDrawable getBackground()
	{
		return this.background;
	}

	public IDrawable getFluidOverlay()
	{
		return this.fluidOverlay;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Fluid recipe, IFocusGroup focuses)
	{
		super.setRecipe(builder, recipe, focuses);

		long capacity = FluidConstants.fromMillibuckets(MachinesConfig.FUEL_LOADER_FLUID_CAPACITY);
		builder.addSlot(RecipeIngredientRole.INPUT, RecipeHelper.FuelLoader.TANK_LEFT, RecipeHelper.FuelLoader.TANK_TOP) //
				.addFluidStack(recipe, capacity) //
				.setFluidRenderer(capacity, false, RecipeHelper.FuelLoader.TANK_WIDTH, RecipeHelper.FuelLoader.TANK_HEIGHT) //
				.setOverlay(this.fluidOverlay, 0, 0);
	}

	@Override
	public List<ItemStack> getRecipeCatalystItemStacks()
	{
		List<ItemStack> list = super.getRecipeCatalystItemStacks();
		list.add(new ItemStack(AddonBlocks.FUEL_LOADER.get()));
		return list;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registry)
	{
		super.registerRecipes(registry);

		registry.addRecipes(this.getRecipeType(), ObjectRegistry.get(Registries.FLUID).stream().filter(RecipeHelper.FuelLoader::testFluid).toList());
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registry)
	{
		super.registerGuiHandlers(registry);

		registry.addGuiContainerHandler(FuelLoaderScreen.class, new FuelLoaderGuiContainerHandler(registry.getJeiHelpers()));
	}

}
