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
package org.l2junity.gameserver.model.actor.knownlist;

import org.l2junity.Config;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.instance.L2GuardInstance;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuardKnownList extends AttackableKnownList
{
	private static final Logger _log = LoggerFactory.getLogger(GuardKnownList.class.getName());
	
	public GuardKnownList(L2GuardInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(WorldObject object)
	{
		if (!super.addKnownObject(object))
		{
			return false;
		}
		
		if (object.isPlayer())
		{
			// Check if the object added is a L2PcInstance that owns Karma
			if (object.getActingPlayer().getKarma() > 0)
			{
				if (Config.DEBUG)
				{
					_log.debug(getActiveChar().getObjectId() + ": PK " + object.getObjectId() + " entered scan range");
				}
				
				// Set the L2GuardInstance Intention to AI_INTENTION_ACTIVE
				if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if ((Config.GUARD_ATTACK_AGGRO_MOB && getActiveChar().isInActiveRegion()) && object.isMonster())
		{
			// Check if the object added is an aggressive L2MonsterInstance
			if (((L2MonsterInstance) object).isAggressive())
			{
				if (Config.DEBUG)
				{
					_log.debug(getActiveChar().getObjectId() + ": Aggressive mob " + object.getObjectId() + " entered scan range");
				}
				
				// Set the L2GuardInstance Intention to AI_INTENTION_ACTIVE
				if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		
		return true;
	}
	
	@Override
	protected boolean removeKnownObject(WorldObject object, boolean forget)
	{
		if (!super.removeKnownObject(object, forget))
		{
			return false;
		}
		
		// Check if the aggression list of this guard is empty.
		if (getActiveChar().getAggroList().isEmpty())
		{
			// Set the L2GuardInstance to AI_INTENTION_IDLE
			if (getActiveChar().hasAI() && !getActiveChar().isWalker())
			{
				getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
			}
		}
		
		return true;
	}
	
	@Override
	public final L2GuardInstance getActiveChar()
	{
		return (L2GuardInstance) super.getActiveChar();
	}
}
