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
package org.l2junity.gameserver.ai;

import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_MOVE_TO;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_REST;

import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Creature.AIAccessor;
import org.l2junity.gameserver.model.actor.instance.L2StaticObjectInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

public class PlayerAI extends PlayableAI
{
	private boolean _thinking; // to prevent recursive thinking
	
	IntentionCommand _nextIntention = null;
	
	public PlayerAI(AIAccessor accessor)
	{
		super(accessor);
	}
	
	void saveNextIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_nextIntention = new IntentionCommand(intention, arg0, arg1);
	}
	
	@Override
	public IntentionCommand getNextIntention()
	{
		return _nextIntention;
	}
	
	/**
	 * Saves the current Intention for this L2PlayerAI if necessary and calls changeIntention in AbstractAI.
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// do nothing unless CAST intention
		// however, forget interrupted actions when starting to use an offensive skill
		if ((intention != AI_INTENTION_CAST) || ((arg0 != null) && ((Skill) arg0).isBad()))
		{
			_nextIntention = null;
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// do nothing if next intention is same as current one.
		if ((intention == _intention) && (arg0 == _intentionArg0) && (arg1 == _intentionArg1))
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// save current intention so it can be used after cast
		saveNextIntention(_intention, _intentionArg0, _intentionArg1);
		super.changeIntention(intention, arg0, arg1);
	}
	
	/**
	 * Launch actions corresponding to the Event ReadyToAct.<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Launch actions corresponding to the Event Think</li>
	 * </ul>
	 */
	@Override
	protected void onEvtReadyToAct()
	{
		// Launch actions corresponding to the Event Think
		if (_nextIntention != null)
		{
			setIntention(_nextIntention._crtlIntention, _nextIntention._arg0, _nextIntention._arg1);
			_nextIntention = null;
		}
		super.onEvtReadyToAct();
	}
	
	/**
	 * Launch actions corresponding to the Event Cancel.<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Event Think</li>
	 * </ul>
	 */
	@Override
	protected void onEvtCancel()
	{
		_nextIntention = null;
		super.onEvtCancel();
	}
	
	/**
	 * Finalize the casting of a skill. This method overrides L2CharacterAI method.<br>
	 * <B>What it does:</B><br>
	 * Check if actual intention is set to CAST and, if so, retrieves latest intention before the actual CAST and set it as the current intention for the player.
	 */
	@Override
	protected void onEvtFinishCasting()
	{
		if (getIntention() == AI_INTENTION_CAST)
		{
			// run interrupted or next intention
			
			IntentionCommand nextIntention = _nextIntention;
			if (nextIntention != null)
			{
				if (nextIntention._crtlIntention != AI_INTENTION_CAST) // previous state shouldn't be casting
				{
					setIntention(nextIntention._crtlIntention, nextIntention._arg0, nextIntention._arg1);
				}
				else
				{
					setIntention(AI_INTENTION_IDLE);
				}
			}
			else
			{
				// set intention to idle if skill doesn't change intention.
				setIntention(AI_INTENTION_IDLE);
			}
		}
	}
	
	@Override
	protected void onIntentionRest()
	{
		if (getIntention() != AI_INTENTION_REST)
		{
			changeIntention(AI_INTENTION_REST, null, null);
			setTarget(null);
			if (getAttackTarget() != null)
			{
				setAttackTarget(null);
			}
			clientStopMoving(null);
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		setIntention(AI_INTENTION_IDLE);
	}
	
	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<br>
	 * <B><U> Actions</U> : </B>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to AI_INTENTION_MOVE_TO</li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionMoveTo(Location loc)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			clientActionFailed();
			saveNextIntention(AI_INTENTION_MOVE_TO, loc, null);
			return;
		}
		
		// Set the Intention of this AbstractAI to AI_INTENTION_MOVE_TO
		changeIntention(AI_INTENTION_MOVE_TO, loc, null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Abort the attack of the L2Character and send Server->Client ActionFailed packet
		_actor.abortAttack();
		
		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		_clientMoving = false;
		
		super.clientNotifyDead();
	}
	
	private void thinkAttack()
	{
		Creature target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		if (checkTargetLostOrDead(target))
		{
			// Notify the target
			setAttackTarget(null);
			return;
		}
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		_accessor.doAttack(target);
	}
	
	private void thinkCast()
	{
		Creature target = getCastTarget();
		if ((_skill.getTargetType() == L2TargetType.GROUND) && (_actor instanceof PlayerInstance))
		{
			if (maybeMoveToPosition(((PlayerInstance) _actor).getCurrentSkillWorldPosition(), _actor.getMagicalAttackRange(_skill)))
			{
				_actor.setIsCastingNow(false);
				return;
			}
		}
		else
		{
			if (checkTargetLost(target))
			{
				if (_skill.isBad() && (getAttackTarget() != null))
				{
					// Notify the target
					setCastTarget(null);
				}
				_actor.setIsCastingNow(false);
				return;
			}
			if ((target != null) && maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
			{
				_actor.setIsCastingNow(false);
				return;
			}
		}
		
		if ((_skill.getHitTime() > 50) && !_skill.isSimultaneousCast())
		{
			clientStopMoving(null);
		}
		
		_accessor.doCast(_skill);
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		setIntention(AI_INTENTION_IDLE);
		((PlayerInstance.AIAccessor) _accessor).doPickupItem(target);
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		if (!(target instanceof L2StaticObjectInstance))
		{
			((PlayerInstance.AIAccessor) _accessor).doInteract((Creature) target);
		}
		setIntention(AI_INTENTION_IDLE);
	}
	
	@Override
	protected void onEvtThink()
	{
		if (_thinking && (getIntention() != AI_INTENTION_CAST))
		{
			return;
		}
		
		_thinking = true;
		try
		{
			if (getIntention() == AI_INTENTION_ATTACK)
			{
				thinkAttack();
			}
			else if (getIntention() == AI_INTENTION_CAST)
			{
				thinkCast();
			}
			else if (getIntention() == AI_INTENTION_PICK_UP)
			{
				thinkPickUp();
			}
			else if (getIntention() == AI_INTENTION_INTERACT)
			{
				thinkInteract();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
}
