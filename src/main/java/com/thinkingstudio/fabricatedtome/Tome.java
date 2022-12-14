package website.eccentric.tome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class Tome {
    public static ItemStack convert(ItemStack tome, ItemStack book) {
        var modsBooks = getModsBooks(tome);
        var mod = website.eccentric.tome.ModName.from(book);
        var books = modsBooks.get(mod);
        var registry = ForgeRegistries.ITEMS.getKey(book.getItem());
        books.removeIf(b -> ForgeRegistries.ITEMS.getKey(b.getItem()).equals(registry));

        setModsBooks(book, modsBooks);
        website.eccentric.tome.Migration.setVersion(book);
        book.getOrCreateTag().putBoolean(website.eccentric.tome.Tag.IS_TOME, true);
        setHoverName(book);

        return book;
    }

    public static ItemStack revert(ItemStack book) {
        website.eccentric.tome.Migration.apply(book);

        var tome = new ItemStack(website.eccentric.tome.EccentricTome.TOME.get());
        copyMods(book, tome);
        website.eccentric.tome.Migration.setVersion(tome);
        clear(book);

        return tome;
    }

    public static ItemStack attach(ItemStack tome, ItemStack book) {
        var mod = website.eccentric.tome.ModName.from(book);
        var modsBooks = getModsBooks(tome);

        var books = modsBooks.getOrDefault(mod, new ArrayList<ItemStack>());
        books.add(book);
        modsBooks.put(mod, books);

        setModsBooks(tome, modsBooks);
        return tome;
    }

    public static Map<String, List<ItemStack>> getModsBooks(ItemStack stack) {
        return website.eccentric.tome.Tag.deserialize(stack.getOrCreateTagElement(website.eccentric.tome.Tag.MODS));
    }

    public static void setModsBooks(ItemStack stack, Map<String, List<ItemStack>> modsBooks) {
        stack.getOrCreateTag().put(website.eccentric.tome.Tag.MODS, website.eccentric.tome.Tag.serialize(modsBooks));
    }

    public static boolean isTome(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        else if (stack.getItem() instanceof website.eccentric.tome.TomeItem)
            return true;
        else {
            var tag = stack.getTag();
            if (tag == null)
                return false;

            return tag.getBoolean(website.eccentric.tome.Tag.IS_TOME);
        }
    }

    @Nullable
    public static InteractionHand inHand(Player player) {
        var hand = InteractionHand.MAIN_HAND;
        var stack = player.getItemInHand(hand);
        if (isTome(stack))
            return hand;

        hand = InteractionHand.OFF_HAND;
        stack = player.getItemInHand(hand);
        if (isTome(stack))
            return hand;

        return null;
    }

    private static void copyMods(ItemStack source, ItemStack target) {
        var tag = target.getOrCreateTag();
        tag.put(website.eccentric.tome.Tag.MODS, source.getTagElement(website.eccentric.tome.Tag.MODS));
    }

    private static void clear(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null)
            return;

        tag.remove(website.eccentric.tome.Tag.MODS);
        tag.remove(website.eccentric.tome.Tag.IS_TOME);
        tag.remove(website.eccentric.tome.Tag.VERSION);
        stack.resetHoverName();
    }

    private static void setHoverName(ItemStack book) {
        var name = book.getHoverName().copy().withStyle(ChatFormatting.GREEN);
        book.setHoverName(Component.translatable("fabricated-tome.name", name));
    }
}
