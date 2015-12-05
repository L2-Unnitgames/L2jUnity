/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q10746_SeeTheWorld;

import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

/**
 * See the World (10746)
 * @author Sdw
 */
public final class Q10746_SeeTheWorld extends Quest
{
	// NPCs
	private static final int KARLA = 33933;
	private static final int ASTIEL = 33948;
	private static final int LEVIAN = 30037;
	// Items
	private static final ItemHolder EMISSARY_SUPPORT_BOX_WARRIOR = new ItemHolder(40264, 1);
	private static final ItemHolder EMISSARY_SUPPORT_BOX_MAGE = new ItemHolder(40265, 1);
	// Location
	private static final Location GLUDIN_VILLAGE = new Location(-80684, 149770, -3040);
	// Misc
	private static final int MIN_LEVEL = 19;
	private static final int MAX_LEVEL = 25;
	
	public Q10746_SeeTheWorld()
	{
		super(10746);
		addStartNpc(KARLA);
		addTalkId(KARLA, ASTIEL, LEVIAN);
		
		addCondRace(Race.ERTHEIA, "");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33933-00.htm");
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
			case "33933-02.htm":
			{
				qs.startQuest();
				break;
			}
			case "33948-03.html":
			{
				if (qs.isCond(2))
				{
					player.teleToLocation(GLUDIN_VILLAGE);
				}
				break;
			}
			default:
				htmltext = null;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player, boolean isSimulated)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (npc.getId())
		{
			case KARLA:
			{
				switch (qs.getState())
				{
					case State.CREATED:
						htmltext = "33933-01.htm";
						break;
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "33933-03.html";
						}
						break;
					}
					case State.COMPLETED:
						htmltext = getAlreadyCompletedMsg(player);
						break;
				}
				break;
			}
			case ASTIEL:
			{
				if (qs.isStarted())
				{
					if (qs.isCond(1))
					{
						if (!isSimulated)
						{
							qs.setCond(2, true);
						}
						htmltext = "33948-01.html";
					}
					else if (qs.isCond(2))
					{
						htmltext = "33948-02.html";
					}
				}
				break;
			}
			case LEVIAN:
			{
				if (qs.isStarted() && qs.isCond(2))
				{
					if (!isSimulated)
					{
						giveAdena(player, 43000, true);
						addExpAndSp(player, 53422, 5);
						if (player.isMageClass())
						{
							giveItems(player, EMISSARY_SUPPORT_BOX_MAGE);
						}
						else
						{
							giveItems(player, EMISSARY_SUPPORT_BOX_WARRIOR);
						}
						showOnScreenMsg(player, NpcStringId.CHECK_YOUR_EQUIPMENT_IN_YOUR_INVENTORY, ExShowScreenMessage.TOP_CENTER, 10000);
						qs.exitQuest(false, true);
					}
					htmltext = "30037-01.html";
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
		}
		return htmltext;
	}
}
