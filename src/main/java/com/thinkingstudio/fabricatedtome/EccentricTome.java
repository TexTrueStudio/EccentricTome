package com.thinkingstudio.fabricatedtome;

import io.github.fabricators_of_create.porting_lib.event.common.PlayerEvents;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import website.eccentric.tome.client.RenderGuiOverlayHandler;
import website.eccentric.tome.client.TomeHandler;
import website.eccentric.tome.network.RevertMessage;
import website.eccentric.tome.network.TomeChannel;

//@Mod(EccentricTome.ID)
public class EccentricTome implements ModInitializer {
    public static final String ID = "fabricated-tome";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, ID);

    public static final RegistryObject<RecipeSerializer<?>> ATTACHMENT = RECIPES.register("attachment",
            () -> new SimpleCraftingRecipeSerializer<>(website.eccentric.tome.AttachmentRecipe::new));
    public static final RegistryObject<Item> TOME = ITEMS.register("tome", website.eccentric.tome.TomeItem::new);

    public static SimpleChannel CHANNEL;

    public EccentricTome() {
        var modEvent = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modEvent);
        RECIPES.register(modEvent);

        modEvent.addListener(this::onClientSetup);
        modEvent.addListener(this::onCommonSetup);
        modEvent.addListener(this::onModConfig);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, website.eccentric.tome.Configuration.SPEC);

        var minecraftEvent = MinecraftForge.EVENT_BUS;
        minecraftEvent.addListener(EventPriority.LOW, this::onItemDropped);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        var minecraftEvent = MinecraftForge.EVENT_BUS;
        minecraftEvent.addListener(this::onLeftClickEmpty);
        minecraftEvent.addListener(EventPriority.LOW, RenderGuiOverlayHandler::onRender);
        minecraftEvent.addListener(TomeHandler::onOpenTome);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        CHANNEL = TomeChannel.register();
    }

    private void onModConfig(ModConfigEvent event) {
        website.eccentric.tome.Configuration.ALIAS_MAP.clear();
        for (var alias : website.eccentric.tome.Configuration.ALIASES.get()) {
            var tokens = alias.split("=");
            website.eccentric.tome.Configuration.ALIAS_MAP.put(tokens[0], tokens[1]);
        }
    }

    private void onLeftClickEmpty(PlayerHeadItem event) {
        var stack = event.getItemStack();
        if (website.eccentric.tome.Tome.isTome(stack) && !(stack.getItem() instanceof website.eccentric.tome.TomeItem)) {
            CHANNEL.sendToServer(new RevertMessage());
        }
    }

    private void onItemDropped(ItemTossEvent event) {
        if (!event.getPlayer().isShiftKeyDown())
            return;

        var entity = event.getEntity();
        var stack = entity.getItem();

        if (website.eccentric.tome.Tome.isTome(stack) && !(stack.getItem() instanceof website.eccentric.tome.TomeItem)) {
            var detatchment = website.eccentric.tome.Tome.revert(stack);
            var level = entity.getCommandSenderWorld();

            if (!level.isClientSide) {
                level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), detatchment));
            }

            entity.setItem(stack);
        }
    }
}
