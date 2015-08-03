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
package handlers.effecthandlers;

import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.FlyToLocation;
import org.l2junity.gameserver.network.client.send.FlyToLocation.FlyType;
import org.l2junity.gameserver.network.client.send.ValidateLocation;

/**
 * This Blink effect switches the location of the caster and the target.<br>
 * This effect is totally done based on client description. <br>
 * Assume that geodata checks are done on the skill cast and not needed to repeat here.
 * @author Nik
 */
public final class BlinkSwap extends AbstractEffect
{
	public BlinkSwap(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill)
	{
		final Location effectorLoc = effector.getLocation();
		final Location effectedLoc = effected.getLocation();
		
		effector.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		effector.broadcastPacket(new FlyToLocation(effector, effectedLoc, FlyType.DUMMY));
		effector.abortAttack();
		effector.abortCast();
		effector.setXYZ(effectedLoc);
		effector.broadcastPacket(new ValidateLocation(effector));
		
		effected.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		effected.broadcastPacket(new FlyToLocation(effected, effectorLoc, FlyType.DUMMY));
		effected.abortAttack();
		effected.abortCast();
		effected.setXYZ(effectorLoc);
		effected.broadcastPacket(new ValidateLocation(effected));
	}
}
