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
package handlers.admincommandhandlers;

import java.util.StringTokenizer;
import java.util.function.Predicate;

import org.l2junity.gameserver.datatables.SpawnTable;
import org.l2junity.gameserver.handler.IAdminCommandHandler;
import org.l2junity.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2junity.gameserver.model.L2Spawn;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.html.PageBuilder;
import org.l2junity.gameserver.model.html.PageResult;
import org.l2junity.gameserver.model.html.formatters.BypassParserFormatter;
import org.l2junity.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2junity.gameserver.model.html.styles.ButtonsStyle;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.util.BypassBuilder;
import org.l2junity.gameserver.util.BypassParser;
import org.l2junity.gameserver.util.Util;

/**
 * @author NosBit
 */
public class AdminScan implements IAdminCommandHandler
{
	private static final String SPACE = " ";
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_scan",
		"admin_deleteNpcByObjectId"
	};
	
	private static final int DEFAULT_RADIUS = 1000;
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_scan":
			{
				processBypass(activeChar, new BypassParser(command));
				break;
			}
			case "admin_deletenpcbyobjectid":
			{
				if (!st.hasMoreElements())
				{
					activeChar.sendMessage("Usage: //deletenpcbyobjectid objectId=<object_id>");
					return false;
				}
				
				final BypassParser parser = new BypassParser(command);
				try
				{
					final int objectId = parser.getInt("objectId", 0);
					if (objectId == 0)
					{
						activeChar.sendMessage("objectId is not set!");
					}
					
					final WorldObject target = World.getInstance().findObject(objectId);
					final Npc npc = target instanceof Npc ? (Npc) target : null;
					if (npc == null)
					{
						activeChar.sendMessage("NPC does not exist or object_id does not belong to an NPC");
						return false;
					}
					
					npc.deleteMe();
					
					final L2Spawn spawn = npc.getSpawn();
					if (spawn != null)
					{
						spawn.stopRespawn();
						
						if (RaidBossSpawnManager.getInstance().isDefined(spawn.getId()))
						{
							RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
						}
						else
						{
							SpawnTable.getInstance().deleteSpawn(spawn, true);
						}
					}
					
					activeChar.sendMessage(npc.getName() + " have been deleted.");
				}
				catch (NumberFormatException e)
				{
					activeChar.sendMessage("object_id must be a number.");
					return false;
				}
				
				processBypass(activeChar, parser);
				break;
			}
		}
		return true;
	}
	
	private void processBypass(PlayerInstance activeChar, BypassParser parser)
	{
		final int id = parser.getInt("id", 0);
		final String name = parser.getString("name", null);
		int radius = parser.getInt("radius", parser.getInt("range", DEFAULT_RADIUS));
		int page = parser.getInt("page", 0);
		
		final Predicate<Npc> condition;
		if (id > 0)
		{
			condition = npc -> npc.getId() == id;
		}
		else if (name != null)
		{
			condition = npc -> npc.getName().toLowerCase().startsWith(name.toLowerCase());
		}
		else
		{
			condition = npc -> true;
		}
		
		sendNpcList(activeChar, radius, page, condition, parser);
	}
	
	private BypassBuilder createBypassBuilder(BypassParser parser, String bypass)
	{
		final int id = parser.getInt("id", 0);
		final String name = parser.getString("name", null);
		final int radius = parser.getInt("radius", parser.getInt("range", DEFAULT_RADIUS));
		final BypassBuilder builder = new BypassBuilder(bypass);
		
		if (id > 0)
		{
			builder.addParam("id", id);
		}
		else if (name != null)
		{
			builder.addParam("name", name);
		}
		
		if (radius > DEFAULT_RADIUS)
		{
			builder.addParam("radius", radius);
		}
		return builder;
	}
	
	private void sendNpcList(PlayerInstance activeChar, int radius, int page, Predicate<Npc> condition, BypassParser parser)
	{
		final BypassBuilder bypassParser = createBypassBuilder(parser, "bypass -h admin_scan");
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar.getHtmlPrefix(), "data/html/admin/scan.htm");
		
		//@formatter:off
		final PageResult result = PageBuilder.newBuilder(World.getInstance().getVisibleObjects(activeChar, Npc.class, radius, condition), 15, bypassParser.toString())
			.currentPage(page)
			.pageHandler(NextPrevPageHandler.INSTANCE)
			.formatter(BypassParserFormatter.INSTANCE)
			.style(ButtonsStyle.INSTANCE)
			.bodyHandler((pages, character, sb) ->
		{
			final BypassBuilder builder = createBypassBuilder(parser, "bypass -h admin_deleteNpcByObjectId");
			builder.addParam("page", page);
			builder.addParam("objectId", character.getObjectId());
			
			sb.append("<tr>");
			sb.append("<td width=\"45\">").append(character.getId()).append("</td>");
			sb.append("<td><a action=\"bypass -h admin_move_to ").append(character.getX()).append(SPACE).append(character.getY()).append(SPACE).append(character.getZ()).append("\">").append(character.getName()).append("</a></td>");
			sb.append("<td width=\"60\">").append(Util.formatAdena(Math.round(activeChar.calculateDistance(character, false, false)))).append("</td>");
			sb.append("<td width=\"54\"><a action=\"").append(builder.toStringBuilder()).append("\"><font color=\"LEVEL\">Delete</font></a></td>");
			sb.append("</tr>");
		}).build();
		//@formatter:on
		
		if (result.getPages() > 0)
		{
			html.replace("%pages%", "<center><table width=\"100%\" cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table></center>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		
		html.replace("%data%", result.getBodyTemplate().toString());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
