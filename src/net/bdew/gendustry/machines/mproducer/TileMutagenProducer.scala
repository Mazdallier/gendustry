/*
 * Copyright (c) bdew, 2013 - 2014
 * https://github.com/bdew/gendustry
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.gendustry.machines.mproducer

import net.bdew.gendustry.apiimpl.TileWorker
import net.bdew.gendustry.config.{Fluids, Items}
import net.bdew.gendustry.fluids.MutagenSources
import net.bdew.gendustry.power.TilePowered
import net.bdew.lib.covers.TileCoverable
import net.bdew.lib.data._
import net.bdew.lib.data.base.UpdateKind
import net.bdew.lib.power.TileBaseProcessor
import net.bdew.lib.tile.ExposeTank
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids._

class TileMutagenProducer extends TileBaseProcessor with TileWorker with TilePowered with ExposeTank with TileCoverable {
  lazy val cfg = MachineMutagenProducer

  val tank = DataSlotTankRestricted("tank", this, cfg.tankSize, Fluids.mutagen).setUpdate(UpdateKind.GUI, UpdateKind.SAVE)
  val output = DataSlotInt("output", this).setUpdate(UpdateKind.SAVE)

  def getSizeInventory = 1

  def getTankFromDirection(dir: ForgeDirection): IFluidTank = tank

  def isWorking = output > 0
  def tryStart(): Boolean = {
    if (getStackInSlot(0) != null) {
      output := MutagenSources.getValue(getStackInSlot(0))
      decrStackSize(0, 1)
      return true
    } else return false
  }

  def tryFinish(): Boolean = {
    if (tank.fill(output, false) == output.value) {
      tank.fill(output, true)
      output := -1
      return true
    } else return false
  }

  def sendFluid() {
    for (dir <- ForgeDirection.VALID_DIRECTIONS) {
      val te: TileEntity = worldObj.getTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ)
      if (te != null && te.isInstanceOf[IFluidHandler]) {
        val pumped = te.asInstanceOf[IFluidHandler].fill(dir.getOpposite, tank.getFluid.copy(), true)
        if (pumped > 0) {
          tank.drain(pumped, true)
          if (tank.getFluidAmount <= 0) return
        }
      }
    }
  }

  override def tickServer() {
    super.tickServer()
    if (tank.getFluidAmount > 0) sendFluid()
  }

  allowSided = true
  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = MutagenSources.getValue(stack) > 0
  override def canExtractItem(slot: Int, stack: ItemStack, side: Int): Boolean = false

  override def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean) = 0
  override def canFill(from: ForgeDirection, fluid: Fluid) = false

  override def isValidCover(side: ForgeDirection, cover: ItemStack) = cover.getItem == Items.coverImport
}