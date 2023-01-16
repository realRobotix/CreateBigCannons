package rbasamoyai.createbigcannons.cannons;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public abstract class CannonBehavior extends TileEntityBehaviour {

	protected final Set<Direction> connectedTowards = EnumSet.noneOf(Direction.class);
	protected Direction currentFacing;

	protected CannonBehavior(SmartTileEntity te) {
		super(te);
	}
	
	@Override
	public void tick() {
		super.tick();
		
		BlockState state = this.tileEntity.getBlockState();
		if (state.hasProperty(BlockStateProperties.FACING)) {
			Direction previousFacing = this.currentFacing;
			this.currentFacing = state.getValue(BlockStateProperties.FACING);
			if (previousFacing != null && previousFacing != this.currentFacing) {
				Direction.Axis rotationAxis = getRotationAxis(previousFacing, currentFacing);
				Rotation rotation = getRotationBetween(previousFacing, this.currentFacing, rotationAxis);
				
				EnumSet<Direction> copyFrom = EnumSet.noneOf(Direction.class);
				this.connectedTowards.forEach(d -> {
					Direction dc = d;
					for (int i = 0; i < rotation.ordinal(); ++i) {
						dc = dc.getClockWise(rotationAxis);
					}
					copyFrom.add(dc);
				});
				this.connectedTowards.clear();
				this.connectedTowards.addAll(copyFrom);
				this.tileEntity.setChanged();
			}
		}
	}
	
	public boolean isConnectedTo(Direction face) {
		return this.connectedTowards.contains(face);
	}
	
	public void setConnectedFace(Direction face, boolean isConnected) {
		if (isConnected) {
			if (this.connectedTowards.add(face)) this.tileEntity.setChanged();
		} else {
			if (this.connectedTowards.remove(face)) this.tileEntity.setChanged();
		}
	}
	
	private static Direction.Axis getRotationAxis(Direction prev, Direction current) {
		Set<Direction.Axis> axes = EnumSet.allOf(Direction.Axis.class);
		axes.remove(prev.getAxis());
		axes.remove(current.getAxis());
		return axes.stream().findFirst().orElseThrow(() -> new IllegalStateException("Failed to find the rotation axes of two different axes"));
	}
	
	private static Rotation getRotationBetween(Direction prev, Direction current, Direction.Axis axis) {
		if (prev == current) return Rotation.NONE;
		if (prev == current.getOpposite()) return Rotation.CLOCKWISE_180;
		return prev.getClockWise(axis) == current ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
	}
	
	@Override public boolean isSafeNBT() { return true; }
	
	@Override
	public void write(CompoundTag nbt, boolean spawnPacket) {
		if (this.currentFacing != null) {
			nbt.putString("Facing", this.currentFacing.getSerializedName());
		}

		ListTag connectionTag = new ListTag();
		this.connectedTowards.stream()
		.map(Direction::getSerializedName)
		.map(StringTag::valueOf)
		.forEach(connectionTag::add);
		nbt.put("Connections", connectionTag);
		
		super.write(nbt, spawnPacket);
	}
	
	@Override
	public void read(CompoundTag nbt, boolean clientPacket) {
		this.currentFacing = nbt.contains("Facing") ? Direction.byName(nbt.getString("Facing")) : null;
		
		this.connectedTowards.clear();
		ListTag connectionTag = nbt.getList("Connections", Tag.TAG_STRING);
		connectionTag.stream()
		.map(Tag::getAsString)
		.map(Direction::byName)
		.filter(Objects::nonNull)
		.forEach(this.connectedTowards::add);
		
		super.read(nbt, clientPacket);
	}

}
