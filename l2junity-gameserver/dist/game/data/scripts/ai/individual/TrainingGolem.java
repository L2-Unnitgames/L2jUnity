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
package ai.individual;

import org.l2junity.gameserver.model.actor.Npc;

import ai.npc.AbstractNpcAI;

/**
 * Training Golem AI.
 * @author Gladicek
 */
public final class TrainingGolem extends AbstractNpcAI
{
	// NPCs
	private static final int TRAINING_GOLEM = 27532;
	
	private TrainingGolem()
	{
		addSpawnId(TRAINING_GOLEM);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setIsImmobilized(true);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new TrainingGolem();
	}
}
