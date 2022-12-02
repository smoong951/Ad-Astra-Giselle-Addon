package ad_astra_giselle_addon.common.item;

import java.util.List;
import java.util.function.BiPredicate;

import ad_astra_giselle_addon.common.config.ItemsConfig;
import ad_astra_giselle_addon.common.content.oxygen.ChargeMode;
import ad_astra_giselle_addon.common.content.oxygen.IChargeMode;
import ad_astra_giselle_addon.common.content.oxygen.IOxygenCharger;
import ad_astra_giselle_addon.common.content.oxygen.IOxygenChargerItem;
import ad_astra_giselle_addon.common.content.oxygen.OxygenChargerUtils;
import ad_astra_giselle_addon.common.fluid.FluidHooks2;
import ad_astra_giselle_addon.common.fluid.FluidPredicates;
import ad_astra_giselle_addon.common.fluid.UniveralFluidHandler;
import ad_astra_giselle_addon.common.util.NBTUtils;
import ad_astra_giselle_addon.common.util.TranslationUtils;
import earth.terrarium.ad_astra.items.FluidContainingItem;
import earth.terrarium.ad_astra.registry.ModFluids;
import earth.terrarium.botarium.api.fluid.FluidHolder;
import earth.terrarium.botarium.api.fluid.FluidHooks;
import earth.terrarium.botarium.api.item.ItemStackHolder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class OxygenCanItem extends Item implements FluidContainingItem, IOxygenChargerItem
{
	public static final String KEY_OXYGEN_CHARGER = "oxygencharger";
	public static final String KEY_CHARGE_MODE = "chargemode";

	public OxygenCanItem(Properties properties)
	{
		super(properties.stacksTo(1));
	}

	@Override
	public long getTankSize()
	{
		return ItemsConfig.OXYGEN_CAN_FLUID_CAPACITY;
	}

	@Override
	public BiPredicate<Integer, FluidHolder> getFilter()
	{
		return FluidPredicates::isOxygen;
	}

	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> list)
	{
		super.fillItemCategory(group, list);

		if (this.allowedIn(group))
		{
			ItemStackHolder full = new ItemStackHolder(new ItemStack(this));
			IOxygenCharger oxygenCharger = OxygenChargerUtils.get(full);
			oxygenCharger.setChargeMode(ChargeMode.ALL);
			oxygenCharger.getFluidHandler().insertFluid(FluidHooks.newFluidHolder(ModFluids.OXYGEN.get(), Integer.MAX_VALUE, null), false);
			list.add(full.getStack());
		}

	}

	@Override
	public Rarity getRarity(ItemStack stack)
	{
		return this.isFoil(stack) ? Rarity.EPIC : super.getRarity(stack);
	}

	@Override
	public boolean isFoil(ItemStack stack)
	{
		IOxygenCharger oxygenCharger = OxygenChargerUtils.get(new ItemStackHolder(stack));

		if (oxygenCharger != null && oxygenCharger.getChargeMode() != ChargeMode.NONE)
		{
			long storedAmount = FluidHooks2.getStoredAmount(oxygenCharger.getFluidHandler());

			if (storedAmount > 0)
			{
				return true;
			}

		}

		return super.isFoil(stack);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack item = player.getItemInHand(hand);

		if (level.isClientSide())
		{
			return InteractionResultHolder.pass(item);
		}

		ItemStackReference holder = new ItemStackReference(item, ItemStackConsumers.hand(hand, player::setItemInHand));
		IOxygenCharger oxygenCharger = OxygenChargerUtils.get(holder);

		if (oxygenCharger != null && !player.isShiftKeyDown())
		{
			IChargeMode chargeMode = oxygenCharger.getChargeMode();
			List<IChargeMode> modes = oxygenCharger.getAvailableChargeModes();
			int nextIndex = (modes.indexOf(chargeMode) + 1) % modes.size();
			IChargeMode nextMode = modes.get(nextIndex);
			oxygenCharger.setChargeMode(nextMode);
			player.sendSystemMessage(TranslationUtils.descriptionChargeMode(nextMode));
		}

		return InteractionResultHolder.pass(item);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(stack, level, tooltip, flag);

		IOxygenCharger oxygenCharger = OxygenChargerUtils.get(new ItemStackHolder(stack));

		if (oxygenCharger != null)
		{
			tooltip.add(TranslationUtils.descriptionChargeMode(oxygenCharger.getChargeMode()));

			UniveralFluidHandler fluidHandler = oxygenCharger.getFluidHandler();

			for (int i = 0; i < fluidHandler.getTankAmount(); i++)
			{
				FluidHolder fluid = fluidHandler.getFluidInTank(i);
				long capacity = fluidHandler.getTankCapacity(i);
				tooltip.add(TranslationUtils.oxygenStorage(fluid.getFluidAmount(), capacity));
			}

		}

	}

	@Override
	public boolean isBarVisible(ItemStack stack)
	{
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack)
	{
		double ratio = FluidHooks2.getStoredRatio(UniveralFluidHandler.from(new ItemStackHolder(stack)));
		return (int) (ratio * 13);
	}

	@Override
	public int getBarColor(ItemStack stack)
	{
		double ratio = FluidHooks2.getStoredRatio(UniveralFluidHandler.from(new ItemStackHolder(stack)));
		return Mth.hsvToRgb((float) (ratio / 3.0F), 1.0F, 1.0F);
	}

	@Override
	public IOxygenCharger getOxygenCharger(ItemStackHolder item)
	{
		return new IOxygenCharger()
		{
			@Override
			public void setChargeMode(IChargeMode mode)
			{
				CompoundTag tag = NBTUtils.getOrCreateTag(item.getStack(), KEY_OXYGEN_CHARGER);
				tag.put(KEY_CHARGE_MODE, IChargeMode.writeNBT(mode));
			}

			@Override
			public IChargeMode getChargeMode()
			{
				CompoundTag tag = NBTUtils.getTag(item.getStack(), KEY_OXYGEN_CHARGER);
				return IChargeMode.find(this.getAvailableChargeModes(), tag.getString(KEY_CHARGE_MODE));
			}

			@Override
			public long getTransferAmount()
			{
				return ItemsConfig.OXYGEN_CAN_FLUID_TRANSFER;
			}

			@Override
			public UniveralFluidHandler getFluidHandler()
			{
				return UniveralFluidHandler.from(item);
			}

		};

	}

}
