package me.mrCookieSlime.QuestWorld.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import me.mrCookieSlime.QuestWorld.api.menu.Menu;
import me.mrCookieSlime.QuestWorld.managers.MenuManager;

public class MenuListener implements Listener {

	@EventHandler(ignoreCancelled=true)
	public void onInventoryClick(InventoryClickEvent event) {
		Menu openMenu = MenuManager.get().playerOpenMenus.get(event.getWhoClicked().getUniqueId());
		if(openMenu != null) 
			event.setCancelled(openMenu.click(event));
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent event) {
		MenuManager.get().playerOpenMenus.remove(event.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onDrag(InventoryDragEvent event) {
		Menu openMenu = MenuManager.get().playerOpenMenus.get(event.getWhoClicked().getUniqueId());
		if(openMenu != null)
			for(int key : event.getRawSlots())
				if(openMenu.requestCancel(event.getInventory(), key)) {
					event.setCancelled(true);
					return;
				}
	}
}
