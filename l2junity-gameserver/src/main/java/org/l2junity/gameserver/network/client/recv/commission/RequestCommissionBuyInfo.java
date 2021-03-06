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
package org.l2junity.gameserver.network.client.recv.commission;

import org.l2junity.gameserver.instancemanager.CommissionManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.commission.CommissionItem;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.recv.IClientIncomingPacket;
import org.l2junity.gameserver.network.client.send.commission.ExCloseCommission;
import org.l2junity.gameserver.network.client.send.commission.ExResponseCommissionBuyInfo;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.network.PacketReader;

/**
 * @author NosBit
 */
public class RequestCommissionBuyInfo implements IClientIncomingPacket
{
	private long _commissionId;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_commissionId = packet.readQ();
		// packet.readD(); // CommissionItemType
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (!CommissionManager.isPlayerAllowedToInteract(player))
		{
			client.sendPacket(ExCloseCommission.STATIC_PACKET);
			return;
		}
		
		if (!player.isInventoryUnder80(false) || (player.getWeightPenalty() >= 3))
		{
			client.sendPacket(SystemMessageId.IF_THE_WEIGHT_IS_80_OR_MORE_AND_THE_INVENTORY_NUMBER_IS_90_OR_MORE_PURCHASE_CANCELLATION_IS_NOT_POSSIBLE);
			client.sendPacket(ExResponseCommissionBuyInfo.FAILED);
			return;
		}
		
		final CommissionItem commissionItem = CommissionManager.getInstance().getCommissionItem(_commissionId);
		if (commissionItem != null)
		{
			client.sendPacket(new ExResponseCommissionBuyInfo(commissionItem));
		}
		else
		{
			client.sendPacket(SystemMessageId.ITEM_PURCHASE_IS_NOT_AVAILABLE_BECAUSE_THE_CORRESPONDING_ITEM_DOES_NOT_EXIST);
			client.sendPacket(ExResponseCommissionBuyInfo.FAILED);
		}
	}
}
