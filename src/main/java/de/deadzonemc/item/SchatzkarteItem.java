package de.deadzonemc.item;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.Verstecke;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine zerknitterte Skizze: beim Auffalten wird daraus eine Karte mit einem roten Kreuz über einem Versteck,
 * das man noch nicht kennt. Was dort liegt, ist vergraben oder gut versteckt.
 */
public class SchatzkarteItem extends Item {

    public SchatzkarteItem(Properties eigenschaften) {
        super(eigenschaften);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player spieler, InteractionHand hand) {
        ItemStack stack = spieler.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.success(stack);
        }
        List<Long> bekannt = spieler.getData(ModRegistry.VERSTECKE);
        BlockPos ziel = null;
        for (BlockPos p : Verstecke.umkreis(sl.getSeed(), spieler.getBlockX(), spieler.getBlockZ(), 3)) {
            double d = Math.sqrt(p.distToCenterSqr(spieler.getX(), p.getY(), spieler.getZ()));
            if (d > 60 && !bekannt.contains(p.asLong())) {
                ziel = p;
                break;
            }
        }
        if (ziel == null) {
            spieler.displayClientMessage(Component.translatable("item.deadzonemc.schatzkarte.nichts").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        List<Long> neu = new ArrayList<>(bekannt);
        neu.add(ziel.asLong());
        spieler.setData(ModRegistry.VERSTECKE, neu);
        ItemStack karte = MapItem.create(sl, ziel.getX(), ziel.getZ(), (byte) 1, true, true);
        MapItem.renderBiomePreviewMap(sl, karte);
        MapItemSavedData.addTargetDecoration(karte, ziel, "+", MapDecorationTypes.RED_X);
        karte.set(DataComponents.CUSTOM_NAME, Component.translatable("item.deadzonemc.schatzkarte.karte")
                .withStyle(s -> s.withItalic(false)));
        int hinweis = Math.floorMod((int) (ziel.asLong() ^ sl.getSeed()), 3);
        karte.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("item.deadzonemc.schatzkarte.hinweis" + hinweis).withStyle(ChatFormatting.GRAY))));
        sl.playSound(null, spieler.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.9F);
        stack.consume(1, spieler);
        if (stack.isEmpty()) {
            return InteractionResultHolder.success(karte);
        }
        if (!spieler.getInventory().add(karte)) {
            spieler.drop(karte, false);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.schatzkarte.info").withStyle(ChatFormatting.GRAY));
    }
}
