package me.mrCookieSlime.QuestWorld.api;

public enum Translation implements Translator {
	default_category("defaults.category.name"),
	default_quest   ("defaults.quest.name"),
	default_mission ("defaults.mission.name"),
	default_prefix  ("defaults.prefix"),
	
	book_display("book.display"),
	book_lore   ("book.lore"),
	
	nav_display("navigation.display", "page",     "pages"),
	nav_lore(   "navigation.lore",    "pre-next", "pre-last"),
	nav_next(   "navigation.prefix-next"),
	nav_prev(   "navigation.prefix-last"),
	nav_nextbad("navigation.prefix-next-inactive"),
	nav_prevbad("navigation.prefix-last-inactive"),
	
	category_created   ("editor.category.created",     "name"),
	category_deleted   ("editor.category.deleted",     "name"),
	category_namechange("editor.category.name-change", "name"),
	category_nameset   ("editor.category.name-set",    "name", "name_old"),
	category_permchange("editor.category.perm-change", "name", "perm"),
	category_permset   ("editor.category.perm-set",    "name", "perm", "perm_old"),
	
	quest_created   ("editor.quest.created",     "name"),
	quest_deleted   ("editor.quest.deleted",     "name"),
	quest_namechange("editor.quest.name-change", "name"),
	quest_nameset   ("editor.quest.name-set",    "name", "name_old"),
	quest_permchange("editor.quest.perm-change", "name", "perm"),
	quest_permset   ("editor.quest.perm-set",    "name", "perm", "perm_old"),
	
	// TODO better names, PH
	mission_await("editor.await-mission-name"),
	mission_name("editor.edit-mission-name"),
	
	dialog_add("editor.add-dialogue"),
	dialog_set("editor.set-dialogue", "path"),
	
	mission_desc("editor.misssion-description"),
	
	killmission_rename("editor.rename-kill-mission"),
	killtype_rename("editor.renamed-kill-type"),
	location_rename("editor.renamed-location"),
	// End PH
	
	notify_timefail    ("notifications.task-failed-timeframe",  "quest"),
	notify_timestart   ("notifications.task-timeframe-started", "task", "time"),
	notify_completetask("notifications.task-completed",         "quest"),

	party_errorfull  ("party.full",       "max"),
	party_errorabsent("party.not-online", "name"),
	party_errormember("party.already",    "name"),
	party_playerpick ("party.invite"),
	party_playeradd  ("party.invited",    "name"),
	party_playerjoin ("party.join",       "name"),
	party_playerkick ("party.kicked",     "name"),
	party_groupinvite("party.invitation", "name"),
	party_groupjoin  ("party.joined",     "name"),

	// TODO
	// This is hacky, look again when less tired
	gui_title(0),
	gui_party(0),
	button_open(0),
	button_back_party(0),
	button_back_quests(0),
	button_back_general(0),
	quests_locked(0),
	quests_locked_in_world("quests.locked-in-world"),
	quests_tasks_completed("quests.tasks_completed"),
	quests_state_cooldown(0),
	quests_state_completed(0),
	quests_state_reward_claimable("quests.state.reward_claimable"),
	quests_state_reward_claim("quests.state.reward_claim"),
	quests_display_cooldown(0),
	quests_display_monetary(0),
	quests_display_exp(0),
	quests_display_rewards(0),
	category_desc_total(0),
	category_desc_completed(0),
	category_desc_available(0),
	category_desc_cooldown(0),
	category_desc_claimable_reward("category.desc.claimable_reward"),
	task_locked(0),
	;
	
	private String path;
	private String[] placeholders;
	Translation(String path, String... placeholders) {
		this.path = path;
		this.placeholders = placeholders;
		for(int i = 0; i < this.placeholders.length; ++i)
			this.placeholders[i] = "%" + this.placeholders[i] + "%"; 
	}
	
	Translation(int d, String... placeholders) {
		path = name().replace('_', '.');
		this.placeholders = placeholders;
		for(int i = 0; i < this.placeholders.length; ++i)
			this.placeholders[i] = "%" + this.placeholders[i] + "%"; 
	}
	
	@Override
	public String path() {
		return path;
	}
	
	@Override
	public String[] placeholders() {
		return placeholders;
	}
	
	@Override
	public String toString() {
		String result = path;
		if(placeholders.length > 0) {
			result += "[";
			for(String s : placeholders)
				result += s + ", ";
			result = result.substring(0, result.length()-2) + "]";
		}
		
		return result;
	}
}
