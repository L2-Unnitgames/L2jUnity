/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q10774_LettersFromTheQueenCrumaTowerPart2;

import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import quests.LetterQuest;

/**
 * Letters from the Queen: Cruma Tower, Part 2 (10774)
 * @author malyelfik
 */
public final class Q10774_LettersFromTheQueenCrumaTowerPart2 extends LetterQuest
{
	// NPCs
	private static final int SYLVAIN = 30070;
	private static final int ROMBEL = 30487;
	// Items
	private static final int SOE_DION_TOWN = 39595;
	private static final int SOE_CRUMA_TOWER = 39596;
	private static final int ENCHANT_ARMOR_C = 952;
	private static final int STEEL_DOOR_GUILD_COIN = 37045;
	// Location
	private static final Location TELEPORT_LOC = new Location(16014, 142326, -2688);
	// Misc
	private static final int MIN_LEVEL = 46;
	private static final int MAX_LEVEL = 50;
	
	public Q10774_LettersFromTheQueenCrumaTowerPart2()
	{
		super(10774, Q10774_LettersFromTheQueenCrumaTowerPart2.class.getSimpleName(), "Letters from the Queen: Cruma Tower, Part 2");
		addTalkId(SYLVAIN, ROMBEL);
		
		setIsErtheiaQuest(true);
		setLevel(MIN_LEVEL, MAX_LEVEL);
		setStartLocation(SOE_DION_TOWN, TELEPORT_LOC);
		setStartQuestSound("Npcdialog1.serenia_quest_4");
		registerQuestItems(SOE_DION_TOWN, SOE_CRUMA_TOWER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "30070-02.html":
			case "30487-02.html":
				break;
			case "30070-03.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					giveItems(player, SOE_CRUMA_TOWER, 1);
					showOnScreenMsg(player, NpcStringId.TRY_USING_THE_TELEPORT_SCROLL_SYLVAIN_GAVE_YOU_TO_GO_TO_CRUMA_TOWER2, ExShowScreenMessage.TOP_CENTER, 8000);
				}
				break;
			}
			case "30487-03.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, ENCHANT_ARMOR_C, 2);
					giveItems(player, STEEL_DOOR_GUILD_COIN, 11);
					addExpAndSp(player, 483840, 116);
					showOnScreenMsg(player, NpcStringId.GROW_STRONGER_HERE_UNTIL_YOU_RECEIVE_THE_NEXT_LETTER_FROM_QUEEN_NAVARI_AT_LV_52, ExShowScreenMessage.TOP_CENTER, 8000);
					qs.exitQuest(false, true);
				}
				break;
			}
			default:
				htmltext = null;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = getNoQuestMsg(player);
		if (qs == null)
		{
			return htmltext;
		}
		
		if (qs.isStarted())
		{
			if (npc.getId() == SYLVAIN)
			{
				htmltext = (qs.isCond(1)) ? "30070-01.html" : "30070-04.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = "30487-01.html";
			}
		}
		return htmltext;
	}
}
