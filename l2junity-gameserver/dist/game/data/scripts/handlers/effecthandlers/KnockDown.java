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

import org.l2junity.gameserver.GeoData;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.send.FlyToLocation;
import org.l2junity.gameserver.network.client.send.FlyToLocation.FlyType;
import org.l2junity.gameserver.network.client.send.ValidateLocation;
import org.l2junity.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public final class KnockDown extends AbstractEffect
{
	private int _distance = 50;
	private int _speed = 0;
	private int _delay = 0;
	private int _animationSpeed = 0;
	
	public KnockDown(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		if (params != null)
		{
			_distance = params.getInt("distance", 50);
			_speed = params.getInt("speed", 0);
			_delay = params.getInt("delay", 0);
			_animationSpeed = params.getInt("animationSpeed", 0);
		}
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final Creature effected = info.getEffected();
		final double radians = Math.toRadians(Util.calculateAngleFrom(info.getEffector(), info.getEffected()));
		final int newHeading = Util.calculateHeadingFrom(info.getEffected(), info.getEffector());
		final int x = (int) (info.getEffected().getX() + (_distance * Math.cos(radians)));
		final int y = (int) (info.getEffected().getY() + (_distance * Math.sin(radians)));
		final int z = effected.getZ();
		final Location loc = GeoData.getInstance().moveCheck(effected.getX(), effected.getY(), effected.getZ(), x, y, z, effected.getInstanceId());
		
		effected.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		effected.broadcastPacket(new FlyToLocation(effected, loc, FlyType.PUSH_DOWN_HORIZONTAL, _speed, _delay, _animationSpeed));
		effected.abortAttack();
		effected.abortCast();
		effected.setXYZ(loc);
		effected.setHeading(newHeading);
		effected.broadcastPacket(new ValidateLocation(effected));
	}
}
