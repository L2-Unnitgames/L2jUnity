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
package quests.Q10399_TheAlphabetOfTheGiants;

import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import quests.Q10398_ASuspiciousBadge.Q10398_ASuspiciousBadge;

/**
 * The Alphabet of the Giants (10399)
 * @author St3eT
 */
public final class Q10399_TheAlphabetOfTheGiants extends Quest
{
	// NPCs
	private static final int BACON = 33846;
	private static final int[] MONSTERS =
	{
		23309, // Corpse Looter Stakato
		23310, // Lesser Laikel
	};
	// Items
	private static final int TABLET = 36667; // Giant's Alphabet
	private static final int EAB = 948; // Scroll: Enchant Armor (B-grade)
	private static final int STEEL_COIN = 37045; // Steel Door Guild Coin
	// Misc
	private static final int MIN_LEVEL = 52;
	private static final int MAX_LEVEL = 58;
	
	public Q10399_TheAlphabetOfTheGiants()
	{
		super(10399, Q10399_TheAlphabetOfTheGiants.class.getSimpleName(), "The Alphabet of the Giants");
		addStartNpc(BACON);
		addTalkId(BACON);
		addKillId(MONSTERS);
		registerQuestItems(TABLET);
		addCondNotRace(Race.ERTHEIA, "33846-08.html");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33846-07.htm");
		addCondCompletedQuest(Q10398_ASuspiciousBadge.class.getSimpleName(), "33846-07.htm");
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
			case "33846-02.htm":
			{
				htmltext = event;
				break;
			}
			case "33846-03.htm":
			{
				st.startQuest();
				st.setQuestLocation(NpcStringId.SEA_OF_SPORES_LV_52);
				htmltext = event;
				break;
			}
			case "33846-06.html":
			{
				if (st.isCond(2))
				{
					st.exitQuest(false, true);
					giveItems(player, EAB, 5);
					giveItems(player, STEEL_COIN, 37);
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExpAndSp(player, 3_811_500, 914);
					}
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
		final QuestState st = getQuestState(player, true);
		
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				htmltext = "33846-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1))
				{
					htmltext = "33846-04.html";
				}
				else if (st.isCond(2))
				{
					htmltext = "33846-05.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState st = getQuestState(killer, false);
		
		if ((st != null) && st.isStarted() && st.isCond(1))
		{
			if (giveItemRandomly(killer, npc, TABLET, 1, 20, 1, true))
			{
				st.setCond(2);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
}