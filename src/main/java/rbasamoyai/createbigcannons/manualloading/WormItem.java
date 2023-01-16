package rbasamoyai.createbigcannons.manualloading;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.content.contraptions.components.deployer.DeployerFakePlayer;
import com.simibubi.create.foundation.utility.NBTProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import rbasamoyai.createbigcannons.cannons.CannonBlock;
import rbasamoyai.createbigcannons.cannons.big_cannons.IBigCannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.CannonEnd;
import rbasamoyai.createbigcannons.config.CBCConfigs;

import java.util.List;

public class WormItem extends Item {
	
	private final Multimap<Attribute, AttributeModifier> defaultModifiers;
	
	public WormItem(Properties properties) {
		super(properties);
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 2.5d, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", -3.0d, AttributeModifier.Operation.ADDITION));
		this.defaultModifiers = builder.build();
	}
	
	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
		return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getAttributeModifiers(slot, stack);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player instanceof DeployerFakePlayer && !CBCConfigs.SERVER.cannons.deployersCanUseLoadingTools.get()) return InteractionResult.PASS;
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction reachDirection = context.getClickedFace().getOpposite();
		
		for (int i = 0; i < CBCConfigs.SERVER.cannons.wormReach.get(); ++i) {
			BlockPos pos1 = pos.relative(reachDirection, i);
			BlockState state1 = level.getBlockState(pos1);
			if (!isValidLoadBlock(state1, level, pos1, reachDirection) || !(level.getBlockEntity(pos1) instanceof IBigCannonBlockEntity cbe)) return InteractionResult.FAIL;
			StructureBlockInfo info = cbe.cannonBehavior().block();
			if (info == null || info.state == null || info.state.isAir()) continue;
			BlockPos pos2 = pos1.relative(context.getClickedFace());
			if (level.getBlockEntity(pos2) instanceof IBigCannonBlockEntity cbe1 && !cbe1.canLoadBlock(info)
				|| !(level.getBlockEntity(pos2) instanceof IBigCannonBlockEntity) && !level.getBlockState(pos2).isAir()) {
				return InteractionResult.FAIL;
			}
			
			if (!level.isClientSide) {
				if (level.getBlockEntity(pos2) instanceof IBigCannonBlockEntity cbe2) {
					cbe2.cannonBehavior().loadBlock(info);
				} else if (level.getBlockState(pos2).isAir()) {
					level.setBlock(pos2, info.state, Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_ALL);
					BlockEntity be = level.getBlockEntity(pos2);
					CompoundTag tag = info.nbt;
					if (be != null) tag = NBTProcessors.process(be, tag, false);
					if (be != null && tag != null) {
						tag.putInt("x", pos2.getX());
						tag.putInt("y", pos2.getY());
						tag.putInt("z", pos2.getZ());
						be.load(tag);
					}
				}
				cbe.cannonBehavior().removeBlock();
				level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1, 1);
			}
			player.causeFoodExhaustion(CBCConfigs.SERVER.cannons.loadingToolHungerConsumption.getF());
			player.getCooldowns().addCooldown(this, CBCConfigs.SERVER.cannons.loadingToolCooldown.get());
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return super.useOn(context);
	}
	
	public static boolean isValidLoadBlock(BlockState state, Level level, BlockPos pos, Direction dir) {
		return state.getBlock() instanceof CannonBlock cBlock && cBlock.getOpeningType(level, state, pos) == CannonEnd.OPEN;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);

	}
	
	public static int getReach() { return CBCConfigs.SERVER.cannons.ramRodReach.get(); }
	public static boolean deployersCanUse() { return CBCConfigs.SERVER.cannons.deployersCanUseLoadingTools.get(); }
	
}
