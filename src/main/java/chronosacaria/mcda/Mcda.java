package chronosacaria.mcda;

import chronosacaria.mcda.items.ArmorSets;
import chronosacaria.mcda.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Random;

public class Mcda implements ModInitializer {
    public static final String MOD_ID = "mcda";

    public static final Random random = new Random();

    public static Identifier ID(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static final ItemGroup ARMORS_GROUP = FabricItemGroupBuilder.build(ID( "armor"),
            () -> new ItemStack(ArmorsRegistry.armorItems.get(ArmorSets.SPLENDID).get(EquipmentSlot.CHEST)));

    @Override
    public void onInitialize() {
        ArmorsRegistry.init();
        EnchantsRegistry.init();
        ItemsRegistry.init();
        LootRegistry.init();
        StatusEffectsRegistry.init();
        TradesRegistry.registerVillagerOffers();
        TradesRegistry.registerWanderingTrades();
    }
}
