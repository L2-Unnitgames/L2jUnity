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
package org.l2junity.gameserver.instancemanager;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.CeremonyOfChaosState;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.ceremonyofchaos.CeremonyOfChaosEvent;
import org.l2junity.gameserver.model.ceremonyofchaos.CeremonyOfChaosMember;
import org.l2junity.gameserver.model.eventengine.AbstractEventManager;
import org.l2junity.gameserver.model.eventengine.ScheduleTarget;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerBypass;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogin;
import org.l2junity.gameserver.model.events.returns.TerminateReturn;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseState;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sdw
 */
public class CeremonyOfChaosManager extends AbstractEventManager<CeremonyOfChaosEvent>
{
	protected static final Logger LOGGER = LoggerFactory.getLogger(CeremonyOfChaosManager.class);
	
	public static final String BUFF_KEY = "buff";
	public static final String ITEMS_KEY = "items";
	public static final String MAX_PLAYERS_KEY = "max_players";
	public static final String MAX_ARENAS_KEY = "max_arenas";
	public static final String INSTANCE_TEMPLATES_KEY = "instance_templates";
	
	protected CeremonyOfChaosManager()
	{
	}
	
	@Override
	public void onInitialized()
	{
	
	}
	
	@ScheduleTarget
	public void onPeriodEnd(String text)
	{
		LOGGER.info("Ceremony of Chaos period has ended!");
		LOGGER.info(text);
	}
	
	@ScheduleTarget
	public void onEventStart()
	{
		LOGGER.info("Ceremony of Chaos event has started!");
	}
	
	@ScheduleTarget
	public void onEventEnd()
	{
		LOGGER.info("Ceremony of Chaos event has ended!");
	}
	
	@ScheduleTarget
	public void onRegistrationStart()
	{
		if (getState() != CeremonyOfChaosState.SCHEDULED)
		{
			return;
		}
		
		setState(CeremonyOfChaosState.REGISTRATION);
		for (PlayerInstance player : World.getInstance().getPlayers())
		{
			if (player.isOnline())
			{
				player.sendPacket(SystemMessageId.REGISTRATION_FOR_THE_CEREMONY_OF_CHAOS_HAS_BEGUN);
				if (canRegister(player, false))
				{
					player.sendPacket(ExCuriousHouseState.REGISTRATION_PACKET);
				}
			}
		}
	}
	
	@ScheduleTarget
	public void onRegistrationEnd()
	{
		if (getState() != CeremonyOfChaosState.REGISTRATION)
		{
			return;
		}
		
		setState(CeremonyOfChaosState.PREPARING_FOR_TELEPORT);
		for (PlayerInstance player : World.getInstance().getPlayers())
		{
			if (player.isOnline())
			{
				player.sendPacket(SystemMessageId.REGISTRATION_FOR_THE_CEREMONY_OF_CHAOS_HAS_ENDED);
				if (!isRegistered(player))
				{
					player.sendPacket(ExCuriousHouseState.IDLE_PACKET);
				}
			}
		}
		
		ThreadPoolManager.getInstance().scheduleEvent(() -> onAboutToTeleport(60), 55 * 1000);
		ThreadPoolManager.getInstance().scheduleEvent(() -> onAboutToTeleport(10), (55 + 10) * 1000);
		for (int i = 5; i > 0; i--)
		{
			final int timeLeft = i;
			ThreadPoolManager.getInstance().scheduleEvent(() -> onAboutToTeleport(timeLeft), (55 + 10 + (5 - timeLeft)) * 1000);
		}
	}
	
