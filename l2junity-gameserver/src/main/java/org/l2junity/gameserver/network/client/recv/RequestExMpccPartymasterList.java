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
package org.l2junity.gameserver.network.client.recv;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.l2junity.gameserver.enums.MatchingRoomType;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.MatchingRoom;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.send.ExMPCCPartymasterList;
import org.l2junity.network.PacketReader;

/**
 * @author Sdw
 */
public class RequestExMpccPartymasterList implements IClientIncomingPacket
{
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final MatchingRoom room = activeChar.getMatchingRoom();
		if ((room != null) && (room.getRoomType() == MatchingRoomType.COMMAND_CHANNEL))
		{
			final Set<String> leadersName = room.getMembers().stream().map(PlayerInstance::getParty).filter(Objects::nonNull).map(Party::getLeader).map(PlayerInstance::getName).collect(Collectors.toSet());
			activeChar.sendPacket(new ExMPCCPartymasterList(leadersName));
		}
	}
}
