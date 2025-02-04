package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.InventoryPower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ApoliCompat implements PresetModule {
	public static final Identifier ID = new Identifier("switchy", "apoli");

	public static final String KEY_POWER_DATA_LIST = "PowerData";

	public final Map<PowerType<?>, NbtElement> powerNbt = new HashMap<>();

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		powerNbt.clear();
		List<Power> powers = PowerHolderComponent.KEY.get(player).getPowers();
		for (Power power : powers) {
			this.powerNbt.put(power.getType(), power.toTag());
		}
		if (nextPreset != null) {
			clearInventories(PowerHolderComponent.getPowers(player, InventoryPower.class));
		}
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		for (Map.Entry<PowerType<?>, NbtElement> entry : powerNbt.entrySet()) {
			Power power = PowerHolderComponent.KEY.get(player).getPower(entry.getKey());
			if (power != null) {
				power.fromTag(entry.getValue());
			}
		}
	}

	private static void clearInventories(List<InventoryPower> powers) {
		powers.forEach(InventoryPower::clear);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		NbtList powerNbtList = new NbtList();
		for (Map.Entry<PowerType<?>, NbtElement> entry : powerNbt.entrySet()) {
			NbtCompound powerTag = new NbtCompound();
			powerTag.putString("PowerType", entry.getKey().getIdentifier().toString());
			powerTag.put("Data", entry.getValue());
			powerNbtList.add(powerTag);
		}
		outNbt.put(KEY_POWER_DATA_LIST, powerNbtList);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.powerNbt.clear();
		if (nbt.contains(KEY_POWER_DATA_LIST, NbtElement.LIST_TYPE)) {
			NbtList powerDataList = nbt.getList(KEY_POWER_DATA_LIST, NbtElement.COMPOUND_TYPE);
			for (NbtElement dataElement : powerDataList) {
				if (dataElement instanceof NbtCompound dataCompound) {
					String powerId = dataCompound.getString("PowerType");
					NbtElement powerData = dataCompound.get("Data");
					try {
						PowerType<?> powerType = PowerTypeRegistry.get(Identifier.tryParse(powerId));
						this.powerNbt.put(powerType, powerData);
					} catch (IllegalArgumentException e) {
						Switchy.LOGGER.warn("Switchy: Failed to load preset power with id" + powerId);
					}
				}
			}
		}
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		PresetModuleRegistry.registerModule(ID, ApoliCompat::new, true, ModuleImportable.OPERATOR, Set.of(OriginsCompat.ID));
	}
}