	private void onAboutToTeleport(int time)
	{
		switch (time)
		{
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 60:
			{
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_THE_ARENA_IN_S1_SECOND_S);
				msg.addByte(time);
				for (PlayerInstance player : getRegisteredPlayers())
				{
					if (player.isOnline())
					{
						player.sendPacket(msg);
					}
				}
				break;
			}
			case 10:
			{
				for (PlayerInstance player : getRegisteredPlayers())
				{
					if (player.isOnline())
					{
						player.sendPacket(ExCuriousHouseState.STARTING_PACKET);
					}
				}
				break;
			}
		}
	}
	
	@ScheduleTarget
	public void onPrepareForFight()
	{
		if (getState() != CeremonyOfChaosState.PREPARING_FOR_TELEPORT)
		{
			return;
		}
		
		setState(CeremonyOfChaosState.PREPARING_FOR_FIGHT);
		int eventId = 0;
		int position = 1;
		CeremonyOfChaosEvent event = null;
		final List<PlayerInstance> players = getRegisteredPlayers().stream().sorted(Comparator.comparingInt(PlayerInstance::getLevel)).collect(Collectors.toList());
		final int maxPlayers = getVariables().getInt(MAX_PLAYERS_KEY, 18);
		final List<String> templates = getVariables().getList(INSTANCE_TEMPLATES_KEY, String.class);
		for (PlayerInstance player : players)
		{
			if (player.isOnline() && canRegister(player, true))
			{
				if ((event == null) || (event.getMembers().size() >= maxPlayers))
				{
					
					event = new CeremonyOfChaosEvent(eventId++, templates.get(Rnd.get(templates.size())));
					position = 1;
					getEvents().add(event);
				}
				
				event.addMember(new CeremonyOfChaosMember(player, event, position++));
			}
			else
			{
				// TODO: Handle player penalties
				player.sendPacket(ExCuriousHouseState.IDLE_PACKET);
			}
		}
		
		// Prepare all event's players for start
		getEvents().forEach(CeremonyOfChaosEvent::preparePlayers);
	}
	
	@ScheduleTarget
	public void onStartFight()
	{
		if (getState() != CeremonyOfChaosState.PREPARING_FOR_FIGHT)
		{
			return;
		}
		
		setState(CeremonyOfChaosState.RUNNING);
		getEvents().forEach(CeremonyOfChaosEvent::startFight);
	}
	
	@ScheduleTarget
	public void onEndFight()
	{
		if (getState() != CeremonyOfChaosState.RUNNING)
		{
			return;
		}
		
		setState(CeremonyOfChaosState.SCHEDULED);
		getEvents().forEach(CeremonyOfChaosEvent::stopFight);
	}
	
	@Override
	public boolean canRegister(PlayerInstance player, boolean sendMessage)
	{
		boolean canRegister = true;
		
		final L2Clan clan = player.getClan();
		
		SystemMessageId sm = null;
		
		if (getState() != CeremonyOfChaosState.REGISTRATION)
		{
			canRegister = false;
		}
		else if (player.getLevel() < 85)
		{
			sm = SystemMessageId.ONLY_CHARACTERS_LEVEL_85_OR_ABOVE_MAY_PARTICIPATE_IN_THE_TOURNAMENT;
			canRegister = false;
		}
		else if (player.isFlyingMounted())
		{
			sm = SystemMessageId.YOU_CANNOT_PARTICIPATE_IN_THE_CEREMONY_OF_CHAOS_AS_A_FLYING_TRANSFORMED_OBJECT;
			canRegister = false;
		}
		else if (!player.isInCategory(CategoryType.AWAKEN_GROUP))
		{
			sm = SystemMessageId.ONLY_CHARACTERS_WHO_HAVE_COMPLETED_THE_3RD_CLASS_TRANSFER_MAY_PARTICIPATE;
			canRegister = false;
		}
		else if (!player.isInventoryUnder90(false))
		{
			sm = SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY;
			canRegister = false;
		}
		else if ((clan == null) || (clan.getLevel() < 6))
		{
			sm = SystemMessageId.ONLY_CHARACTERS_WHO_ARE_A_PART_OF_A_CLAN_OF_LEVEL_6_OR_ABOVE_MAY_PARTICIPATE;
			canRegister = false;
		}
		else if (getRegisteredPlayers().size() >= (getVariables().getInt(MAX_ARENAS_KEY, 5) * getVariables().getInt(MAX_PLAYERS_KEY, 18)))
		{
			sm = SystemMessageId.THERE_ARE_TOO_MANY_CHALLENGERS_YOU_CANNOT_PARTICIPATE_NOW;
			canRegister = false;
		}
		else if (player.isCursedWeaponEquipped() || (player.getReputation() < 0))
		{
			sm = SystemMessageId.WAITING_LIST_REGISTRATION_IS_NOT_ALLOWED_WHILE_THE_CURSED_SWORD_IS_BEING_USED_OR_THE_STATUS_IS_IN_A_CHAOTIC_STATE;
			canRegister = false;
		}
		else if (player.isInDuel())
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_IN_THE_WAITING_LIST_DURING_A_DUEL;
			canRegister = false;
		}
		else if (player.isInOlympiadMode())
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_IN_THE_WAITING_LIST_WHILE_PARTICIPATING_IN_OLYMPIAD;
			canRegister = false;
		}
		else if (player.getInstanceId() > 0)
		{
			// TODO : check if there is a message for that case
			canRegister = false;
		}
		else if (player.isInSiege())
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_FOR_THE_WAITING_LIST_ON_THE_BATTLEFIELD_CASTLE_SIEGE_FORTRESS_SIEGE;
			canRegister = false;
		}
		else if (player.isInsideZone(ZoneId.SIEGE))
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_IN_THE_WAITING_LIST_WHILE_BEING_INSIDE_OF_A_BATTLEGROUND_CASTLE_SIEGE_FORTRESS_SIEGE;
			canRegister = false;
		}
		else if (isRegistered(player))
		{
			sm = SystemMessageId.YOU_ARE_ON_THE_WAITING_LIST_FOR_THE_CEREMONY_OF_CHAOS;
			canRegister = false;
		}
		
		if ((sm != null) && sendMessage)
		{
			player.sendPacket(sm);
		}
		
		return canRegister;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public TerminateReturn OnPlayerBypass(OnPlayerBypass event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (player == null)
		{
			return null;
		}
		
		if (event.getCommand().equalsIgnoreCase("pledgegame?command=apply"))
		{
			if (registerPlayer(player))
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOW_ON_THE_WAITING_LIST_YOU_WILL_AUTOMATICALLY_BE_TELEPORTED_WHEN_THE_TOURNAMENT_STARTS_AND_WILL_BE_REMOVED_FROM_THE_WAITING_LIST_IF_YOU_LOG_OUT_IF_YOU_CANCEL_REGISTRATION_WITHIN_THE_LAST_MINUTE_OF_ENTERING_THE_ARENA_AFTER_SIGNING_UP_30_TIMES_OR_MORE_OR_FORFEIT_AFTER_ENTERING_THE_ARENA_30_TIMES_OR_MORE_DURING_A_CYCLE_YOU_BECOME_INELIGIBLE_FOR_PARTICIPATION_IN_THE_CEREMONY_OF_CHAOS_UNTIL_THE_NEXT_CYCLE_ALL_THE_BUFFS_EXCEPT_THE_VITALITY_BUFF_WILL_BE_REMOVED_ONCE_YOU_ENTER_THE_ARENAS);
				player.sendPacket(SystemMessageId.EXCEPT_THE_VITALITY_BUFF_ALL_BUFFS_INCLUDING_ART_OF_SEDUCTION_WILL_BE_DELETED);
				player.sendPacket(ExCuriousHouseState.PREPARE_PACKET);
			}
			return new TerminateReturn(true, false, false);
		}
		return null;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		if (getState() == CeremonyOfChaosState.REGISTRATION)
		{
			final PlayerInstance player = event.getActiveChar();
			if (canRegister(player, false))
			{
				player.sendPacket(ExCuriousHouseState.REGISTRATION_PACKET);
			}
		}
	}
	
	// player leave clan
	
	public static CeremonyOfChaosManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CeremonyOfChaosManager _instance = new CeremonyOfChaosManager();
	}
}
