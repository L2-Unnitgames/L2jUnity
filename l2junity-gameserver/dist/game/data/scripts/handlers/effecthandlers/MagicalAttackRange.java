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

import org.l2junity.commons.util.CommonUtil;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * Magical Attack effect implementation.
 * @author Adry_85
 */
public final class MagicalAttackRange extends AbstractEffect
{
	private final double _power;
	private final double _powerRange;
	
	public MagicalAttackRange(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_powerRange = params.getDouble("powerRange", 0);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.MAGICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}

	@Override
	public void instant(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer() && effected.getActingPlayer().isFakeDeath())
		{
			effected.stopFakeDeath(true);
		}

		final double power = _power + CommonUtil.map(effector.calculateDistance(effected, false, false), 0, skill.getCastRange(), _powerRange, 0);
		
		boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcMCrit(effector.getMCriticalHit(effected, skill), skill, effected);
		final byte shld = Formulas.calcShldUse(effector, effected, skill);
		int damage = (int) Formulas.calcMagicDam(effector, effected, skill, power, shld, sps, bss, mcrit);
		
		if (damage > 0)
		{
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!effected.isRaid() && Formulas.calcAtkBreak(effected, damage))
			{
				effected.breakAttack();
				effected.breakCast();
			}
			
			// Shield Deflect Magic: Reflect all damage on caster.
			if (effected.getStat().calcStat(Stats.VENGEANCE_SKILL_MAGIC_DAMAGE, 0, effected, skill) > Rnd.get(100))
			{
				effector.reduceCurrentHp(damage, effected, skill);
				effector.notifyDamageReceived(damage, effected, skill, mcrit, false, true);
			}
			else
			{
				effected.reduceCurrentHp(damage, effector, skill);
				effected.notifyDamageReceived(damage, effector, skill, mcrit, false, false);
				effector.sendDamageMessage(effected, damage, mcrit, false, false);
			}
		}
		
		if (skill.isSuicideAttack())
		{
			effector.doDie(effector);
		}
	}
}