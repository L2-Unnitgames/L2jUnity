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
package org.l2junity.gameserver.model;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.data.sql.impl.TerritoryTable;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2ControllableMobInstance;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;

/**
 * @author littlecrow A special spawn implementation to spawn controllable mob
 */
public class L2GroupSpawn extends L2Spawn
{
	private final L2NpcTemplate _template;
	
	public L2GroupSpawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		super(mobTemplate);
		_template = mobTemplate;
		
		setAmount(1);
	}
	
	public Npc doGroupSpawn()
	{
		try
		{
			if (_template.isType("L2Pet") || _template.isType("L2Minion"))
			{
				return null;
			}
			
			int newlocx = 0;
			int newlocy = 0;
			int newlocz = 0;
			
			if ((getX() == 0) && (getY() == 0))
			{
				if (getLocationId() == 0)
				{
					return null;
				}
				
				final Location location = TerritoryTable.getInstance().getRandomPoint(getLocationId());
				if (location != null)
				{
					newlocx = location.getX();
					newlocy = location.getY();
					newlocz = location.getZ();
				}
			}
			else
			{
				newlocx = getX();
				newlocy = getY();
				newlocz = getZ();
			}
			
			final Npc mob = new L2ControllableMobInstance(_template);
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			
			if (getHeading() == -1)
			{
				mob.setHeading(Rnd.nextInt(61794));
			}
			else
			{
				mob.setHeading(getHeading());
			}
			
			mob.setSpawn(this);
			mob.spawnMe(newlocx, newlocy, newlocz);
			return mob;
			
		}
		catch (Exception e)
		{
			_log.warn("NPC class not found: " + e.getMessage(), e);
			return null;
		}
	}
}