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
package ai.npc.OlyManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2junity.Config;
import org.l2junity.gameserver.data.xml.impl.MultisellData;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.entity.Hero;
import org.l2junity.gameserver.model.olympiad.CompetitionType;
import org.l2junity.gameserver.model.olympiad.Olympiad;
import org.l2junity.gameserver.model.olympiad.OlympiadManager;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

import ai.npc.AbstractNpcAI;

/**
 * Olympiad Manager AI.
 * @author St3eT
 */
public final class OlyManager extends AbstractNpcAI
{
	// NPCs
	private static final int[] MANAGERS =
	{
		31688,
	};
	// Misc
	
	private static final Map<CategoryType, Integer> EQUIPMENT_MULTISELL = new HashMap<>();
	
	{
		EQUIPMENT_MULTISELL.put(CategoryType.SIGEL_GROUP, 917);
		EQUIPMENT_MULTISELL.put(CategoryType.TYRR_GROUP, 918);
		EQUIPMENT_MULTISELL.put(CategoryType.OTHELL_GROUP, 919);
		EQUIPMENT_MULTISELL.put(CategoryType.YUL_GROUP, 920);
		EQUIPMENT_MULTISELL.put(CategoryType.FEOH_GROUP, 921);
		EQUIPMENT_MULTISELL.put(CategoryType.ISS_GROUP, 923);
		EQUIPMENT_MULTISELL.put(CategoryType.WYNN_GROUP, 922);
		EQUIPMENT_MULTISELL.put(CategoryType.AEORE_GROUP, 924);
	}
	
