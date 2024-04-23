package net.salju.trialstowers.block;

import net.salju.trialstowers.init.TrialsSounds;
import net.salju.trialstowers.init.TrialsBlockEntities;
import net.salju.trialstowers.events.TrialsManager;
import net.salju.trialstowers.TrialsTowersMod;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.Connection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

public class TrialVaultEntity extends BlockEntity {
	private int cd;

	public TrialVaultEntity(BlockPos pos, BlockState state) {
		super(TrialsBlockEntities.VAULT.get(), pos, state);
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("Cooldown", this.cd);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		this.cd = tag.getInt("Cooldown");
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection queen, ClientboundBlockEntityDataPacket packet) {
		if (packet != null && packet.getTag() != null) {
			this.cd = packet.getTag().getInt("Cooldown");
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Cooldown", this.cd);
		return tag;
	}

	public static void tick(Level world, BlockPos pos, BlockState state, TrialVaultEntity target) {
		if (state.getBlock() instanceof TrialVaultBlock block) {
			target.updateBlock();
			if (world instanceof ServerLevel lvl) {
				if (block.isActive(state)) {
					lvl.sendParticles(ParticleTypes.FLAME, (pos.getX() + 0.5), (pos.getY() + 0.5), (pos.getZ() + 0.5), 1, 0.12, 0.12, 0.12, 0);
					if (block.isEjecting(state)) {
						if (target.getCd() == 1) {
							world.setBlock(pos, state.setValue(TrialVaultBlock.EJECT, Boolean.valueOf(false)).setValue(TrialVaultBlock.ACTIVE, Boolean.valueOf(false)), 3);
							lvl.playSound(null, pos, TrialsSounds.SPAWNER_CLOSE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
							target.setCd(12000);
						} else if (target.getCd() == 0) {
							int e = Mth.nextInt(world.getRandom(), 4, 7);
							target.setCd(20 * e);
							lvl.playSound(null, pos, TrialsSounds.VAULT_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
							for (int i = 0; i != e; ++i) {
								TrialsTowersMod.queueServerWork((20 * i), () -> {
									for (ItemStack stack : TrialsManager.getLoot(target, world, "trials:gameplay/vault_loot")) {
										lvl.playSound(null, pos, TrialsSounds.VAULT_EJECT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
										Containers.dropItemStack(world, pos.getX(), (pos.getY() + 1.0), pos.getZ(), stack);
									}
								});
							}
						} else {
							target.setCd(target.getCd() - 1);
						}
					}
				} else {
					lvl.sendParticles(ParticleTypes.SMOKE, (pos.getX() + 0.5), (pos.getY() + 0.95), (pos.getZ() + 0.5), 1, 0.12, 0.12, 0.12, 0);
					if (target.getCd() != 0) {
						target.setCd(target.getCd() - 1);
					} else {
						world.setBlock(pos, state.setValue(TrialVaultBlock.ACTIVE, Boolean.valueOf(true)), 3);
						lvl.playSound(null, pos, TrialsSounds.VAULT_ACTIVATE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
					}
				}
			}
		}
	}

	public int getCd() {
		return this.cd;
	}

	public void setCd(int i) {
		this.cd = i;
	}

	public void updateBlock() {
		this.setChanged();
		this.getLevel().updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
	}
}
