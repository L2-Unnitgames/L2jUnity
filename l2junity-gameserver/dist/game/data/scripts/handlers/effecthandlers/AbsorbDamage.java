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
package handlers.effecthandlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureDamageReceived;
import org.l2junity.gameserver.model.events.listeners.FunctionEventListener;
import org.l2junity.gameserver.model.events.returns.DamageReturn;
import org.l2junity.gameserver.model.skills.BuffInfo;

/**
 * @author Sdw
 */
public class AbsorbDamage extends AbstractEffect
{
	private final double _damage;
	private static final Map<Integer, Double> _damageHolder = new ConcurrentHashMap<>();
	
	public AbsorbDamage(StatsSet params)
	{
		_damage = params.getDouble("damage", 0);
	}
	
	private DamageReturn onDamageReceivedEvent(OnCreatureDamageReceived event)
	{
		// DOT effects are not taken into account.
		if (event.isDamageOverTime())
		{
			return null;
		}
		
		final int objectId = event.getTarget().getObjectId();
		
		double damageLeft = _damageHolder.get(objectId);
		double newDamageLeft = Math.max(damageLeft - event.getDamage(), 0);
		double newDamage = Math.max(event.getDamage() - damageLeft, 0);
		
		_damageHolder.put(objectId, newDamageLeft);
		
		return new DamageReturn(false, true, false, newDamage);
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
		_damageHolder.remove(info.getEffected().getObjectId());
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		_damageHolder.put(info.getEffected().getObjectId(), _damage);
		info.getEffected().addListener(new FunctionEventListener(info.getEffected(), EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedEvent(event), this));
	}
}
