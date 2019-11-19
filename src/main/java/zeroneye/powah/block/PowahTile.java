package zeroneye.powah.block;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import zeroneye.lib.block.IInvBase;
import zeroneye.lib.block.TileBase;
import zeroneye.lib.util.Energy;
import zeroneye.powah.energy.PowahStorage;
import zeroneye.powah.energy.RedstoneMode;
import zeroneye.powah.energy.SideConfig;

import javax.annotation.Nullable;

public abstract class PowahTile extends TileBase.Tickable implements IInvBase {
    protected RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    protected final PowahStorage internal;
    protected SideConfig sideConfig;
    protected boolean isCreative;

    public PowahTile(TileEntityType<?> type, int capacity, int maxReceive, int maxExtract, boolean isCreative) {
        super(type);
        this.internal = new PowahStorage(capacity, maxReceive, maxExtract);
        this.sideConfig = new SideConfig(this);
        this.isCreative = isCreative;
        if (isCreative) {
            this.internal.setEnergy(capacity);
        }
    }

    public PowahTile(TileEntityType<?> type) {
        this(type, 0, 0, 0, false);
    }

    @Override
    public void readStorable(CompoundNBT compound) {
        super.readStorable(compound);
        this.internal.read(compound);
        this.sideConfig.read(compound);
        this.isCreative = compound.getBoolean("IsCreative");

    }

    @Override
    public CompoundNBT writeStorable(CompoundNBT compound) {
        this.internal.write(compound);
        this.sideConfig.write(compound);
        compound.putBoolean("IsCreative", this.isCreative);
        return super.writeStorable(compound);
    }

    @Override
    public void readSync(CompoundNBT compound) {
        super.readSync(compound);
        this.redstoneMode = RedstoneMode.values()[compound.getInt("RedstoneMode")];
    }

    @Override
    public CompoundNBT writeSync(CompoundNBT compound) {
        compound.putInt("RedstoneMode", this.redstoneMode.ordinal());
        return super.writeSync(compound);
    }

    @Override
    protected void postTicks() {
        if (this.world == null || !checkRedstone()) return;

        int extracted = 0;
        for (Direction direction : Direction.values()) {
            if (!canExtract(direction)) break;
            int amount = Math.min(this.internal.getMaxReceive(), this.internal.getEnergyStored());
            int received = Energy.receive(this.world, this.pos.offset(direction), direction.getOpposite(), amount, false);
            extracted += extractEnergy(received, false, direction);
        }

        for (int i = 0; i < getChargingSlots(); i++) {
            final ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty() && canExtract(null)) {
                int amount = Math.min(this.internal.getMaxReceive(), this.internal.getEnergyStored());
                int received = Energy.receive(stack, amount, false);
                extracted += extractEnergy(received, false, null);
            }
        }

        if (extracted > 0) {
            sync(true);
        }
    }

    @Override
    public int getSyncTicks() {
        return isContainerOpen() ? 10 : 40;
    }

    @Override
    public int getSizeInventory() {
        return getChargingSlots() + getUpgradSlots();
    }

    private int getUpgradSlots() {
        return 0;
    }

    public int getChargingSlots() {
        return 0;
    }

    public int receiveEnergy(int maxReceive, boolean simulate, @Nullable Direction side) {
        if (!canReceive(side)) return 0;
        int energyReceived = Math.min(this.internal.getMaxEnergyStored() - this.internal.getEnergyStored(), Math.min(this.internal.getMaxReceive(), maxReceive));
        if (!simulate) {
            this.internal.setEnergy(this.internal.getEnergyStored() + energyReceived);
        }
        return energyReceived;
    }

    public int extractEnergy(int maxExtract, boolean simulate, @Nullable Direction side) {
        if (!canExtract(side)) return 0;
        int energyExtracted = Math.min(this.internal.getEnergyStored(), Math.min(this.internal.getMaxExtract(), maxExtract));
        if (!simulate && !this.isCreative) {
            this.internal.setEnergy(this.internal.getEnergyStored() - energyExtracted);
        }
        return energyExtracted;
    }

    public PowahStorage getInternal() {
        return internal;
    }

    public SideConfig getSideConfig() {
        return sideConfig;
    }

    public boolean canExtract(@Nullable Direction side) {
        return checkRedstone() && (this.sideConfig.getPowerMode(side).isOut() || side == null) && this.internal.canExtract();
    }

    public boolean canReceive(@Nullable Direction side) {
        return checkRedstone() && (this.sideConfig.getPowerMode(side).isIn() || side == null) && this.internal.canReceive() && !this.isCreative;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
    }

    public void nextRedstoneMode() {
        int i = this.redstoneMode.ordinal() + 1;
        RedstoneMode redstoneMode = RedstoneMode.values()[i > 2 ? 0 : i];
        this.redstoneMode = redstoneMode;
        markDirtyAndSync();
    }

    @Override
    protected boolean doTicks() {
        return checkRedstone();
    }

    public boolean checkRedstone() {
        boolean power = this.world != null && this.world.getRedstonePowerFromNeighbors(this.pos) > 0;
        return RedstoneMode.IGNORE.equals(getRedstoneMode())
                || power && RedstoneMode.ON.equals(getRedstoneMode())
                || !power && RedstoneMode.OFF.equals(getRedstoneMode());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityEnergy.ENERGY ? LazyOptional.of(() -> new PowahStorage(this.internal) {
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return PowahTile.this.extractEnergy(maxExtract, simulate, side);
            }

            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return PowahTile.this.receiveEnergy(maxReceive, simulate, side);
            }

            @Override
            public boolean canReceive() {
                return PowahTile.this.canReceive(side);
            }

            @Override
            public boolean canExtract() {
                return PowahTile.this.canExtract(side);
            }
        }).cast() : super.getCapability(cap, side);
    }
}