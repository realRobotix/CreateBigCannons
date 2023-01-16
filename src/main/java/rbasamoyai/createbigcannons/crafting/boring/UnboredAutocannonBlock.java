package rbasamoyai.createbigcannons.crafting.boring;

import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rbasamoyai.createbigcannons.cannons.autocannon.AbstractIncompleteAutocannonBlock;
import rbasamoyai.createbigcannons.cannons.autocannon.AutocannonMaterial;
import rbasamoyai.createbigcannons.crafting.casting.CannonCastShape;

import java.util.function.Supplier;

public class UnboredAutocannonBlock extends AbstractIncompleteAutocannonBlock implements TransformableByBoring {

	private final VoxelShaper shapes;
	private final Supplier<CannonCastShape> cannonShape;

	public UnboredAutocannonBlock(Properties properties, AutocannonMaterial material, VoxelShape shape, Supplier<CannonCastShape> castShape, NonNullSupplier<? extends Block> boredBlockSup) {
		super(properties, material, boredBlockSup);
		this.shapes = new AllShapes.Builder(shape).forDirectional();
		this.cannonShape = castShape;
	}

	public static UnboredAutocannonBlock barrel(Properties properties, AutocannonMaterial material, NonNullSupplier<? extends Block> boredBlock) {
		return new UnboredAutocannonBlock(properties, material, box(6, 0, 6, 10, 16, 10), CannonCastShape.AUTOCANNON_BARREL, boredBlock);
	}

	public static UnboredAutocannonBlock recoilSpring(Properties properties, AutocannonMaterial material, NonNullSupplier<? extends Block> boredBlock) {
		return new UnboredAutocannonBlock(properties, material, box(5, 0, 5, 11, 16, 11), CannonCastShape.AUTOCANNON_RECOIL_SPRING, boredBlock);
	}

	public static UnboredAutocannonBlock breech(Properties properties, AutocannonMaterial material, NonNullSupplier<? extends Block> boredBlock) {
		return new UnboredAutocannonBlock(properties, material, box(4, 0, 4, 12, 16, 12), CannonCastShape.AUTOCANNON_BREECH, boredBlock);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.shapes.get(this.getFacing(state));
	}

	@Override public CannonCastShape getCannonShape() { return this.cannonShape.get(); }

	@Override
	public BlockState getBoredBlockState(BlockState state) {
		BlockState bored = this.getResultBlock().defaultBlockState();
		return bored.hasProperty(FACING) ? bored.setValue(FACING, state.getValue(FACING)) : bored;
	}

}