	private OlyManager()
	{
		addStartNpc(MANAGERS);
		addFirstTalkId(MANAGERS);
		addTalkId(MANAGERS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		
		switch (event)
		{
			case "OlyManager-info.html":
			case "OlyManager-infoHistory.html":
			case "OlyManager-infoRules.html":
			case "OlyManager-infoPoints.html":
			case "OlyManager-infoPointsCalc.html":
			case "OlyManager-rank.html":
			case "OlyManager-rewards.html":
			{
				htmltext = event;
				break;
			}
			case "index":
			{
				htmltext = onFirstTalk(npc, player);
				break;
			}
			case "joinMatch":
			{
				if (OlympiadManager.getInstance().isRegistered(player))
				{
					htmltext = "OlyManager-registred.html";
				}
				else
				{
					switch (LocalDate.now().get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfMonth()))
					{
						case 1:
						case 2:
						case 3: // First 3 weeks of month is 1v1 + 1v1 class matches
						{
							htmltext = getHtm(player.getHtmlPrefix(), "OlyManager-joinMatch.html");
							break;
						}
						default:// Rest is only 1v1 class matches
						{
							htmltext = getHtm(player.getHtmlPrefix(), "OlyManager-joinMatchClass.html");
							break;
						}
					}
					
					htmltext = htmltext.replace("%olympiad_round%", String.valueOf(Olympiad.getInstance().getPeriod()));
					htmltext = htmltext.replace("%olympiad_week%", String.valueOf(Olympiad.getInstance().getCurrentCycle()));
					htmltext = htmltext.replace("%olympiad_participant%", String.valueOf(OlympiadManager.getInstance().getCountOpponents()));
				}
				break;
			}
			case "register1v1":
			case "register1v1class":
			{
				if (player.isSubClassActive())
				{
					htmltext = "OlyManager-subclass.html";
				}
				else if (!player.isInCategory(CategoryType.AWAKEN_GROUP))
				{
					htmltext = "OlyManager-awaken.html";
				}
				else if (Olympiad.getInstance().getNoblePoints(player) <= 0)
				{
					htmltext = "OlyManager-noPoints.html";
				}
				else if (!player.isInventoryUnder80(false))
				{
					player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
				}
				else
				{
					if (event.equals("register1v1"))
					{
						OlympiadManager.getInstance().registerNoble(player, CompetitionType.NON_CLASSED);
					}
					else
					{
						OlympiadManager.getInstance().registerNoble(player, CompetitionType.CLASSED);
					}
				}
				break;
			}
			case "unregister":
			{
				OlympiadManager.getInstance().unRegisterNoble(player);
				break;
			}
			case "calculatePoints":
			{
				final int points = Olympiad.getInstance().getOlympiadTradePoint(player, false);
				if (points == 0)
				{
					htmltext = "OlyManager-calculateNoEnough.html";
				}
				else if (points < 20)
				{
					if (Hero.getInstance().isUnclaimedHero(player.getObjectId()) || Hero.getInstance().isHero(player.getObjectId()))
					{
						htmltext = "OlyManager-calculateEnough.html";
					}
					else
					{
						htmltext = "OlyManager-calculateNoEnough.html";
					}
				}
				else
				{
					htmltext = "OlyManager-calculateEnough.html";
				}
				break;
			}
			case "calculatePointsDone":
			{
				if (player.isInventoryUnder80(false))
				{
					final int tradePoints = Olympiad.getInstance().getOlympiadTradePoint(player, true);
					if (tradePoints > 0)
					{
						giveItems(player, Config.ALT_OLY_COMP_RITEM, tradePoints * Config.ALT_OLY_MARK_PER_POINT);
					}
				}
				else
				{
					player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
				}
				break;
			}
			case "showEquipmentReward":
			{
				int multisellId = -1;
				
				if (player.getClassId() == ClassId.SAYHA_SEER)
				{
					multisellId = 926;
				}
				else if (player.getClassId() == ClassId.EVISCERATOR)
				{
					multisellId = 925;
				}
				else
				{
					for (CategoryType type : EQUIPMENT_MULTISELL.keySet())
					{
						if (player.isInCategory(type))
						{
							multisellId = EQUIPMENT_MULTISELL.get(type);
							break;
						}
					}
				}
				
				if (multisellId > 0)
				{
					MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
				}
				break;
			}
			case "rank_148": // Sigel Phoenix Knight
			case "rank_149": // Sigel Hell Knight
			case "rank_150": // Sigel Eva's Templar
			case "rank_151": // Sigel Shillien Templar
			case "rank_152": // Tyrr Duelist
			case "rank_153": // Tyrr Dreadnought
			case "rank_154": // Tyrr Titan
			case "rank_155": // Tyrr Grand Khavatari
			case "rank_156": // Tyrr Maestro
			case "rank_157": // Tyrr Doombringer
			case "rank_158": // Othell Adventurer
			case "rank_159": // Othell Wind Rider
			case "rank_160": // Othell Ghost Hunter
			case "rank_161": // Othell Fortune Seeker
			case "rank_162": // Yul Sagittarius
			case "rank_163": // Yul Moonlight Sentinel
			case "rank_164": // Yul Ghost Sentinel
			case "rank_165": // Yul Trickster
			case "rank_166": // Feoh Archmage
			case "rank_167": // Feoh Soultaker
			case "rank_168": // Feoh Mystic Muse
			case "rank_169": // Feoh Storm Screamer
			case "rank_170": // Feoh Soul Hound
			case "rank_171": // Iss Hierophant
			case "rank_172": // Iss Sword Muse
			case "rank_173": // Iss Spectral Dancer
			case "rank_174": // Iss Dominator
			case "rank_175": // Iss Doomcryer
			case "rank_176": // Wynn Arcana Lord
			case "rank_177": // Wynn Elemental Master
			case "rank_178": // Wynn Spectral Master
			case "rank_179": // Aeore Cardinal
			case "rank_180": // Aeore Eva's Saint
			case "rank_181": // Aeore Shillien Saint
			case "rank_188": // Eviscerator
			case "rank_189": // Sayha's Seer
			{
				final int classId = Integer.parseInt(event.replace("rank_", ""));
				final List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
				htmltext = getHtm(player.getHtmlPrefix(), "OlyManager-rankDetail.html");
				
				int index = 1;
				for (String name : names)
				{
					htmltext = htmltext.replace("%Rank" + index + "%", String.valueOf(index));
					htmltext = htmltext.replace("%Name" + index + "%", name);
					index++;
					if (index > 15)
					{
						break;
					}
				}
				for (; index <= 15; index++)
				{
					htmltext = htmltext.replace("%Rank" + index + "%", "");
					htmltext = htmltext.replace("%Name" + index + "%", "");
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		
		if (!player.isCursedWeaponEquipped())
		{
			htmltext = player.isNoble() ? "OlyManager-noble.html" : "OlyManager-noNoble.html";
		}
		else
		{
			htmltext = "OlyManager-noCursed.html";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new OlyManager();
	}
}