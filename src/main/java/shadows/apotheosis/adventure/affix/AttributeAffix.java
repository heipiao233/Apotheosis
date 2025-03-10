package shadows.apotheosis.adventure.affix;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.apotheosis.adventure.AdventureModule;
import shadows.apotheosis.adventure.loot.LootCategory;
import shadows.apotheosis.adventure.loot.LootRarity;
import shadows.placebo.json.JsonUtil;
import shadows.placebo.util.StepFunction;

/**
 * Helper class for affixes that modify attributes, as the apply method is the same for most of those.
 */
public class AttributeAffix extends Affix {

	protected final Map<LootRarity, ModifierInst> modifiers;
	protected final Set<LootCategory> types;
	protected final Set<EquipmentSlot> armorTypes;

	public AttributeAffix(Attribute attr, Operation op, Map<LootRarity, StepFunction> values, Set<LootCategory> types, Set<EquipmentSlot> armorTypes) {
		super(AffixType.STAT);
		this.modifiers = values.entrySet().stream().map(entry -> Pair.of(entry.getKey(), new ModifierInst(attr, op, entry.getValue(), new HashMap<>()))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		this.types = types;
		this.armorTypes = armorTypes;
	}

	@Override
	public void addInformation(ItemStack stack, LootRarity rarity, float level, Consumer<Component> list) {
	};

	@Override
	public void addModifiers(ItemStack stack, LootRarity rarity, float level, EquipmentSlot type, BiConsumer<Attribute, AttributeModifier> map) {
		LootCategory cat = LootCategory.forItem(stack);
		if (cat == LootCategory.NONE) {
			AdventureModule.LOGGER.debug("Attempted to apply the attributes of affix {} on item {}, but it is not an affix-compatible item!", this.getId(), stack.getHoverName().getString());
			return;
		}
		ModifierInst modif = this.modifiers.get(rarity);
		if (modif.attr == null) {
			AdventureModule.LOGGER.debug("The affix {} has attempted to apply a null attribute modifier to {}!", this.getId(), stack.getHoverName().getString());
			return;
		}
		for (EquipmentSlot slot : cat.getSlots(stack)) {
			if (slot == type) {
				map.accept(modif.attr, modif.build(slot, this.getId(), level));
			}
		}
	}

	@Override
	public boolean canApplyTo(ItemStack stack, LootRarity rarity) {
		LootCategory cat = LootCategory.forItem(stack);
		if (cat == LootCategory.NONE) return false;
		return (this.types.isEmpty() || this.types.contains(cat)) && (cat != LootCategory.ARMOR || this.armorTypes.isEmpty() || this.armorTypes.contains(((ArmorItem) stack.getItem()).getSlot())) && this.modifiers.containsKey(rarity);
	};

	public record ModifierInst(Attribute attr, Operation op, StepFunction valueFactory, Map<EquipmentSlot, UUID> cache) {

		public AttributeModifier build(EquipmentSlot slot, ResourceLocation id, float level) {
			return new AttributeModifier(this.cache.computeIfAbsent(slot, k -> UUID.randomUUID()), "affix:" + id, this.valueFactory.get(level), this.op);
		}
	}

	public static AttributeAffix read(JsonObject obj) {
		Attribute attr = JsonUtil.getRegistryObject(obj, "attribute", ForgeRegistries.ATTRIBUTES);
		Operation op = Operation.valueOf(GsonHelper.getAsString(obj, "operation"));
		var values = AffixHelper.readValues(GsonHelper.getAsJsonObject(obj, "values"));
		var types = AffixHelper.readTypes(GsonHelper.getAsJsonArray(obj, "types"));
		Set<EquipmentSlot> armorTypes = GSON.fromJson(GsonHelper.getAsJsonArray(obj, "armor_types", new JsonArray()), new TypeToken<Set<EquipmentSlot>>() {
		}.getType());
		return new AttributeAffix(attr, op, values, types, armorTypes);
	}

	public JsonObject write() {
		return new JsonObject();
	}

	public void write(FriendlyByteBuf buf) {
		ModifierInst inst = this.modifiers.values().stream().findFirst().get();
		buf.writeRegistryId(inst.attr);
		buf.writeEnum(inst.op);
		buf.writeMap(this.modifiers, (b, key) -> b.writeUtf(key.id()), (b, modif) -> modif.valueFactory.write(b));
		buf.writeByte(this.types.size());
		this.types.forEach(c -> buf.writeEnum(c));
		buf.writeByte(this.armorTypes.size());
		this.armorTypes.forEach(c -> buf.writeEnum(c));
	}

	public static AttributeAffix read(FriendlyByteBuf buf) {
		Attribute attr = buf.readRegistryIdSafe(Attribute.class);
		Operation op = buf.readEnum(Operation.class);
		Map<LootRarity, StepFunction> values = buf.readMap(b -> LootRarity.byId(b.readUtf()), b -> StepFunction.read(b));
		Set<LootCategory> types = new HashSet<>();
		Set<EquipmentSlot> armorTypes = new HashSet<>();
		int size = buf.readByte();
		for (int i = 0; i < size; i++) {
			types.add(buf.readEnum(LootCategory.class));
		}
		size = buf.readByte();
		for (int i = 0; i < size; i++) {
			armorTypes.add(buf.readEnum(EquipmentSlot.class));
		}
		return new AttributeAffix(attr, op, values, types, armorTypes);
	}

}