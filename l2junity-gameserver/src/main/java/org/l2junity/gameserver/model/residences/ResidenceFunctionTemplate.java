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
package org.l2junity.gameserver.model.residences;

import java.time.Duration;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.holders.ItemHolder;

/**
 * @author UnAfraid
 */
public class ResidenceFunctionTemplate
{
	private final int _id;
	private final int _level;
	private final ResidenceFunctionType _type;
	private final ItemHolder _cost;
	private final Duration _duration;
	private final double _value;
	
	public ResidenceFunctionTemplate(StatsSet set)
	{
		_id = set.getInt("id");
		_level = set.getInt("level");
		_type = set.getEnum("type", ResidenceFunctionType.class, ResidenceFunctionType.NONE);
		_cost = new ItemHolder(set.getInt("costId"), set.getLong("costCount"));
		_duration = set.getDuration("duration");
		_value = set.getDouble("value", 0);
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public ResidenceFunctionType getType()
	{
		return _type;
	}
	
	public ItemHolder getCost()
	{
		return _cost;
	}
	
	public Duration getDuration()
	{
		return _duration;
	}
	
	public double getValue()
	{
		return _value;
	}
}
