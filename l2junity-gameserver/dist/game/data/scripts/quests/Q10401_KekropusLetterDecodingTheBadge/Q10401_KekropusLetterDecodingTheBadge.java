/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q10401_KekropusLetterDecodingTheBadge;

import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import quests.LetterQuest;

/**
 * Kekropus' Letter: Decoding the Badge (10401)
 * @author St3eT
 */
public final class Q10401_KekropusLetterDecodingTheBadge extends LetterQuest
{
	// NPCs
	private static final int PATERSON = 33864;
	private static final int EBLUNE = 33865;
	private static final int INVISIBLE_NPC = 19543;
	// Items
	private static final int SOE_TOWN_OF_ADEN = 37115; // Scroll of Escape: Town of Aden
	private static final int SOE_FORSAKEN_PLAINS = 37028; // Scroll of Escape: Forsaken Plains
	private static final int EAB = 948; // Scroll: Enchant Armor (B-grade)
	private static final int STEEL_COIN = 37045; // Steel Door Guild Coin
	// Location
	private static final Location TELEPORT_LOC = new Location(147540, 24661, -1984);
	// Misc
	private static final int MIN_LEVEL = 58;
	private static final int MAX_LEVEL = 60;
	
	public Q10401_KekropusLetterDecodingTheBadge()
	{
		super(10401, Q10401_KekropusLetterDecodingTheBadge.class.getSimpleName(), "Kekropus' Letter: Decoding the Badge");
		addTalkId(PATERSON, EBLUNE);
		addSeeCreatureId(INVISIBLE_NPC);
		
		setIsErtheiaQuest(false);
		setLevel(MIN_LEVEL, MAX_LEVEL);
		setStartQuestSound("Npcdialog1.kekrops_quest_4");
		setStartLocation(SOE_TOWN_OF_ADEN, TELEPORT_LOC);
		registerQuestItems(SOE_TOWN_OF_ADEN, SOE_FORSAKEN_PLAINS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "33864-02.html":
			{
				htmltext = event;
				break;
			}
			case "33864-03.html":
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
					giveItems(player, SOE_FORSAKEN_PLAINS, 1);
					htmltext = event;
				}
				break;
			}
			case "33865-02.html":
			{
				if (st.isCond(2))
				{
					st.exitQuest(false, true);
					giveItems(player, EAB, 5);
					giveItems(player, STEEL_COIN, 30);
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExpAndSp(player, 731_010, 175);
					}
					showOnScreenMsg(player, NpcStringId.GROW_STRONGER_HERE_UNTIL_YOU_RECEIVE_THE_NEXT_LETTER_FROM_KEKROPUS_AT_LV_61, ExShowScreenMessage.TOP_CENTER, 6000);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, false);
		
		if (st == null)
		{
			return htmltext;
		}
		
		if (st.isStarted())
		{
			if (st.isCond(1) && (npc.getId() == PATERSON))
			{
				htmltext = "33864-01.html";
			}
			else if (st.isCond(2))
			{
				htmltext = npc.getId() == PATERSON ? "33864-04.html" : "33865-01.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		if (creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			final QuestState st = getQuestState(player, false);
			
			if ((st != null) && st.isCond(2))
			{
				showOnScreenMsg(player, NpcStringId.FORSAKEN_PLAINS_IA_A_GOOD_HUNTING_ZONE_FOR_LV_58_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
			}
		}
		return super.onSeeCreature(npc, creature, isSummon);
	}
}