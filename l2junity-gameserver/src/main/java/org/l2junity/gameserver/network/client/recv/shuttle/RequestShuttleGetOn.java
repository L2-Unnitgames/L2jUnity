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
package org.l2junity.gameserver.network.client.recv.shuttle;

import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.instance.L2ShuttleInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.recv.IClientIncomingPacket;
import org.l2junity.network.PacketReader;

/**
 * @author UnAfraid
 */
public class RequestShuttleGetOn implements IClientIncomingPacket
{
	private int _x;
	private int _y;
	private int _z;
	
	@Override
	public boolean read(PacketReader packet)
	{
		packet.readD(); // charId
		_x = packet.readD();
		_y = packet.readD();
		_z = packet.readD();
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
		
		// TODO: better way?
		for (WorldObject obj : activeChar.getKnownList().getKnownObjects().values())
		{
			if ((obj instanceof L2ShuttleInstance))
			{
				L2ShuttleInstance shuttle = (L2ShuttleInstance) obj;
				if (shuttle.calculateDistance(activeChar, false, false) < 1000)
				{
					shuttle.addPassenger(activeChar);
					activeChar.getInVehiclePosition().setXYZ(_x, _y, _z);
					break;
				}
				_log.info(getClass().getSimpleName() + ": range between char and shuttle: " + shuttle.calculateDistance(activeChar, false, false));
			}
		}
	}
}
