package me.mrCookieSlime.QuestWorld.extension.builtin;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.QuestWorld.api.Decaying;
import me.mrCookieSlime.QuestWorld.api.MissionType;
import me.mrCookieSlime.QuestWorld.api.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.contract.IMission;
import me.mrCookieSlime.QuestWorld.api.contract.IMissionState;
import me.mrCookieSlime.QuestWorld.api.contract.MissionEntry;
import me.mrCookieSlime.QuestWorld.api.menu.MissionButton;
import me.mrCookieSlime.QuestWorld.util.ItemBuilder;
import me.mrCookieSlime.QuestWorld.util.Text;

public class FishMission extends MissionType implements Listener, Decaying {
	public FishMission() {
		super("FISH", true, new ItemStack(Material.FISHING_ROD));
	}
	
	@Override
	public ItemStack userDisplayItem(IMission instance) {
		return instance.getItem();
	}
	
	@Override
	protected String userInstanceDescription(IMission instance) {
		return "&7Reel in " + instance.getAmount() + "x " + Text.itemName(instance.getDisplayItem());
	}
	
	@EventHandler
	public void onFish(PlayerFishEvent e) {
		if (!(e.getCaught() instanceof Item))
			return;
		ItemStack caught = ((Item)e.getCaught()).getItemStack();

		for(MissionEntry r : QuestWorld.getMissionEntries(this, e.getPlayer()))
			if(ItemBuilder.compareItems(caught, r.getMission().getItem()))
				r.addProgress(caught.getAmount());
	}
	
	@Override
	protected void layoutMenu(IMissionState changes) {
		putButton(10, MissionButton.item(changes));
		putButton(17, MissionButton.amount(changes));
	}
}
