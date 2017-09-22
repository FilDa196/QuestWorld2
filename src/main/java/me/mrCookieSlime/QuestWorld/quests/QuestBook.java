package me.mrCookieSlime.QuestWorld.quests;

import java.util.ArrayList;
import java.util.List;

import me.mrCookieSlime.QuestWorld.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.CategoryChange;
import me.mrCookieSlime.QuestWorld.api.Manual;
import me.mrCookieSlime.QuestWorld.api.MissionChange;
import me.mrCookieSlime.QuestWorld.api.MissionType;
import me.mrCookieSlime.QuestWorld.api.QuestChange;
import me.mrCookieSlime.QuestWorld.api.SinglePrompt;
import me.mrCookieSlime.QuestWorld.api.Translation;
import me.mrCookieSlime.QuestWorld.api.menu.Buttons;
import me.mrCookieSlime.QuestWorld.api.menu.Menu;
import me.mrCookieSlime.QuestWorld.api.menu.MissionButton;
import me.mrCookieSlime.QuestWorld.containers.PagedMapping;
import me.mrCookieSlime.QuestWorld.managers.PlayerManager;
import me.mrCookieSlime.QuestWorld.parties.Party;
import me.mrCookieSlime.QuestWorld.utils.ItemBuilder;
import me.mrCookieSlime.QuestWorld.utils.PlayerTools;
import me.mrCookieSlime.QuestWorld.utils.Text;
import me.mrCookieSlime.QuestWorld.utils.ItemBuilder.Proto;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class QuestBook {
	
	public static void openMainMenu(Player p) {
		QuestWorld.getSounds().QuestClick().playTo(p);
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(null);
		
		Menu menu = new Menu(1, QuestWorld.translate(Translation.gui_title));

		addPartyMenuButton(menu, p);
		
		PagedMapping view = new PagedMapping(45, 9);
		view.hackNav(4);
		for(Category category : QuestWorld.getInstance().getCategories()) {
			if (!category.isHidden()) {
				if (category.isWorldEnabled(p.getWorld().getName())) {
					if ((category.getParent() != null && !QuestWorld.getInstance().getManager(p).hasFinished(category.getParent())) || !category.hasPermission(p)) {
						view.addItem(category.getID(), new ItemBuilder(Material.BARRIER).display(category.getName()).lore(
								"",
								QuestWorld.translate(Translation.quests_locked)).get());
					}
					else {
						view.addNavButton(category.getID(),
								new ItemBuilder(category.getItem()).lore(
										"",
										category.getProgress(p),
										"",
										"&7" + category.getQuests().size() + QuestWorld.translate(Translation.category_desc_total),
										"&a" + category.getFinishedQuests(p).size() + QuestWorld.translate(Translation.category_desc_completed),
										"&b" + category.getQuests(p, QuestStatus.AVAILABLE).size() + QuestWorld.translate(Translation.category_desc_available),
										"&e" + category.getQuests(p, QuestStatus.ON_COOLDOWN).size() + QuestWorld.translate(Translation.category_desc_cooldown),
										"&5" + category.getQuests(p, QuestStatus.REWARD_CLAIMABLE).size() + QuestWorld.translate(Translation.category_desc_claimable_reward))
								.get(),
								event -> {
									Player p2 = (Player) event.getWhoClicked();
									QuestWorld.getInstance().getManager(p2).putPage(0);
									openCategory(p2, category, true);
								}
						);
					}
				}
				else {
					view.addItem(category.getID(), new ItemBuilder(Material.BARRIER).display(category.getName()).lore(
							"",
							QuestWorld.translate(Translation.quests_locked_in_world)).get());
				}
			}
		}
		view.build(menu, p);
		menu.openFor(p);
	}
	
	public static void openLastMenu(Player p) {
		QuestingObject last = QuestWorld.getInstance().getManager(p).getLastEntry();
		if (last != null) {			
			if(last instanceof Quest) {
				Quest q = (Quest)last;
				
				if(q.isValid()) {
					QuestBook.openQuest(p, q, true, true);
					return;
				}
				else
					last = q.getCategory();
			}
			
			if (last instanceof Category) {
				Category c = (Category)last;

				if(c.isValid()) {
					QuestBook.openCategory(p, c, true);
					return;
				}
			}
		}
		
		QuestBook.openMainMenu(p);
	}
	
	private static void addPartyMenuButton(Menu menu, Player p) {
		if (QuestWorld.getInstance().getCfg().getBoolean("party.enabled")) {
			menu.put(4, new ItemBuilder(SkullType.PLAYER)
					.display(QuestWorld.translate(Translation.gui_party))
					.lore(QuestWorld.getInstance().getManager(p).getProgress(), "", QuestWorld.translate(Translation.button_open))
					.get(),
					event -> {
						openPartyMenu((Player) event.getWhoClicked());
					}
			);
		}
		else {
			menu.put(4, new ItemBuilder(Material.ENCHANTED_BOOK)
					.display("&eQuest Book")
					.lore("", QuestWorld.getInstance().getManager(p).getProgress()).get(),
					null);
		}
	}

	public static void openPartyMembers(final Player p) {
		QuestWorld.getSounds().PartyClick().playTo(p);
		
		Menu menu = new Menu(1, QuestWorld.translate(Translation.gui_party));

		ItemBuilder skull = new ItemBuilder(SkullType.PLAYER);
		menu.put(4,
				skull.display(QuestWorld.translate(Translation.gui_party)).lore("", QuestWorld.translate(Translation.button_back_party)).get(),
				event -> {
					openPartyMenu((Player) event.getWhoClicked());
				}
		);

		final Party party = QuestWorld.getInstance().getManager(p).getParty();
		if (party != null) {
			for (int i = 0; i < party.getSize(); i++) {
				final OfflinePlayer player = Bukkit.getOfflinePlayer(party.getPlayers().get(i));
				if (!party.isLeader(p)) {
					menu.put(i + 9,
							skull.skull(player.getName())
							.display("&e" + player.getName())
							.lore("", (party.isLeader(player) ? "&4Party Leader": "&eParty Member")).get(),
							null
					);
				}
				else {
					menu.put(i + 9,
							skull.skull(player.getName())
							.display("&e" + player.getName())
							.lore("", (party.isLeader(player) ? "&5&lParty Leader": "&e&lParty Member"), "", (party.isLeader(player) ? "": "&7&oClick here to kick this Member"))
							.get(),
							event -> {
								if (!party.isLeader(player)) {
									party.kickPlayer(player.getName());
									openPartyMembers((Player) event.getWhoClicked());
								}
							}
					);
				}
			}
		}
		
		menu.openFor(p);
	}

	public static void openPartyMenu(final Player p) {
		QuestWorld.getSounds().PartyClick().playTo(p);
		
		Menu menu = new Menu(2, QuestWorld.translate(Translation.gui_party));
		
		menu.put(4, new ItemBuilder(Material.MAP).display(QuestWorld.translate(Translation.gui_title)).lore(
				"",
				QuestWorld.translate(Translation.button_back_quests)).get(),
				event -> {
					openMainMenu((Player) event.getWhoClicked());
				}
		);
		
		final Party party = QuestWorld.getInstance().getManager(p).getParty();
		
		ItemBuilder wool = new ItemBuilder(Material.WOOL);
		
		if (party == null) {
			menu.put(9,
					wool.color(DyeColor.GREEN)
					.display("&a&lCreate a new Party")
					.lore("", "&rCreates a brand new Party for you", "&rto invite Friends and share your Progress").getNew(),
					event -> {
						Player p2 = (Player) event.getWhoClicked();
						Party.create(p2);
						openPartyMenu(p2);
					}
			);
		}
		else {
			if (party.isLeader(p)) {
				menu.put(9,
						wool.color(DyeColor.GREEN)
						.display("&a&lInvite a Player")
						.lore("",
								"&rInvites a Player to your Party",
								"&rMax. Party Members: &e" + QuestWorld.getInstance().getCfg().getInt("party.max-members")).getNew(),
						event -> {
							Player p2 = (Player) event.getWhoClicked();
							if (party.getSize() >= QuestWorld.getInstance().getCfg().getInt("party.max-members"))
								PlayerTools.sendTranslation(p2, true, Translation.party_errorfull);
							else {
								PlayerTools.promptInput(p2, new SinglePrompt(
										PlayerTools.makeTranslation(true, Translation.party_playerpick),
										(c,s) -> {
											String name = Text.decolor(s).replace("@", "");

											Player player = PlayerTools.getPlayer(name);
											if (player != null) {
												if (QuestWorld.getInstance().getManager(player).getParty() == null) {
													PlayerTools.sendTranslation(p2, true, Translation.party_playeradd, name);
													try {
														party.invitePlayer(player);
													} catch (Exception e1) {
														e1.printStackTrace();
													}
												}
												else PlayerTools.sendTranslation(p2, true, Translation.party_errormember, name);
											}
											else {
												PlayerTools.sendTranslation(p2, true, Translation.party_errorabsent, name);
											}
											return true;
										}
								));

								PlayerTools.closeInventoryWithEvent(p2);
							}
						}
				);
				
				menu.put(17,
						wool.color(DyeColor.RED)
						.display("&4&lDelete your Party")
						.lore("", "&rDeletes this Party", "&rBe careful with this Option!").getNew(),
						event -> {
							party.abandon();
							openPartyMenu((Player) event.getWhoClicked());
						}
				);
			}
			else {
				menu.put(17,
						wool.color(DyeColor.RED)
						.display("&4&lLeave your Party")
						.lore("", "&rLeaves this Party", "&rBe careful with this Option!").getNew(), 
						event -> {
							Player p2 = (Player) event.getWhoClicked();
							party.kickPlayer(p2.getName());
							openPartyMenu(p2);
						}
				);
			}
			
			menu.put(13,
					new ItemBuilder(SkullType.PLAYER)
					.display("&eMember List")
					.lore("", "&rShows you all Members of this Party").get(),
					event -> {
						openPartyMembers((Player) event.getWhoClicked());
					}
			);
		}
		
		menu.openFor(p);
	}

	public static void openCategory(Player p, Category category, final boolean back) {
		QuestWorld.getSounds().QuestClick().playTo(p);
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(category);
		
		Menu menu = new Menu(3, QuestWorld.translate(Translation.gui_title));
		
		addPartyMenuButton(menu, p);
		
		if (back)
			menu.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), event -> {
				openMainMenu((Player) event.getWhoClicked());
			});
		
		ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE).color(DyeColor.RED);
		
		PagedMapping view = new PagedMapping(45, 9);
		view.hackNav(4);
		for (final Quest quest: category.getQuests()) {
			glassPane.display(quest.getName());
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED) || !quest.isWorldEnabled(p.getWorld().getName())) {
				view.addItem(quest.getID(), glassPane.lore("", QuestWorld.translate(Translation.quests_locked)).getNew());
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED_NO_PARTY)) {
				view.addItem(quest.getID(), glassPane.lore("", "&4You need to leave your current Party").getNew());
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.LOCKED_PARTY_SIZE)) {
				view.addItem(quest.getID(), glassPane.lore("", "&4You can only do this Quest in a Party", "&4with at least &c" + quest.getPartySize() + " &4Members").getNew());
			}
			else {
				List<String> lore = new ArrayList<String>();
				lore.add("");
				lore.add(quest.getProgress(p));
				lore.add("");
				lore.add(Text.colorize("&7") + quest.getFinishedTasks(p).size() + "/" + quest.getMissions().size() + QuestWorld.translate(Translation.quests_tasks_completed));
				if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.REWARD_CLAIMABLE)) {
					lore.add("");
					lore.add(QuestWorld.translate(Translation.quests_state_reward_claimable));
				}
				else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
					lore.add("");
					lore.add(QuestWorld.translate(Translation.quests_state_cooldown));
				}
				else if (QuestWorld.getInstance().getManager(p).hasFinished(quest)) {
					lore.add("");
					lore.add(QuestWorld.translate(Translation.quests_state_completed));
				}
				view.addNavButton(quest.getID(),
						new ItemBuilder(quest.getItem()).lore(lore)
						.get(),
						event -> {
							openQuest((Player) event.getWhoClicked(), quest, back, true);
						}
				);
			}
		}
		view.build(menu, p);
		menu.openFor(p);
	}
	
	public static void openQuest(final Player p, final Quest quest, final boolean categoryBack, final boolean back) {
		QuestWorld.getSounds().QuestClick().playTo(p);
		QuestWorld.getInstance().getManager(p).update(false);
		QuestWorld.getInstance().getManager(p).updateLastEntry(quest);
		
		Menu menu = new Menu(3, QuestWorld.translate(Translation.gui_title));
		
		if (back) {
			menu.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), event -> {
				openCategory((Player) event.getWhoClicked(), quest.getCategory(), categoryBack);
			});
		}
		
		// Detect all
		menu.put(1,
				new ItemBuilder(Material.CHEST).display("&7Check all Tasks").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					PlayerManager manager = QuestWorld.getInstance().getManager(p2);
					for(Mission mission : quest.getMissions()) {
						if (!manager.hasUnlockedTask(mission)) continue;
						if (manager.getStatus(quest).equals(QuestStatus.AVAILABLE) && quest.isWorldEnabled(p2.getWorld().getName())) {
							if (manager.hasCompletedTask(mission)) continue;
							
							if(mission.getType() instanceof Manual) {
								Manual m = (Manual) mission.getType();
								int progress = m.onManual(p2, mission);
								if(progress != Manual.FAIL) {
									manager.setProgress(mission, progress);
									openQuest(p2, quest, categoryBack, back);
								}
							}
						}
					}
				}
		);
		
		if (quest.getCooldown() >= 0) {
			String cooldown = quest.getFormattedCooldown();
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
				long remaining = (QuestWorld.getInstance().getManager(p).getCooldownEnd(quest) - System.currentTimeMillis() + 59999) / 60 / 1000;
				cooldown = (remaining / 60) + "h " + (remaining % 60) + "m remaining";
			}
			menu.put(8, new ItemBuilder(Material.WATCH).display(QuestWorld.translate(Translation.quests_display_cooldown)).lore(
					"",
					"&b" + cooldown).get(),
					null
			);
		}
		
		int rewardIndex = 2;
		if (quest.getMoney() > 0 && QuestWorld.getInstance().getEconomy() != null) {
			menu.put(rewardIndex, new ItemBuilder(Material.GOLD_INGOT).display(QuestWorld.translate(Translation.quests_display_monetary)).lore(
					"",
					"&6$" + quest.getMoney()).get(),
					null
			);
			rewardIndex++;
		}
		
		if (quest.getXP() > 0) {
			menu.put(rewardIndex, new ItemBuilder(Material.EXP_BOTTLE).display(QuestWorld.translate(Translation.quests_display_exp)).lore(
					"",
					"&a" + quest.getXP() + " Level").get(),
					null
			);
			rewardIndex++;
		}
		
		ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE);
		glassPane.color(DyeColor.RED).display("&7&kSOMEWEIRDMISSION").lore("", QuestWorld.translate(Translation.task_locked));
		
		int index = 9;
		for (final Mission mission: quest.getMissions()) {
			ItemStack item = glassPane.get();
			if (QuestWorld.getInstance().getManager(p).hasUnlockedTask(mission)) {
				ItemBuilder entryItem = new ItemBuilder(mission.getDisplayItem()).display(mission.getText());
				
				if(mission.getType() instanceof Manual) {
					String label = ((Manual) mission.getType()).getLabel();
					entryItem.lore("", mission.getProgress(p), "", "&r> Click for Manual " + label);
				}
				else
					entryItem.lore("", mission.getProgress(p));

				item = entryItem.get();
			}
			
			menu.put(index, item, event -> {
				Player p2 = (Player) event.getWhoClicked();
				PlayerManager manager = QuestWorld.getInstance().getManager(p2);
				
				if (!manager.hasUnlockedTask(mission)) return;
				if (manager.getStatus(quest).equals(QuestStatus.AVAILABLE) && quest.isWorldEnabled(p2.getWorld().getName())) {
					if (manager.hasCompletedTask(mission)) return;
					
					if(mission.getType() instanceof Manual) {
						Manual m = (Manual) mission.getType();
						int progress = m.onManual(p2, mission);
						if(progress != Manual.FAIL) {
							manager.setProgress(mission, progress);
							openQuest(p2, quest, categoryBack, back);
						}
					}
				}
			});
			index++;
		}
		
		for (int i = 0; i < 9; i++) {
			if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.REWARD_CLAIMABLE)) {
				menu.put(i + 18,
						glassPane.color(DyeColor.PURPLE)
						.display(QuestWorld.translate(Translation.quests_state_reward_claim)).get(),
						event -> {
							Player p2 = (Player) event.getWhoClicked();
							quest.handoutReward(p2);
							QuestWorld.getSounds().muteNext();
							openQuest(p2, quest, categoryBack, back);
						}
				);
			}
			else if (QuestWorld.getInstance().getManager(p).getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
				menu.put(i + 18,
						glassPane.color(DyeColor.YELLOW)
						.display(QuestWorld.translate(Translation.quests_state_cooldown)).get(),
						null
				);
			}
			else {
				menu.put(i + 18,
						glassPane.color(DyeColor.GRAY)
						.display(QuestWorld.translate(Translation.quests_display_rewards)).get(),
						null);
			}
		}
		
		int slot = 27;
		for (ItemStack reward: quest.getRewards()) {
			menu.put(slot, reward, null);
			slot++;
		}
		
		menu.openFor(p);
	}

	
	/*
	 * 
	 * 			Quest Editor
	 * 
	 */
	public static void openEditor(Player p) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		final Menu menu = new Menu(6, "&3Quest Editor");
		
		ItemBuilder defaultItem = new ItemBuilder(Material.STAINED_GLASS_PANE)
				.color(DyeColor.RED).display("&7&o> New Category");

		PagedMapping view = new PagedMapping(45);
		view.touch(0); // Dummy, force a page to exist
		
		int found = 0, categoryCount = QuestWorld.getInstance().getCategories().size();
		for(int i = 0; i < view.getCapacity(); ++i) {
			if(found < categoryCount)
				view.touch(i + view.getPageCapacity());
			
			Category category = QuestWorld.getInstance().getCategory(i);
			if(category != null) {
				String[] lore = {
						"",
						"&c&oLeft Click to edit",
						"&c&oShift + Left Click to open",
						"&c&oRight Click to delete"
				};
				int quests = category.getQuests().size();
				if(quests > 0) {
					int j = 0;
					List<String> lines = new ArrayList<>();
					for(Quest q : category.getQuests()) {
						lines.add("&7- " + q.getName());
						if(++j >= 5)
							break;
					}
					if(j < quests)
						lines.add("&7&oand "+(quests-j)+" more...");
					String[] newLore = lines.toArray(new String[lines.size() + lore.length]);
					for(j = 0; j < lore.length; ++j)
						newLore[lines.size() + j] = lore[j];
					lore = newLore;
				}
				
				++found;
				view.addNavButton(i,
						new ItemBuilder(category.getItem()).lore(lore).get(),
						Buttons.onCategory(category));
			}
			else
				view.addNavButton(i, defaultItem.get(), Buttons.newCategory(i));
		}

		view.build(menu, p);
		menu.openFor(p);
	}

	public static void openCategoryQuestEditor(Player p, final Category category) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		final Menu menu = new Menu(6, "&3Quest Editor");

		menu.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), event -> {
			openEditor((Player) event.getWhoClicked());
		});
		
		ItemBuilder defaultItem = new ItemBuilder(Material.STAINED_GLASS_PANE)
				.color(DyeColor.RED).display("&7&o> New Quest");
		
		PagedMapping view = new PagedMapping(45);
		view.touch(0); // Dummy, force a page to exist
		
		int found = 0, questCount = category.getQuests().size();
		for (int i = 0; i < view.getCapacity(); ++i) {
			if(found < questCount)
				view.touch(i + view.getPageCapacity());
			
			Quest quest = category.getQuest(i);
			if (quest != null) {
				int missions = quest.getMissions().size();
				String[] lore = {
					"",
					"&c&oLeft Click to edit",
					"&c&oRight Click to delete"
				};
				
				if(missions > 0) {
					int j = 0;
					List<String> lines = new ArrayList<>();
					for(Mission m : quest.getMissions()) {
						lines.add("&7- " + m.getText());
						if(++j >= 5)
							break;
					}
					if(j < missions)
						lines.add("&7&oand "+(missions-j)+" more...");
					String[] newLore = lines.toArray(new String[lines.size() + lore.length]);
					for(j = 0; j < lore.length; ++j)
						newLore[lines.size() + j] = lore[j];
					lore = newLore;
				}
				
				++found;
				view.addNavButton(i,
						new ItemBuilder(quest.getItem()).lore(lore).get(),
						Buttons.onQuest(quest));
			}
			else
				view.addNavButton(i, defaultItem.getNew(), Buttons.newQuest(category.getID(), i));
		}
		view.build(menu, p);
		menu.openFor(p);
	}

	public static void openCategoryEditor(Player p, final Category category) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		final Menu menu = new Menu(2, "&3Quest Editor");
		CategoryChange changes = new CategoryChange(category);
		
		menu.put(0,  Proto.MAP_BACK.getItem(), event -> {
			openEditor((Player) event.getWhoClicked());
		});
		
		menu.put(9,
				new ItemBuilder(category.getItem()).lore(
						"",
						"&e> Click to change the Item to",
						"&ethe Item you are currently holding").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					ItemStack hand = PlayerTools.getActiveHandItem(p2);
					if (hand != null) {
						changes.setItem(hand);
						if(changes.sendEvent())
							changes.apply();
						openCategoryEditor(p2, category);
				}
		});
		
		menu.put(10,
				new ItemBuilder(Material.NAME_TAG)
				.display(category.getName())
				.lore("", "&e> Click to change the Name").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					PlayerTools.promptInput(p2, new SinglePrompt(
							PlayerTools.makeTranslation(true, Translation.category_namechange, category.getName()),
							(c,s) -> {
								changes.setName(s);
								if(changes.sendEvent()) {
									String oldName = category.getName();
									changes.apply();
									PlayerTools.sendTranslation(p2, true, Translation.category_nameset, s, oldName);
								}
								
								QuestBook.openCategoryEditor(p2, category);
								return true;
							}
					));

					PlayerTools.closeInventoryWithEvent(p2);
				}
		);
		
		menu.put(11,
				new ItemBuilder(Material.BOOK_AND_QUILL).display("&7Quest Requirement:").lore(
						"",
						(category.getParent() != null ? "&r" + category.getParent().getName(): "&7&oNone"),
						"",
						"&rLeft Click: &eChange Quest Requirement",
						"&rRight Click: &eRemove Quest Requirement").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					if (event.isRightClick()) {
						changes.setParent(null);
						if(changes.sendEvent())
							changes.apply();
						openCategoryEditor(p2, category);
					}
					else {
						QuestWorld.getInstance().getManager(p2).putPage(0);
						QBDialogue.openQuestRequirementChooser(p2, category);
					}
				}
		);
		
		menu.put(12,
				new ItemBuilder(Material.NAME_TAG)
				.display("&r" + (category.getPermission().equals("") ? "None": category.getPermission())).lore(
						"",
						"&e> Click to change the rquired Permission Node").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					PlayerTools.promptInput(p2, new SinglePrompt(
							PlayerTools.makeTranslation(true, Translation.category_permchange, category.getName(), category.getPermission()),
							(c,s) -> {
								String permission = s.equalsIgnoreCase("none") ? "": s;
								changes.setPermission(permission);
								if(changes.sendEvent()) {
									String oldPerm = category.getPermission();
									changes.apply();
									PlayerTools.sendTranslation(p2, true, Translation.category_permset, category.getName(), s, oldPerm);
								}
								
								QuestBook.openCategoryEditor(p2, category);
								return true;
							}
					));
					
					PlayerTools.closeInventoryWithEvent(p2);
				}
		);
		
		menu.put(13,
				new ItemBuilder(Material.GOLDEN_CARROT)
				.display("&rShow in Quest Book: " + (!category.isHidden() ? "&2&l\u2714": "&4&l\u2718")).lore(
						"",
						"&e> Click to change whether this Category",
						"&ewill appear in the Quest Book").get(),
				event -> {
					changes.setHidden(!category.isHidden());
					if(changes.sendEvent())
						changes.apply();
					openCategoryEditor((Player) event.getWhoClicked(), category);
				}
		);
		
		menu.put(14,
				new ItemBuilder(Material.GRASS).display("&7World Blacklist").lore(
						"",
						"&e> Click to configure in which Worlds",
						"&ethis Category is enabled").get(),
				event -> {
					openWorldEditor((Player) event.getWhoClicked(), category);
				}
		);
		
		menu.put(17,
				ItemBuilder.Proto.RED_WOOL.get().display("&4Delete Database").lore(
						"",
						"&rThis is going to delete the Database",
						"&rof all Quests inside this Category",
						"&rand will clear all Player's Progress associated",
						"&rwith those Quests.").get(),
				event -> {
					for (Quest quest: category.getQuests()) {
						PlayerManager.clearAllQuestData(quest);
					}
					QuestWorld.getSounds().DestructiveClick().playTo((Player) event.getWhoClicked());
				}
		);
		
		menu.openFor(p);
	}

	public static void openQuestEditor(Player p, final Quest quest) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		final Menu menu = new Menu(6, "&3Quest Editor");
		QuestChange changes = new QuestChange(quest);
		
		menu.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), event -> {
			openCategoryQuestEditor((Player) event.getWhoClicked(), quest.getCategory());
		});
		
		menu.put(9,
				new ItemBuilder(quest.getItem()).lore(
						"",
						"&e> Click to change the Item to",
						"&ethe Item you are currently holding").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					ItemStack mainItem = p2.getInventory().getItemInMainHand();
					if (mainItem != null) {
						changes.setItem(mainItem);
						if(changes.sendEvent())
							changes.apply();
						
						openQuestEditor(p2, quest);
					}
				}
		);
		
		menu.put(10,
				new ItemBuilder(Material.NAME_TAG)
				.display(quest.getName())
				.lore("", "&e> Click to change the Name").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					PlayerTools.promptInput(p2, new SinglePrompt(
							PlayerTools.makeTranslation(true, Translation.quest_namechange, quest.getName()),
							(c,s) -> {
								changes.setName(s);
								if(changes.sendEvent()) {
									String oldName = quest.getName();
									changes.apply();
									PlayerTools.sendTranslation(p2, true, Translation.quest_nameset, s, oldName);
								}

								QuestBook.openQuestEditor(p2, quest);
								return true;
							}
					));

					PlayerTools.closeInventoryWithEvent(p2);
				}
		);
		
		menu.put(11,
				new ItemBuilder(Material.CHEST)
				.display("&rRewards &7(Item)").lore(
						"",
						"&e> Click to change the Rewards",
						"&eto be the Items in your Hotbar").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					changes.setItemRewards(p2);
					if(changes.sendEvent())
						changes.apply();
					
					openQuestEditor(p2, quest);
				}
		);
		
		menu.put(12,
				new ItemBuilder(Material.WATCH)
				.display("&7Cooldown: &b" + quest.getFormattedCooldown()).lore(
						"",
						"&rLeft Click: &e+1m",
						"&rRight Click: &e-1m",
						"&rShift + Left Click: &e+1h",
						"&rShift + Right Click: &e-1h").get(),
				event -> {
					long cooldown = quest.getCooldown();
					long delta = event.isShiftClick() ? 60: 1;
					if (event.isRightClick()) delta = -delta;

					// Force a step at 0, so you can't jump from 59 -> -1 or -1 -> 59
					if(cooldown + delta < 0) {
						if(cooldown <= 0) 
							cooldown = -1;
						else
							cooldown = 0;
					}
					else if(cooldown == -1)
						cooldown = 0;
					else
						cooldown += delta;
					
					changes.setCooldown(cooldown);
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor((Player) event.getWhoClicked(), quest);
				}
		);
		
		if (QuestWorld.getInstance().getEconomy() != null) {
			menu.put(13,
					new ItemBuilder(Material.GOLD_INGOT)
					.display("&7Monetary Reward: &6$" + quest.getMoney()).lore(
							"",
							"&rLeft Click: &e+1",
							"&rRight Click: &e-1",
							"&rShift + Left Click: &e+100",
							"&rShift + Right Click: &e-100").get(),
					event -> {
						int money = quest.getMoney();
						if (event.isRightClick()) money = money - (event.isShiftClick() ? 100: 1);
						else money = money + (event.isShiftClick() ? 100: 1);
						if (money < 0) money = 0;
						changes.setMoney(money);
						if(changes.sendEvent())
							changes.apply();
						openQuestEditor((Player) event.getWhoClicked(), quest);
					}
			);
		}
		
		menu.put(14,
				new ItemBuilder(Material.EXP_BOTTLE)
				.display("&7XP Reward: &b" + quest.getXP() + " Level").lore(
						"",
						"&rLeft Click: &e+1",
						"&rRight Click: &e-1",
						"&rShift + Left Click: &e+10",
						"&rShift + Right Click: &e-10").get(),
				event -> {
					int xp = quest.getXP();
					if (event.isRightClick()) xp = xp - (event.isShiftClick() ? 10: 1);
					else xp = xp + (event.isShiftClick() ? 10: 1);
					if (xp < 0) xp = 0;
					changes.setXP(xp);
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor((Player) event.getWhoClicked(), quest);
				}
		);
		
		menu.put(15,
				new ItemBuilder(Material.BOOK_AND_QUILL)
				.display("&7Quest Requirement:").lore(
						"",
						(quest.getParent() != null ? "&r" + quest.getParent().getName(): "&7&oNone"),
						"",
						"&rLeft Click: &eChange Quest Requirement",
						"&rRight Click: &eRemove Quest Requirement").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					if (event.isRightClick()) {
						changes.setParent(null);
						if(changes.sendEvent())
							changes.apply();
						openQuestEditor(p2, quest);
					}
					else {
						QuestWorld.getInstance().getManager(p2).putPage(0);
						QBDialogue.openQuestRequirementChooser(p2, quest);
					}
				}
		);
		
		menu.put(16,
				new ItemBuilder(Material.COMMAND)
				.display("&7Commands executed upon Completion").lore(
						"",
						"&rLeft Click: &eOpen Command Editor").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					PlayerTools.closeInventoryWithEvent(p2);
					QBDialogue.openCommandEditor(p2, quest);
				}
		);
		
		menu.put(17, new ItemBuilder(Material.NAME_TAG)
				.display("&r" + (quest.getPermission().equals("") ? "None": quest.getPermission())).lore(
						"",
						"&e> Click to change the required Permission Node").get(),
				event -> {
					Player p2 = (Player) event.getWhoClicked();
					PlayerTools.promptInput(p2, new SinglePrompt(
							PlayerTools.makeTranslation(true, Translation.quest_permchange, quest.getName(), quest.getPermission()),
							(c,s) -> {
								String permission = s.equalsIgnoreCase("none") ? "": s;
								changes.setPermission(permission);
								if(changes.sendEvent()) {
									String oldPerm = quest.getPermission();
									changes.apply();
									PlayerTools.sendTranslation(p2, true, Translation.quest_permset, quest.getName(), s, oldPerm);
								}

								QuestBook.openQuestEditor(p2, quest);
								return true;
							}
					));

					PlayerTools.closeInventoryWithEvent(p2);
				}
		);
		
		menu.put(18,
				new ItemBuilder(Material.FIREWORK)
				.display("&rParty Support: " + (quest.supportsParties() ? "&2&l\u2714": "&4&l\u2718")).lore(
						"",
						"&e> Click to change whether this Quest can be done in Parties or not").get(),
				event -> {
					changes.setPartySupport(quest.supportsParties());
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor((Player) event.getWhoClicked(), quest);
				}
		);
		
		menu.put(19,
				new ItemBuilder(Material.COMMAND)
				.display("&rOrdered Completion Mode: " + (quest.isOrdered() ? "&2&l\u2714": "&4&l\u2718")).lore(
						"",
						"&e> Click to change whether this Quest's Tasks",
						"&ehave to be done in the Order they are arranged").get(),
				event -> {
					changes.setOrdered(!quest.isOrdered());
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor((Player) event.getWhoClicked(), quest);
				}
		);
		
		menu.put(20,
				new ItemBuilder(Material.CHEST)
				.display("&rAuto-Claim Rewards: " + (quest.isAutoClaiming() ? "&2&l\u2714": "&4&l\u2718")).lore(
						"",
						"&e> Click to change whether this Quest's Rewards",
						"&ewill be automatically given or have to be",
						"&eclaimed manually").get(),
				event -> {
					changes.setAutoClaim(!changes.isAutoClaiming());
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor((Player) event.getWhoClicked(), quest);
				}
		);
		
		menu.put(21,
				new ItemBuilder(Material.GRASS)
				.display("&7World Blacklist").lore(
						"",
						"&e> Click to configure in which Worlds",
						"&ethis Quest is able to be completed").get(),
				event -> {
					openWorldEditor((Player) event.getWhoClicked(), quest);
				}
		);
		
		String wtfString = "&rMinimal Party Size: " + (quest.getPartySize() < 1 ? "&4Players aren't allowed be in a Party": (quest.getPartySize() == 1 ? ("&ePlayers can but don't have to be in a Party") : ("&aPlayers need to be in a Party of " + quest.getPartySize() + " or more")));
		menu.put(22,
				new ItemBuilder(Material.FIREWORK)
				.display(wtfString).lore(
						"",
						"&eChange the min. Amount of Players in",
						"&ea Party needed to start this Quest",
						"",
						"&r1 = &7Players can but don't have to be in a Party",
						"&r0 = &7Players aren't allowed to be in a Party",
						"",
						"&rLeft Click: &e+1",
						"&rRight Click: &e-1").get(),
				event -> {
					int size = quest.getPartySize();
					if (event.isRightClick()) size--;
					else size++;
					if (size < 0) size = 0;
					changes.setPartySize(size);
					if(changes.sendEvent())
						changes.apply();
					openQuestEditor((Player) event.getWhoClicked(), quest);
				}
		);
		
		menu.put(26,
				ItemBuilder.Proto.RED_WOOL.get().display("&4Delete Database").lore(
						"",
						"&rThis is going to delete this Quest's Database",
						"&rand will clear all Player's Progress associated",
						"&rwith this Quest.").get(),
				event -> {
					PlayerManager.clearAllQuestData(quest);
					QuestWorld.getSounds().DestructiveClick().playTo((Player) event.getWhoClicked());
				}
		);
		
		int index = 36;
		for (ItemStack reward: quest.getRewards()) {
			menu.put(index, reward, null);
			index++;
		}
		
		for (int i = 0; i < 9; i++) {
			final Mission mission = quest.getMission(i);
			if (mission == null) {
				menu.put(45 + i,
						new ItemBuilder(Material.PAPER).display("&7&o> New Task").get(),
						event -> {
							changes.addMission(new Mission(quest, String.valueOf(event.getSlot() + 9), MissionType.valueOf("SUBMIT"), EntityType.PLAYER, "", new ItemStack(Material.STONE), p.getLocation().getBlock().getLocation(), 1, null, 0, false, 0, true, "Hey there! Do this Quest."));
							if(changes.sendEvent())
								changes.apply();
							openQuestEditor((Player) event.getWhoClicked(), quest);
						});
			}
			else {
				menu.put(45 + i,
						new ItemBuilder(Material.BOOK)
						.display(mission.getText()).lore(
								"",
								"&c&oLeft Click to edit",
								"&c&oRight Click to delete").get(),
						event -> {
							Player p2 = (Player) event.getWhoClicked();
							if (event.isRightClick()) QBDialogue.openDeletionConfirmation(p2, mission);
							else openQuestMissionEditor(p2, mission);
						}
				);
			}
		}
		
		menu.openFor(p);
	}

	public static void openWorldEditor(Player p, final Quest quest) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		final Menu menu = new Menu(2, "&3Quest Editor");
		
		menu.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), event -> {
			openQuestEditor((Player) event.getWhoClicked(), quest);
		});
		
		int index = 9;
		for (final World world: Bukkit.getWorlds()) {
			menu.put(index,
					new ItemBuilder(Material.GRASS)
					.display("&r" + world.getName() + ": " + (quest.isWorldEnabled(world.getName()) ? "&2&l\u2714": "&4&l\u2718")).get(),
					event -> {
						QuestChange changes = new QuestChange(quest);
						changes.toggleWorld(world.getName());
						if(changes.sendEvent())
							changes.apply();
						openWorldEditor((Player) event.getWhoClicked(), quest);
					}
			);
			index++;
		}
		
		menu.openFor(p);
	}

	public static void openWorldEditor(Player p, final Category category) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		final Menu menu = new Menu(2, "&3Quest Editor");
		
		menu.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), event -> {
			openCategoryEditor((Player) event.getWhoClicked(), category);
		});
		
		int index = 9;
		for (final World world: Bukkit.getWorlds()) {
			menu.put(index,
					new ItemBuilder(Material.GRASS)
					.display("&r" + world.getName() + ": " + (category.isWorldEnabled(world.getName()) ? "&2&l\u2714": "&4&l\u2718")).get(),
					event -> {
						CategoryChange changes = new CategoryChange(category);
						changes.toggleWorld(world.getName());
						if(changes.sendEvent())
							changes.apply();
						
						openWorldEditor((Player) event.getWhoClicked(), category);
					}
			);
			index++;
		}
		
		menu.openFor(p);
	}

	public static void openQuestMissionEditor(Player p, final Mission mission) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		Menu menu2 = new Menu(3, "&3Quest Editor");
		MissionChange changes = new MissionChange(mission);

		menu2.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), e -> {
			openQuestEditor(p, mission.getQuest());
		});
		
		// Mission types now handle their own menu data!
		mission.getType().buildMenu(changes, menu2);
		
		ItemStack missionSelector = new ItemBuilder(mission.getType().getSelectorItem())
				.display("&7" + mission.getType().toString())
				.lore(
						"",
						"&e> Click to change the Mission Type").get();
		
		menu2.put(9, missionSelector, e -> {
			openMissionSelector(p, changes.getSource());
		});
		
		menu2.openFor(p);
	}

	public static void openMissionSelector(Player p, Mission mission) {
		QuestWorld.getSounds().EditorClick().playTo(p);
		
		final Menu menu = new Menu(2, Text.colorize("&3Mission Selector: " + mission.getQuest().getName()));
		MissionChange changes = new MissionChange(mission);

		menu.put(0, ItemBuilder.Proto.MAP_BACK.getItem(), event -> {
			openQuestMissionEditor((Player) event.getWhoClicked(), mission);
		});
		
		PagedMapping view = new PagedMapping(45, 9);
		int i = 0;
		for(MissionType type : QuestWorld.getInstance().getMissionTypes().values()) {
			String name = Text.niceName(type.getName());
			view.addItem(i, new ItemBuilder(type.getSelectorItem()).display("&f" + name).get());
			view.addButton(i, event -> {
				changes.setType(type);
				MissionButton.apply(event, changes);
			});
			++i;
		}
		view.setBackButton(event -> openQuestMissionEditor(p, mission));
		view.build(menu, p);
		
		menu.openFor(p);
	}
}
