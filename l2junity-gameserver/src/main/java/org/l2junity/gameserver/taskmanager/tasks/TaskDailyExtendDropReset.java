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
package org.l2junity.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.variables.PlayerVariables;
import org.l2junity.gameserver.taskmanager.Task;
import org.l2junity.gameserver.taskmanager.TaskManager;
import org.l2junity.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.l2junity.gameserver.taskmanager.TaskTypes;

/**
 * @author Sdw
 */
public class TaskDailyExtendDropReset extends Task
{
	private static final String NAME = "daily_extend_drop_reset";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ?"))
		{
			ps.setString(1, PlayerVariables.EXTEND_DROP);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error(getClass().getSimpleName() + ": Could not reset extend drop : " + e);
		}
		
		// Update data for online players.
		World.getInstance().getPlayers().stream().forEach(player ->
		{
			player.getVariables().set(PlayerVariables.EXTEND_DROP, "");
			player.getVariables().storeMe();
		});
		
		LOGGER.info("Daily world chat points has been resetted.");
	}
	
	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(getName(), TaskTypes.TYPE_GLOBAL_TASK, "1", Config.EXTEND_DROP_RESET_TIME, "");
	}
}