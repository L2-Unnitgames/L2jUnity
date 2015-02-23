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
package org.l2junity.gameserver.model.olympiad;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.Config;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.instancemanager.AntiFeedManager;
import org.l2junity.gameserver.instancemanager.CastleManager;
import org.l2junity.gameserver.instancemanager.FortManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.Party.MessageType;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.TvTEvent;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.zone.type.OlympiadStadiumZone;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.ExOlympiadMode;
import org.l2junity.gameserver.network.serverpackets.InventoryUpdate;
import org.l2junity.gameserver.network.serverpackets.L2GameServerPacket;
import org.l2junity.gameserver.network.serverpackets.SkillCoolTime;
import org.l2junity.gameserver.network.serverpackets.SystemMessage;

/**
 * @author godson, GodKratos, Pere, DS
 */
public abstract class AbstractOlympiadGame
{
	protected static final Logger _log = Logger.getLogger(AbstractOlympiadGame.class.getName());
	protected static final Logger _logResults = Logger.getLogger("olympiad");
	
	protected static final String POINTS = "olympiad_points";
	protected static final String COMP_DONE = "competitions_done";
	protected static final String COMP_WON = "competitions_won";
	protected static final String COMP_LOST = "competitions_lost";
	protected static final String COMP_DRAWN = "competitions_drawn";
	protected static final String COMP_DONE_WEEK = "competitions_done_week";
	protected static final String COMP_DONE_WEEK_CLASSED = "competitions_done_week_classed";
	protected static final String COMP_DONE_WEEK_NON_CLASSED = "competitions_done_week_non_classed";
	protected static final String COMP_DONE_WEEK_TEAM = "competitions_done_week_team";
	
	protected long _startTime = 0;
	protected boolean _aborted = false;
	protected final int _stadiumID;
	
	protected AbstractOlympiadGame(int id)
	{
		_stadiumID = id;
	}
	
	public final boolean isAborted()
	{
		return _aborted;
	}
	
	public final int getStadiumId()
	{
		return _stadiumID;
	}
	
	protected boolean makeCompetitionStart()
	{
		_startTime = System.currentTimeMillis();
		return !_aborted;
	}
	
