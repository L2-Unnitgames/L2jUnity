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

import java.util.ArrayList;
import java.util.List;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Formulas;

/**
 * Randomize Hate effect implementation.
 */
public final class RandomizeHate extends AbstractEffect
{
	private final int _chance;
	
	public RandomizeHate(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_chance = params.getInt("chance", 100);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return Formulas.calcProbability(_chance, info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if ((info.getEffected() == null) || (info.getEffected() == info.getEffector()) || !info.getEffected().isAttackable())
		{
			return;
		}
		
		Attackable effectedMob = (Attackable) info.getEffected();
		final List<Creature> targetList = new ArrayList<>();
		for (Creature cha : info.getEffected().getKnownList().getKnownCharacters())
		{
			if ((cha != null) && (cha != effectedMob) && (cha != info.getEffector()))
			{
				// Aggro cannot be transfered to a mob of the same faction.
				if (cha.isAttackable() && ((Attackable) cha).isInMyClan(effectedMob))
				{
					continue;
				}
				
				targetList.add(cha);
			}
		}
		// if there is no target, exit function
		if (targetList.isEmpty())
		{
			return;
		}
		
		// Choosing randomly a new target
		final Creature target = targetList.get(Rnd.get(targetList.size()));
		final int hate = effectedMob.getHating(info.getEffector());
		effectedMob.stopHating(info.getEffector());
		effectedMob.addDamageHate(target, 0, hate);
	}
}