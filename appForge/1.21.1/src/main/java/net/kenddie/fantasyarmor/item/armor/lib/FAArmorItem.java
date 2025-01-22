package net.kenddie.fantasyarmor.item.armor.lib;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.kenddie.fantasyarmor.config.FAConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class FAArmorItem extends ArmorItem implements GeoItem {

    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    protected FAArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ArmorMaterials.NETHERITE, type, new Properties().stacksTo(1));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        if (armorAttributes.armor() > 0) {
            builder.put(
                    Attributes.ARMOR.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "armor"),
                            armorAttributes.armor(),
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        if (armorAttributes.armorToughness() > 0) {
            builder.put(
                    Attributes.ARMOR_TOUGHNESS.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "armor_toughness"),
                            armorAttributes.armorToughness(),
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        if (armorAttributes.knockbackResistance() > 0) {
            builder.put(
                    Attributes.KNOCKBACK_RESISTANCE.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "knockback_resistance"),
                            armorAttributes.knockbackResistance(),
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        if (armorAttributes.movementSpeed() > 0) {
            builder.put(
                    Attributes.MOVEMENT_SPEED.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "movement_speed"),
                            armorAttributes.movementSpeed(),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        if (armorAttributes.maxHealth() > 0) {
            builder.put(
                    Attributes.MAX_HEALTH.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "max_health"),
                            armorAttributes.maxHealth(),
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        if (armorAttributes.attackDamage() > 0) {
            builder.put(
                    Attributes.ATTACK_DAMAGE.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "attack_damage"),
                            armorAttributes.attackDamage(),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        if (armorAttributes.attackSpeed() > 0) {
            builder.put(
                    Attributes.ATTACK_SPEED.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "attack_speed"),
                            armorAttributes.attackSpeed(),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        if (armorAttributes.luck() > 0) {
            builder.put(
                    Attributes.LUCK.value(),
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("fantasyarmor", "luck"),
                            armorAttributes.luck(),
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        attributeModifiers = builder.build();
    }




    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        if (FAConfig.showDescriptions) {
            super.appendHoverText(stack, context, tooltipComponents, flag);

            String translationKey = this.getDescriptionId() + ".tooltip";
            tooltipComponents.add(Component.translatable(translationKey));
        }
    }


    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<? extends FAArmorItem> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = createArmorRenderer();
                }

                Minecraft mc = Minecraft.getInstance();
                renderer.prepForRender(
                        livingEntity,
                        itemStack,
                        equipmentSlot,
                        original,
                        mc.renderBuffers().bufferSource(),
                        mc.getTimer().getGameTimeDeltaPartialTick(true),
                        0, 0, 0, 0
                );

                return renderer;
            }
        });
    }


    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!FAConfig.applyModifiers) {
            return super.getDefaultAttributeModifiers(stack);
        }

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        for (var entry : attributeModifiers.entries()) {
            ResourceLocation attributeName = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey());
            if (attributeName != null) {
                ResourceKey<Attribute> attributeKey = ResourceKey.create(BuiltInRegistries.ATTRIBUTE.key(), attributeName);
                Holder<Attribute> attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolderOrThrow(attributeKey);

                builder.add(attributeHolder, entry.getValue(), EquipmentSlotGroup.ANY);
            }
        }

        return builder.build();
    }






    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract GeoArmorRenderer<? extends FAArmorItem> createArmorRenderer();

    public abstract List<MobEffectInstance> getFullSetEffects();
}
