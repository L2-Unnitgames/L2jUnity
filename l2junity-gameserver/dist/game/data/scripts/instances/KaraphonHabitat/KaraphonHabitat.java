/*
 * Copyright (C) 2004-2014 L2J DataPack
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
package instances.KaraphonHabitat;

import instances.AbstractInstance;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;
import org.l2junity.gameserver.model.quest.QuestState;

import quests.Q10745_TheSecretIngredients.Q10745_TheSecretIngredients;

/**
 * Karaphon Habitat instance.
 * @author Sdw
 */
public class KaraphonHabitat extends AbstractInstance
{
	// NPCs
	private static final int DOLKIN = 33954;
	private static final int DOLKIN_INSTANCE = 34002;
	// Monsters
	private static final int KARAPHON = 23459;
	private static final int KEEN_HONEYBEE = 23460;
	private static final int KEEN_GROWLER = 23461;
	// Locations
	private static final Location START_LOC = new Location(-82242, 246404, -14152);
	// Instance
	private static final int TEMPLATE_ID = 253;
	
	public KaraphonHabitat()
	{
		super(KaraphonHabitat.class.getSimpleName(), "instances");
		addStartNpc(DOLKIN);
		addTalkId(DOLKIN);
		addFirstTalkId(DOLKIN_INSTANCE);
		addSpawnId(KARAPHON, KEEN_HONEYBEE, KEEN_GROWLER);
		addKillId(KARAPHON);
	}
	
	@Override
	public void onEnterInstance(PlayerInstance player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
		}
		teleportPlayer(player, START_LOC, world.getInstanceId());
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = player.getQuestState(Q10745_TheSecretIngredients.class.getSimpleName());
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		switch (event)
		{
			case "enter_instance":
			{
				enterInstance(player, "KaraphonHabitat.xml", TEMPLATE_ID);
				break;
			}
			case "exit_instance":
			{
				final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
				if (world != null)
				{
					finishInstance(world, 0);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
		if (world != null)
		{
			handleReenterTime(world);
		}
		return super.onKill(npc, killer, isSummon);
	}
}