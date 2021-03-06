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
package quests.Q10337_SakumsImpact;

import java.util.HashSet;
import java.util.Set;

import org.l2junity.gameserver.enums.QuestSound;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.NpcLogListHolder;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;

/**
 * Sakum's Impact (10337)
 * @author St3eT
 */
public final class Q10337_SakumsImpact extends Quest
{
	// NPCs
	private static final int ADVENTURE_GUILDSMAN = 31795;
	private static final int SILVAN = 33178;
	private static final int LEF = 33510;
	private static final int SKELETON_WARRIOR = 23022;
	private static final int RUIN_IMP = 20506;
	private static final int RUIN_BAT = 23023;
	private static final int BAT = 20411;
	// Misc
	private static final int MIN_LEVEL = 28;
	private static final int MAX_LEVEL = 40;
	
	public Q10337_SakumsImpact()
	{
		super(10337);
		addStartNpc(ADVENTURE_GUILDSMAN);
		addTalkId(ADVENTURE_GUILDSMAN, SILVAN, LEF);
		addKillId(SKELETON_WARRIOR, RUIN_IMP, BAT, RUIN_BAT);
		addCondNotRace(Race.ERTHEIA, "");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "");
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
			case "31795-02.htm":
			case "33178-02.htm":
			{
				htmltext = event;
				break;
			}
			case "31795-03.htm":
			{
				st.startQuest();
				htmltext = event;
				break;
			}
			case "33178-03.htm":
			{
				if (st.isCond(1))
				{
					st.setCond(2);
					htmltext = event;
				}
				break;
			}
			case "33510-04.htm":
			{
				if (st.isCond(3))
				{
					giveAdena(player, 1030, true);
					addExpAndSp(player, 650000, 156);
					st.exitQuest(false, true);
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
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == ADVENTURE_GUILDSMAN)
				{
					htmltext = "31795-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (st.getCond())
				{
					case 1:
					{
						switch (npc.getId())
						{
							case ADVENTURE_GUILDSMAN:
								htmltext = "31795-04.htm";
								break;
							case SILVAN:
								htmltext = "33178-01.htm";
								break;
							case LEF:
								htmltext = "33510-01.htm";
								break;
						}
						break;
					}
					case 2:
					{
						switch (npc.getId())
						{
							case ADVENTURE_GUILDSMAN:
								htmltext = "31795-04.htm";
								break;
							case SILVAN:
								htmltext = "33178-04.htm";
								break;
							case LEF:
								htmltext = "33510-02.htm";
								break;
						}
						break;
					}
					case 3:
					{
						switch (npc.getId())
						{
							case ADVENTURE_GUILDSMAN:
								htmltext = "31795-04.htm";
								break;
							case SILVAN:
								htmltext = "33178-05.htm";
								break;
							case LEF:
								htmltext = "33510-03.htm";
								break;
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				switch (npc.getId())
				{
					case ADVENTURE_GUILDSMAN:
						htmltext = "31795-05.htm";
						break;
					case SILVAN:
						htmltext = "33178-06.htm";
						break;
					case LEF:
						htmltext = "33510-05.htm";
						break;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState st = getQuestState(killer, false);
		
		if ((st != null) && st.isStarted() && st.isCond(2))
		{
			int killedWarriors = st.getInt("killed_" + SKELETON_WARRIOR);
			int killedImps = st.getInt("killed_" + RUIN_IMP);
			int killedBats = st.getInt("killed_" + BAT);
			
			switch (npc.getId())
			{
				case SKELETON_WARRIOR:
				{
					if (killedWarriors < 15)
					{
						killedWarriors++;
						st.set("killed_" + SKELETON_WARRIOR, killedWarriors);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case RUIN_IMP:
				{
					if (killedImps < 20)
					{
						killedImps++;
						st.set("killed_" + RUIN_IMP, killedImps);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case RUIN_BAT:
				case BAT:
				{
					if (killedBats < 25)
					{
						killedBats++;
						st.set("killed_" + BAT, killedBats);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
			}
			
			if ((killedWarriors == 15) && (killedImps == 20) && (killedBats == 25))
			{
				st.setCond(3, true);
			}
			sendNpcLogList(killer);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(PlayerInstance activeChar)
	{
		final QuestState st = getQuestState(activeChar, false);
		if ((st != null) && st.isStarted() && st.isCond(2))
		{
			final Set<NpcLogListHolder> npcLogList = new HashSet<>(3);
			npcLogList.add(new NpcLogListHolder(SKELETON_WARRIOR, false, st.getInt("killed_" + SKELETON_WARRIOR)));
			npcLogList.add(new NpcLogListHolder(RUIN_IMP, false, st.getInt("killed_" + RUIN_IMP)));
			npcLogList.add(new NpcLogListHolder(27458, false, st.getInt("killed_" + BAT))); // NOTE: Somehow quest log react on bad ID, maybe client bug
			return npcLogList;
		}
		return super.getNpcLogList(activeChar);
	}
}