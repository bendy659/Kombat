package ru.benos.combat

import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.slf4j.LoggerFactory

@Mod(Kombat.MOD_ID)
class Kombat {
  companion object {
    const val MOD_ID = "combat"
    val LOGGER = LoggerFactory.getLogger(Kombat::class.java)
  }

  init {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  fun onClientSetup(e: FMLClientSetupEvent) {
    LOGGER.info("# CLIENT SETUP \"Combat\" #")
    LOGGER.info("# Hello \"{}\"!", Minecraft.getInstance().user.name)
  }

  @SubscribeEvent
  fun onCommonSetup(e: FMLCommonSetupEvent) {
    LOGGER.info("# COMMON SETUP \"Combat\" #")
  }
}
