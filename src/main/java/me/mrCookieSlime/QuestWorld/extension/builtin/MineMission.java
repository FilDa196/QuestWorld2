package me.mrCookieSlime.QuestWorld.extension.builtin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.QuestWorld.api.Decaying;
import me.mrCookieSlime.QuestWorld.api.MissionType;
import me.mrCookieSlime.QuestWorld.api.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.contract.IMission;
import me.mrCookieSlime.QuestWorld.api.contract.IMissionState;
import me.mrCookieSlime.QuestWorld.api.contract.MissionEntry;
import me.mrCookieSlime.QuestWorld.api.menu.MenuData;
import me.mrCookieSlime.QuestWorld.api.menu.MissionButton;
import me.mrCookieSlime.QuestWorld.util.ItemBuilder;
import me.mrCookieSlime.QuestWorld.util.PlayerTools;
import me.mrCookieSlime.QuestWorld.util.Text;

public class MineMission extends MissionType implements Listener, Decaying {
	public MineMission() {
		super("MINE_BLOCK", true, new ItemStack(Material.IRON_PICKAXE));
	}
	
	@Override
	public ItemStack userDisplayItem(IMission instance) {
		return instance.getItem();
	}
	
	@Override
	protected String userInstanceDescription(IMission instance) {
		return "&7Mine " + instance.getAmount() + "x " + Text.itemName(instance.getDisplayItem());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onMine(BlockBreakEvent e) {
		for(MissionEntry r : QuestWorld.getMissionEntries(this, e.getPlayer())) {
			if(ItemBuilder.compareItems(PlayerTools.getStackOf(e.getBlock()), r.getMission().getItem()))
				r.addProgress(1);
		}
	}
	
	@Override
	protected void layoutMenu(IMissionState changes) {
		putButton(10, new MenuData(
				new ItemBuilder(changes.getDisplayItem()).wrapLore(
						"",
						"&e> Click to set the block type").get(),
				event -> {
					Player p = (Player)event.getWhoClicked();
					ItemStack mainItem = p.getInventory().getItemInMainHand();
					if(mainItem != null && mainItem.getType().isBlock()) {
						mainItem = mainItem.clone();
						mainItem.setAmount(1);
						changes.setItem(mainItem);
					}
					MissionButton.apply(event, changes);
				}
		));
		putButton(17, MissionButton.amount(changes));
	}
}