	protected final void addPointsToParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, points);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_OLYMPIAD_GAMES);
		sm.addString(par.getName());
		sm.addInt(points);
		broadcastPacket(sm);
	}
	
	protected final void removePointsFromParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, -points);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_OLYMPIAD_GAMES);
		sm.addString(par.getName());
		sm.addInt(points);
		broadcastPacket(sm);
	}
	
	/**
	 * Function return null if player passed all checks or SystemMessage with reason for broadcast to opponent(s).
	 * @param player
	 * @return
	 */
	protected static SystemMessage checkDefaulted(PlayerInstance player)
	{
		if ((player == null) || !player.isOnline())
		{
			return SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		if ((player.getClient() == null) || player.getClient().isDetached())
		{
			return SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		// safety precautions
		if (player.inObserverMode() || TvTEvent.isPlayerParticipant(player.getObjectId()))
		{
			return SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		SystemMessage sm;
		if (player.isDead())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(player);
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		if (player.isSubClassActive())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOU_HAVE_CHANGED_YOUR_CLASS_TO_SUBCLASS);
			sm.addPcName(player);
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		if (player.isCursedWeaponEquipped())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_THE_OWNER_OF_S2_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(player);
			sm.addItemName(player.getCursedWeaponEquippedId());
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		if (!player.isInventoryUnder90(true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_AS_THE_INVENTORY_WEIGHT_SLOT_IS_FILLED_BEYOND_80);
			sm.addPcName(player);
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		return null;
	}
	
	protected static final boolean portPlayerToArena(Participant par, Location loc, int id)
	{
		final PlayerInstance player = par.getPlayer();
		if ((player == null) || !player.isOnline())
		{
			return false;
		}
		
		try
		{
			player.setLastLocation();
			if (player.isSitting())
			{
				player.standUp();
			}
			player.setTarget(null);
			
			player.setOlympiadGameId(id);
			player.setIsInOlympiadMode(true);
			player.setIsOlympiadStart(false);
			player.setOlympiadSide(par.getSide());
			player.setOlympiadBuffCount(Config.ALT_OLY_MAX_BUFFS);
			loc.setInstanceId(OlympiadGameManager.getInstance().getOlympiadTask(id).getZone().getInstanceId());
			player.teleToLocation(loc, false);
			player.sendPacket(new ExOlympiadMode(2));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	protected static final void removals(PlayerInstance player, boolean removeParty)
	{
		try
		{
			if (player == null)
			{
				return;
			}
			
			// Remove Buffs
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			
			// Remove Clan Skills
			if (player.getClan() != null)
			{
				player.getClan().removeSkillEffects(player);
				if (player.getClan().getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(player.getClan()).removeResidentialSkills(player);
				}
				if (player.getClan().getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(player.getClan()).removeResidentialSkills(player);
				}
			}
			// Abort casting if player casting
			player.abortAttack();
			player.abortCast();
			
			// Force the character to be visible
			player.setInvisible(false);
			
			// Heal Player fully
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			
			// Remove Summon's Buffs
			if (player.hasSummon())
			{
				final Summon pet = player.getPet();
				if (pet != null)
				{
					pet.unSummon(player);
				}
				
				player.getServitors().values().forEach(s ->
				{
					s.stopAllEffectsExceptThoseThatLastThroughDeath();
					s.abortAttack();
					s.abortCast();
				});
			}
			
			// stop any cubic that has been given by other player.
			player.stopCubicsByOthers();
			
			// Remove player from his party
			if (removeParty)
			{
				final Party party = player.getParty();
				if (party != null)
				{
					party.removePartyMember(player, MessageType.EXPELLED);
				}
			}
			// Remove Agathion
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
				player.broadcastUserInfo();
			}
			
			player.checkItemRestriction();
			
			// Remove shot automation
			player.disableAutoShotsAll();
			
			// Discharge any active shots
			ItemInstance item = player.getActiveWeaponInstance();
			if (item != null)
			{
				item.unChargeAllShots();
			}
			
			// enable skills with cool time <= 15 minutes
			for (Skill skill : player.getAllSkills())
			{
				if (skill.getReuseDelay() <= 900000)
				{
					player.enableSkill(skill);
				}
			}
			
			player.sendSkillList();
			player.sendPacket(new SkillCoolTime(player));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	protected static final void cleanEffects(PlayerInstance player)
	{
		try
		{
			// prevent players kill each other
			player.setIsOlympiadStart(false);
			player.setTarget(null);
			player.abortAttack();
			player.abortCast();
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			
			if (player.isDead())
			{
				player.setIsDead(false);
			}
			
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			player.clearSouls();
			player.clearCharges();
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
			}
			final Summon pet = player.getPet();
			if ((pet != null) && !pet.isDead())
			{
				pet.setTarget(null);
				pet.abortAttack();
				pet.abortCast();
				pet.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				pet.stopAllEffectsExceptThoseThatLastThroughDeath();
			}
			
			player.getServitors().values().stream().filter(s -> !s.isDead()).forEach(s ->
			{
				s.setTarget(null);
				s.abortAttack();
				s.abortCast();
				s.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				s.stopAllEffectsExceptThoseThatLastThroughDeath();
			});
			
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	protected static final void playerStatusBack(PlayerInstance player)
	{
		try
		{
			if (player.isTransformed())
			{
				player.untransform();
			}
			
			player.setIsInOlympiadMode(false);
			player.setIsOlympiadStart(false);
			player.setOlympiadSide(-1);
			player.setOlympiadGameId(-1);
			player.sendPacket(new ExOlympiadMode(0));
			
			// Add Clan Skills
			if (player.getClan() != null)
			{
				player.getClan().addSkillEffects(player);
				if (player.getClan().getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(player.getClan()).giveResidentialSkills(player);
				}
				if (player.getClan().getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(player.getClan()).giveResidentialSkills(player);
				}
				player.sendSkillList();
			}
			
			// heal again after adding clan skills
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
			
			if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, player);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "portPlayersToArena()", e);
		}
	}
	
	protected static final void portPlayerBack(PlayerInstance player)
	{
		if (player == null)
		{
			return;
		}
		final Location loc = player.getLastLocation();
		if ((loc.getX() == 0) && (loc.getY() == 0))
		{
			return;
		}
		
		player.setInstanceId(0);
		player.teleToLocation(loc);
		player.unsetLastLocation();
	}
	
	public static final void rewardParticipant(PlayerInstance player, int[][] reward)
	{
		if ((player == null) || !player.isOnline() || (reward == null))
		{
			return;
		}
		
		try
		{
			SystemMessage sm;
			ItemInstance item;
			final InventoryUpdate iu = new InventoryUpdate();
			for (int[] it : reward)
			{
				if ((it == null) || (it.length != 2))
				{
					continue;
				}
				
				item = player.getInventory().addItem("Olympiad", it[0], it[1], player, null);
				if (item == null)
				{
					continue;
				}
				
				iu.addModifiedItem(item);
				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
				sm.addItemName(it[0]);
				sm.addInt(it[1]);
				player.sendPacket(sm);
			}
			player.sendPacket(iu);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public abstract CompetitionType getType();
	
	public abstract String[] getPlayerNames();
	
	public abstract boolean containsParticipant(int playerId);
	
	public abstract void sendOlympiadInfo(Creature player);
	
	public abstract void broadcastOlympiadInfo(OlympiadStadiumZone stadium);
	
	protected abstract void broadcastPacket(L2GameServerPacket packet);
	
	protected abstract boolean needBuffers();
	
	protected abstract boolean checkDefaulted();
	
	protected abstract void removals();
	
	protected abstract boolean portPlayersToArena(List<Location> spawns);
	
	protected abstract void cleanEffects();
	
	protected abstract void portPlayersBack();
	
	protected abstract void playersStatusBack();
	
	protected abstract void clearPlayers();
	
	protected abstract void handleDisconnect(PlayerInstance player);
	
	protected abstract void resetDamage();
	
	protected abstract void addDamage(PlayerInstance player, int damage);
	
	protected abstract boolean checkBattleStatus();
	
	protected abstract boolean haveWinner();
	
	protected abstract void validateWinner(OlympiadStadiumZone stadium);
	
	protected abstract int getDivider();
	
	protected abstract int[][] getReward();
	
	protected abstract String getWeeklyMatchType();
}
