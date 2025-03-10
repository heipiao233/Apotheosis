package shadows.apotheosis.adventure.affix.salvaging;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import shadows.apotheosis.adventure.loot.LootRarity;

public class SalvageItem extends Item {

	protected final LootRarity rarity;

	public SalvageItem(LootRarity rarity, Properties pProperties) {
		super(pProperties);
		this.rarity = rarity;
	}

	@Override
	public Component getName(ItemStack pStack) {
		return new TranslatableComponent(this.getDescriptionId(pStack)).withStyle(Style.EMPTY.withColor(rarity.color()));
	}

	@Override
	public void appendHoverText(ItemStack pStack, Level pLevel, List<Component> list, TooltipFlag pIsAdvanced) {
		list.add(new TranslatableComponent("info.apotheosis.rarity_material", rarity.toComponent()).withStyle(ChatFormatting.GRAY));
	}

}
