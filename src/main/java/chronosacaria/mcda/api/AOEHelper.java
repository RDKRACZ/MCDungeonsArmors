package chronosacaria.mcda.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.List;

import static chronosacaria.mcda.Mcda.random;

// TODO: unused
public class AOEHelper {

    /** Returns targets of an AOE effect from 'attacker' around 'center'. This includes 'center'. */
    public static List<LivingEntity> getAoeTargets(LivingEntity center, LivingEntity attacker, float distance) {
        return center.getEntityWorld().getEntitiesByClass(LivingEntity.class,
                new Box(center.getBlockPos()).expand(distance),
                (nearbyEntity) -> AbilityHelper.isAoeTarget(nearbyEntity, attacker, center)
        );
    }

    public static void healNearbyAllies(LivingEntity healer, StatusEffectInstance effectInstance, float distance) {
        if (!(healer instanceof PlayerEntity)) return;
        PlayerEntity playerEntity = (PlayerEntity) healer;

        World world = healer.getEntityWorld();
        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
                new Box(healer.getBlockPos()).expand(distance),
                (nearbyEntity) -> nearbyEntity != healer && AbilityHelper.canHealEntity(healer, nearbyEntity));

        for (LivingEntity nearbyEntity : nearbyEntities) {
            if (nearbyEntity == null) return;
            if (nearbyEntity.getHealth() < nearbyEntity.getMaxHealth()) {
                if (effectInstance.getEffectType().isInstant()) {
                    effectInstance.getEffectType().applyInstantEffect(playerEntity, playerEntity, nearbyEntity,
                            effectInstance.getAmplifier(), 1.0D);
                } else {
                    nearbyEntity.addStatusEffect(new StatusEffectInstance(effectInstance));
                }

                addParticles((ServerWorld) world, nearbyEntity, ParticleTypes.HEART);
            }
        }
    }

    public static void healNearbyAllies(LivingEntity healer, float amount, float distance) {
        if (!(healer instanceof PlayerEntity)) return;

        World world = healer.getEntityWorld();
        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
                new Box(healer.getBlockPos()).expand(distance),
                (nearbyEntity) -> nearbyEntity != healer && AbilityHelper.canHealEntity(healer, nearbyEntity));

        for (LivingEntity nearbyEntity : nearbyEntities) {
            if (nearbyEntity == null) return;
            if (nearbyEntity.getHealth() < nearbyEntity.getMaxHealth()) {
                nearbyEntity.heal(amount);

                addParticles((ServerWorld) world, nearbyEntity, ParticleTypes.HEART);
            }
        }
    }

    public static void burnNearbyEnemies(LivingEntity attacker, float damage, float distance) {
        World world = attacker.getEntityWorld();

        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
                new Box(attacker.getBlockPos()).expand(distance),
                (nearbyEntity) -> AbilityHelper.canFireAtEnemy(attacker, nearbyEntity));

        for (LivingEntity nearbyEntity : nearbyEntities) {
            if (nearbyEntity == null) return;
            nearbyEntity.damage(DamageSource.ON_FIRE, damage);
        }
    }

    public static void causeExplosion(LivingEntity user, LivingEntity target, float damageAmount, float distance) {
        for (LivingEntity nearbyEntity : getAoeTargets(target, user, distance)) {
            nearbyEntity.damage(DamageSource.explosion(user), damageAmount);
        }
    }

    private static void addParticles(ServerWorld world, LivingEntity nearbyEntity, ParticleEffect particleEffect) {

        double velX = 0;
        double velY = 1;
        double velZ = 0;

        double startX = nearbyEntity.getX() - .275f;
        double startY = nearbyEntity.getY();
        double startZ = nearbyEntity.getZ() - .275f;

        for (int i = 0; i < 10; i++) {
            double frontX = .5f * random.nextDouble();
            world.spawnParticles(particleEffect, startX + frontX, startY + random.nextDouble() * .5, startZ + .5f,
                    1,velX, velY, velZ, 0);

            double backX = .5f * random.nextDouble();
            world.spawnParticles(particleEffect, startX + backX, startY + random.nextDouble() * .5, startZ,1, velX, velY,
                    velZ,0);

            double leftZ = .5f * random.nextDouble();
            world.spawnParticles(particleEffect, startX, startY + random.nextDouble() * .5, startZ + leftZ,1, velX, velY,
                    velZ,0);

            double rightZ = .5f * random.nextDouble();
            world.spawnParticles(particleEffect, startX + .5f, startY + random.nextDouble() * .5, startZ + rightZ,1, velX,
                    velY, velZ,0);
        }
    }

}
