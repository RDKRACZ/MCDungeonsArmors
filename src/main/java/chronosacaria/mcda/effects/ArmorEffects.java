package chronosacaria.mcda.effects;

import chronosacaria.mcda.items.ArmorSets;
import chronosacaria.mcda.registry.ArmorsRegistry;
import chronosacaria.mcda.registry.StatusEffectsRegistry;
import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.*;

import static chronosacaria.mcda.config.McdaConfig.config;
import static chronosacaria.mcda.effects.ArmorEffectID.*;

public class ArmorEffects {

    public static final List<StatusEffect> TITAN_SHROUD_STATUS_EFFECTS_LIST =
            List.of(StatusEffects.HUNGER, StatusEffects.NAUSEA, StatusEffects.BLINDNESS,
                    StatusEffects.MINING_FATIGUE, StatusEffects.SLOWNESS,
                    StatusEffects.UNLUCK, StatusEffects.WEAKNESS);

    public static final List<ItemStack> CAULDRONS_OVERFLOW_LIST =
            List.of(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.STRENGTH),
                    PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS),
                    PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.INVISIBILITY));

    // Effects for LivingEntityMixin
    public static void endermanLikeTeleportEffect(LivingEntity livingEntity) {
        World world = livingEntity.getEntityWorld();

        if (!world.isClient) {
            double xpos = livingEntity.getX();
            double ypos = livingEntity.getY();
            double zpos = livingEntity.getZ();

            for (int i = 0; i < 16; i++) {
                double teleportX = livingEntity.getX() + (livingEntity.getRandom().nextDouble() - 0.5D) * 16.0D;
                double teleportY =
                        MathHelper.clamp(livingEntity.getY() + (double) (livingEntity.getRandom().nextInt(16) - 8),
                                0.0D, world.getHeight() - 1);
                double teleportZ = livingEntity.getZ() + (livingEntity.getRandom().nextDouble() - 0.5D) * 16.0D;
                if (livingEntity.hasVehicle()) {
                    livingEntity.stopRiding();
                }

                if (livingEntity.teleport(teleportX, teleportY, teleportZ, true)) {
                    SoundEvent soundEvent = livingEntity instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT :
                            SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
                    livingEntity.world.playSound(
                            null,
                            xpos,
                            ypos,
                            zpos,
                            soundEvent,
                            SoundCategory.PLAYERS,
                            1.0F,
                            1.0F);
                }
            }
        }
    }

    public static void controlledTeleportEffect(LivingEntity livingEntity) {
        World world = livingEntity.getEntityWorld();
        Vec3d target = raytraceForTeleportation(livingEntity);

        if (!world.isClient && target != null) {
            double xpos = livingEntity.getX();
            double ypos = livingEntity.getY();
            double zpos = livingEntity.getZ();

            for (int i = 0; i < 16; i++) {
                Random random = world.random;
                double teleportX =
                        (target.x - livingEntity.getX()) * random.nextDouble() + livingEntity.getX() - 0.5;
                double teleportY =
                        (target.y - livingEntity.getY()) * random.nextDouble() + livingEntity.getY() + 1;
                double teleportZ =
                        (target.z - livingEntity.getZ()) * random.nextDouble() + livingEntity.getZ() - 0.5;
                if (livingEntity.hasVehicle()) {
                    livingEntity.stopRiding();
                }

                if (livingEntity.teleport(teleportX, teleportY, teleportZ, true)) {
                    SoundEvent soundEvent = livingEntity instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT :
                            SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
                    livingEntity.world.playSound(
                            null,
                            xpos,
                            ypos,
                            zpos,
                            soundEvent,
                            SoundCategory.PLAYERS,
                            1.0F,
                            1.0F);
                }
            }
        }
    }

    public static Vec3d raytraceForTeleportation(LivingEntity livingEntity) {
        World world = livingEntity.getEntityWorld();
        Vec3d eyeVec = livingEntity.getCameraPosVec(0f);
        Vec3d direction = livingEntity.getRotationVec(0f);
        Vec3d rayEnd = eyeVec.add(direction.x * 16, direction.y * 16, direction.z * 16);
        BlockHitResult result = world.raycast(new RaycastContext(eyeVec, rayEnd, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, livingEntity));

        BlockPos teleportPos = result.getBlockPos().down(2);

        boolean positionIsFree = positionIsClear(world, teleportPos);

        if (!world.isClient && !result.isInsideBlock()) {

            while(!positionIsFree) {
                teleportPos = teleportPos.down();
                positionIsFree = positionIsClear(world, teleportPos) && world.raycast(new RaycastContext(eyeVec,
                       Vec3d.ofCenter(teleportPos.up()),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, livingEntity)).getType() == HitResult.Type.MISS;
                if (teleportPos.getY() <= 0){
                    break;
                }
            }
        } else if (positionIsFree) {
            Vec3d.ofCenter(teleportPos);
        }
        return Vec3d.ofCenter(teleportPos);
    }

    private static boolean positionIsClear(World world, BlockPos pos) {
        return (world.isAir(pos) || world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && (world.isAir(pos.up()) || world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()));
    }

    public static void applyFluidFreezing(PlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(FLUID_FREEZING))
            return;
        World world = playerEntity.getEntityWorld();
        BlockPos blockPos = playerEntity.getBlockPos();

        if (playerEntity.isAlive() && world.getTime() % 10 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST).get(EquipmentSlot.FEET).asItem()) {
                // From FrostWalkerEnchantment
                if (playerEntity.isOnGround()) {
                    BlockState blockState = Blocks.FROSTED_ICE.getDefaultState();
                    float f = (float) Math.min(16, 2 + 1);
                    BlockPos.Mutable mutable = new BlockPos.Mutable();
                    Iterator var7 = BlockPos.iterate(blockPos.add(-f, -1.0D, -f), blockPos.add(f, -1.0D, f)).iterator();

                    while (var7.hasNext()) {
                        BlockPos blockPos2 = (BlockPos) var7.next();
                        if (blockPos2.isWithinDistance(playerEntity.getPos(), f)) {
                            mutable.set(blockPos2.getX(), blockPos2.getY() + 1, blockPos2.getZ());
                            BlockState blockState2 = world.getBlockState(mutable);
                            if (blockState2.isAir()) {
                                BlockState blockState3 = world.getBlockState(blockPos2);
                                if (blockState3.getMaterial() == Material.WATER && blockState3.get(FluidBlock.LEVEL) == 0 && blockState.canPlaceAt(world, blockPos2) && world.canPlace(blockState, blockPos2, ShapeContext.absent())) {
                                    world.setBlockState(blockPos2, blockState);
                                    world.getBlockTickScheduler().schedule(blockPos2, Blocks.FROSTED_ICE, MathHelper.nextInt(playerEntity.getRandom(), 60, 120));
                                }
                            }
                        }
                    }
                }
                if (playerEntity.isOnGround()) {
                    BlockState blockState = Blocks.CRYING_OBSIDIAN.getDefaultState();
                    float f = (float) Math.min(16, 2 + 1);
                    BlockPos.Mutable mutable = new BlockPos.Mutable();
                    Iterator var7 = BlockPos.iterate(blockPos.add(-f, -1.0D, -f), blockPos.add(f, -1.0D, f)).iterator();

                    while (var7.hasNext()) {
                        BlockPos blockPos2 = (BlockPos) var7.next();
                        if (blockPos2.isWithinDistance(playerEntity.getPos(), f)) {
                            mutable.set(blockPos2.getX(), blockPos2.getY() + 1, blockPos2.getZ());
                            BlockState blockState2 = world.getBlockState(mutable);
                            if (blockState2.isAir()) {
                                BlockState blockState3 = world.getBlockState(blockPos2);
                                if (blockState3.getMaterial() == Material.LAVA && blockState3.get(FluidBlock.LEVEL) == 0 && blockState.canPlaceAt(world, blockPos2) && world.canPlace(blockState, blockPos2, ShapeContext.absent())) {
                                    world.setBlockState(blockPos2, blockState);
                                    world.getBlockTickScheduler().schedule(blockPos2, Blocks.CRYING_OBSIDIAN,
                                            MathHelper.nextInt(playerEntity.getRandom(), 60, 120));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void applyThiefInvisibilityTick(PlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(INVISIBILITY))
            return;
        if (playerEntity.isAlive()){
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.FEET).asItem())
                playerEntity.setInvisible(playerEntity.isSneaking());
        }
    }

    public static void applyWithered(PlayerEntity playerEntity, LivingEntity attacker){
        if (!config.enableArmorEffect.get(WITHERED))
            return;
        if (attacker != null) {
            if (playerEntity.isAlive()) {
                ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
                ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
                ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
                ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

                if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.WITHER).get(EquipmentSlot.HEAD).asItem()
                        && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.WITHER).get(EquipmentSlot.CHEST).asItem()
                        && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.WITHER).get(EquipmentSlot.LEGS).asItem()
                        && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.WITHER).get(EquipmentSlot.FEET).asItem()) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 0));
                }
            }
        }
    }

    public static void applyNimbleTurtleEffects(PlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(NIMBLE_TURTLE_EFFECTS))
            return;
        if (playerEntity.isAlive()) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.NIMBLE_TURTLE).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.NIMBLE_TURTLE).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.NIMBLE_TURTLE).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.NIMBLE_TURTLE).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance resistance = new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 1, false,
                        false);
                StatusEffectInstance healing = new StatusEffectInstance(StatusEffects.REGENERATION, 60, 1, false,
                        false);
                playerEntity.addStatusEffect(resistance);
                playerEntity.addStatusEffect(healing);
            }
        }
    }

    public static void applyTitanShroudStatuses(PlayerEntity playerEntity, LivingEntity target){
        if (!config.enableArmorEffect.get(TITAN_SHROUD_EFFECTS))
            return;
        StatusEffect titanStatusEffect =
                TITAN_SHROUD_STATUS_EFFECTS_LIST.get(playerEntity.getRandom().nextInt(TITAN_SHROUD_STATUS_EFFECTS_LIST.size()));
        target.addStatusEffect(new StatusEffectInstance(titanStatusEffect, 60, 0));
    }

    public static void applyFrostBiteStatus(PlayerEntity playerEntity, LivingEntity target){
        if (!config.enableArmorEffect.get(FROST_BITE_EFFECT))
            return;
        ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

        if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.HEAD).asItem()
                && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.CHEST).asItem()
                && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.LEGS).asItem()
                && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.FEET).asItem()) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffectsRegistry.FREEZING, 60, 0, true, true,
                    false));
        }
    }

    public static void applyGourdiansHatredStatus(PlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(GOURDIANS_HATRED))
            return;
        if (playerEntity.isAlive()) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GOURDIAN).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GOURDIAN).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GOURDIAN).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GOURDIAN).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance strength = new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1);
                playerEntity.addStatusEffect(strength);
            }
        }
    }

    public static void applyCauldronsOverflow(LivingEntity targetedEntity){
        if (!config.enableArmorEffect.get(CAULDRONS_OVERFLOW))
            return;
        ItemStack potionToDrop =
                CAULDRONS_OVERFLOW_LIST.get(targetedEntity.getRandom().nextInt(CAULDRONS_OVERFLOW_LIST.size()));

        ItemEntity potion = new ItemEntity(targetedEntity.world, targetedEntity.getX(), targetedEntity.getY(),
                targetedEntity.getZ(), potionToDrop);
        targetedEntity.world.spawnEntity(potion);
    }

    // Effects for ServerPlayerEntityMixin
    public static void applyHaste(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(HASTE))
            return;
        World world = playerEntity.getEntityWorld();

        if (playerEntity.getY() < 32.0F && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.CAVE_CRAWLER).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.CAVE_CRAWLER).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.CAVE_CRAWLER).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.CAVE_CRAWLER).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance haste = new StatusEffectInstance(StatusEffects.HASTE, 42, 0, false, false);
                playerEntity.addStatusEffect(haste);
            }
        }
        if (playerEntity.getY() > 100.0F && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HIGHLAND).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HIGHLAND).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HIGHLAND).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HIGHLAND).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance haste = new StatusEffectInstance(StatusEffects.HASTE, 42, 0, false, false);
                playerEntity.addStatusEffect(haste);
            }
        }
    }

    public static void applyHeroOfTheVillage(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(HERO_OF_THE_VILLAGE))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HERO).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HERO).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HERO).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HERO).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance heroOfTheVillage = new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 42, 0, false,
                        false);
                playerEntity.addStatusEffect(heroOfTheVillage);
            }

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GILDED).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GILDED).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GILDED).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GILDED).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance heroOfTheVillage = new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 42, 0, false,
                        false);
                playerEntity.addStatusEffect(heroOfTheVillage);
            }
        }
    }

    public static void applyHunger(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(HUNGER))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HUNGRY_HORROR).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HUNGRY_HORROR).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HUNGRY_HORROR).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.HUNGRY_HORROR).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance hunger = new StatusEffectInstance(StatusEffects.HUNGER, 42, 1, false,
                        false);
                playerEntity.addStatusEffect(hunger);
            }
        }
    }

    public static void applyFireResistance(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(FIRE_RESISTANCE))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SPROUT).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SPROUT).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SPROUT).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SPROUT).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance fireResistance = new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 42, 1,
                        false,false);
                playerEntity.addStatusEffect(fireResistance);
            }

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.LIVING_VINES).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.LIVING_VINES).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.LIVING_VINES).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.LIVING_VINES).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance fireResistance = new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 42, 1,
                        false,false);
                playerEntity.addStatusEffect(fireResistance);
            }
        }
    }

    public static void applyLuck(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(LUCK))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.OPULENT).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.OPULENT).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.OPULENT).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.OPULENT).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance luck = new StatusEffectInstance(StatusEffects.LUCK, 42, 0, false,
                        false);
                playerEntity.addStatusEffect(luck);
            }
        }
    }

    public static void applySprintingSpeed(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(SPRINTING))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SHADOW_WALKER).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SHADOW_WALKER).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SHADOW_WALKER).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.SHADOW_WALKER).get(EquipmentSlot.FEET).asItem()) {
                if (playerEntity.isSprinting()) {
                    StatusEffectInstance speed = new StatusEffectInstance(StatusEffects.SPEED, 42, 0, false,
                            false);
                    playerEntity.addStatusEffect(speed);
                }
            }
        }
    }

    public static void applyWaterBreathing(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(WATER_BREATHING))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GLOW_SQUID).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GLOW_SQUID).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GLOW_SQUID).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.GLOW_SQUID).get(EquipmentSlot.FEET).asItem()) {
                if (playerEntity.isSubmergedInWater()) {
                    StatusEffectInstance waterBreathing = new StatusEffectInstance(StatusEffects.WATER_BREATHING, 42, 0,
                            false, false);
                    playerEntity.addStatusEffect(waterBreathing);
                }
            }
        }
    }

    public static void applyInvisibility(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(INVISIBILITY))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.THIEF).get(EquipmentSlot.FEET).asItem()) {
                if (playerEntity.isSneaking()) {
                    StatusEffectInstance invisibility = new StatusEffectInstance(StatusEffects.INVISIBILITY, 42, 0,
                            false, false);
                    playerEntity.addStatusEffect(invisibility);
                }
            }
        }
    }

    public static void applySlowFalling(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(SLOW_FALLING))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.PHANTOM).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.PHANTOM).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.PHANTOM).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.PHANTOM).get(EquipmentSlot.FEET).asItem()
                    || helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.FROST_BITE).get(EquipmentSlot.FEET).asItem()) {
                StatusEffectInstance slowFalling = new StatusEffectInstance(StatusEffects.SLOW_FALLING, 42, 0, false,
                        false);
                playerEntity.addStatusEffect(slowFalling);
            }
        }
    }

    public static void applyLevitationRemoval(ServerPlayerEntity playerEntity){
        if (!config.enableArmorEffect.get(SHULKER_LIKE))
            return;

        World world = playerEntity.getEntityWorld();

        if (playerEntity.isAlive() && world.getTime() % 30 == 0) {
            ItemStack helmetStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chestStack = playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legsStack = playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feetStack = playerEntity.getEquippedStack(EquipmentSlot.FEET);

            if (helmetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.STURDY_SHULKER).get(EquipmentSlot.HEAD).asItem()
                    && chestStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.STURDY_SHULKER).get(EquipmentSlot.CHEST).asItem()
                    && legsStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.STURDY_SHULKER).get(EquipmentSlot.LEGS).asItem()
                    && feetStack.getItem() == ArmorsRegistry.armorItems.get(ArmorSets.STURDY_SHULKER).get(EquipmentSlot.FEET).asItem()) {
                if (playerEntity.hasStatusEffect(StatusEffects.LEVITATION)) {
                    playerEntity.removeStatusEffect(StatusEffects.LEVITATION);
                }
            }
        }
    }

}
