package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenshiAbilityHandler {
//    private static final String DAMAGE_RECORD_KEY = "KogasaTenshiDamageRecord";
//    private static final List<MobEffect> RANDOM_BUFFS = List.of(
//            MobEffects.MOVEMENT_SPEED,
//            MobEffects.DIG_SPEED,
//            MobEffects.DAMAGE_BOOST,
//            MobEffects.JUMP,
//            MobEffects.REGENERATION,
//            MobEffects.DAMAGE_RESISTANCE,
//            MobEffects.FIRE_RESISTANCE,
//            MobEffects.WATER_BREATHING,
//            MobEffects.INVISIBILITY,
//            MobEffects.NIGHT_VISION,
//            MobEffects.HEALTH_BOOST,
//            MobEffects.ABSORPTION,
//            MobEffects.LUCK,
//            MobEffects.SLOW_FALLING,
//            MobEffects.CONDUIT_POWER,
//            MobEffects.DOLPHINS_GRACE,
//            MobEffects.HERO_OF_THE_VILLAGE
//    );
//
//    private TenshiAbilityHandler() {}
//
//    @SubscribeEvent
//    public static void onLivingHurt(LivingHurtEvent event) {
//        // Incoming damage charges the no-mind, no-thought state while Tenshi armor is equipped.
//        if (event.getEntity() instanceof ServerPlayer hurtPlayer && hasTenshiArmor(hurtPlayer)) {
//            double threshold = Config.TENSHI_DAMAGE_THRESHOLD.get();
//            double current = getDamageRecord(hurtPlayer);
//            if (current < threshold) {
//                // Excess damage beyond the configured threshold is intentionally discarded.
//                double accepted = Math.min(Math.max(event.getAmount(), 0.0F), threshold - current);
//                setDamageRecord(hurtPlayer, current + accepted);
//            }
//        }
//
//        // Melee and projectile hits share the same Tenshi weapon effects.
//        ServerPlayer attacker = resolveAttacker(event);
//        if (attacker == null) return;
//        LivingEntity target = event.getEntity();
//
//        // The armor's charged retaliation is independent of which weapon the player uses next.
//        double threshold = Config.TENSHI_DAMAGE_THRESHOLD.get();
//        if (hasTenshiArmor(attacker) && getDamageRecord(attacker) >= threshold) {
//            float bonus = (float) (target.getMaxHealth() * Config.TENSHI_RETALIATION_RATIO.get());
//            event.setAmount(event.getAmount() + Math.max(0.0F, bonus));
//            setDamageRecord(attacker, 0.0);
//        }
//
//        ItemStack weapon = resolveWeapon(attacker);
//        if (weapon.isEmpty() || !KogasaAbilityData.hasAbility(weapon, KogasaAbility.TENSHI)) return;
//
//        int duration = Math.max(1, Config.TENSHI_DEBUFF_DURATION_SECONDS.get()) * 20;
//        // Amplifier 127 is the largest positive value that survives vanilla effect NBT serialization.
//        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 127, false, true, true));
//        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 127, false, true, true));
//
//        if (target instanceof FlyingMob && !(target instanceof EnderDragon) && !(target instanceof WitherBoss)) {
//            event.setAmount(event.getAmount() * Config.TENSHI_FLYING_DAMAGE_MULTIPLIER.get().floatValue());
//        }
//
//        applyStackingRandomBuff(attacker);
//    }
//
//    @SubscribeEvent
//    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
//        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
//        // Sync only a compact state snapshot every five ticks; the server remains authoritative.
//        if (player.tickCount % 5 == 0) {
//            boolean active = hasTenshiArmor(player);
//            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
//                    new TenshiStateSyncPacket(active ? getDamageRecord(player) : 0.0, active));
//        }
//        if (!hasTenshiArmor(player)) return;
//
//        double recorded = getDamageRecord(player);
//        if (recorded <= 0.0) return;
//        int threshold = Math.max(1, Config.TENSHI_DAMAGE_THRESHOLD.get());
//        int step = Math.max(1, (threshold + 3) / 4); // ceil(threshold / 4), default 5.
//        int resistanceLevel = recorded >= threshold
//                ? 4 : Math.min(3, (int) Math.floor(recorded / step));
//        if (resistanceLevel <= 0) return;
//        player.addEffect(new MobEffectInstance(
//                MobEffects.DAMAGE_RESISTANCE, 2, resistanceLevel - 1, true, false, false));
//    }
//
//    @SubscribeEvent
//    public static void onPlayerDeath(LivingDeathEvent event) {
//        if (event.getEntity() instanceof ServerPlayer player) clearDamageRecord(player);
//    }
//
//    public static void clearDamageRecord(Player player) {
//        player.getPersistentData().remove(DAMAGE_RECORD_KEY);
//    }
//
//    /** Current damage accumulated by the Tenshi armor set's no-mind state. */
//    public static double getDamageRecord(Player player) {
//        return Math.max(0.0, player.getPersistentData().getDouble(DAMAGE_RECORD_KEY));
//    }
//
//    /** Whether any worn equipment currently grants Tenshi's armor ability. */
//    public static boolean hasTenshiArmorAbility(Player player) {
//        return hasTenshiArmor(player);
//    }
//
//    /** Resistance amplifier level shown to players (0 means no Resistance level). */
//    public static int getDisplayedResistanceLevel(Player player) {
//        if (!hasTenshiArmor(player)) return 0;
//        double threshold = Math.max(1.0, Config.TENSHI_DAMAGE_THRESHOLD.get());
//        double recorded = getDamageRecord(player);
//        int step = Math.max(1, (int) Math.ceil(threshold / 4.0));
//        if (recorded <= 0.0) return 0;
//        if (recorded >= threshold) return 4;
//        return Math.min(3, (int) Math.floor(recorded / step));
//    }
//
//    private static void setDamageRecord(Player player, double value) {
//        player.getPersistentData().putDouble(DAMAGE_RECORD_KEY, Math.max(0.0, value));
//    }
//
//    private static boolean hasTenshiArmor(Player player) {
//        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
//                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
//            ItemStack stack = player.getItemBySlot(slot);
//            if (stack.getItem() instanceof IKogasaEquipment
//                    && KogasaAbilityData.hasAbility(stack, KogasaAbility.TENSHI)) return true;
//        }
//        return false;
//    }
//
//    private static ServerPlayer resolveAttacker(LivingHurtEvent event) {
//        if (event.getSource().getEntity() instanceof ServerPlayer player) return player;
//        if (event.getSource().getDirectEntity() instanceof Projectile projectile
//                && projectile.getOwner() instanceof ServerPlayer player) return player;
//        return null;
//    }
//
//    private static ItemStack resolveWeapon(ServerPlayer player) {
//        ItemStack main = player.getMainHandItem();
//        if (main.getItem() instanceof IKogasaEquipment) return main;
//        ItemStack off = player.getOffhandItem();
//        if (off.getItem() instanceof IKogasaEquipment) return off;
//        return ItemStack.EMPTY;
//    }
//
//    private static void applyStackingRandomBuff(ServerPlayer player) {
//        MobEffect effect = RANDOM_BUFFS.get(player.getRandom().nextInt(RANDOM_BUFFS.size()));
//        MobEffectInstance old = player.getEffect(effect);
//        int durationCap = 60 * 20;
//        int duration = Math.min(durationCap, (old == null ? 0 : old.getDuration()) + 10 * 20);
//        int amplifier = Math.min(3, old == null ? 0 : old.getAmplifier() + 1);
//        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
//    }
}
