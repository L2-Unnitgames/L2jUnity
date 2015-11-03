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
package org.l2junity.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.commons.util.CommonUtil;
import org.l2junity.gameserver.GameTimeController;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.ItemLocation;
import org.l2junity.gameserver.model.TradeItem;
import org.l2junity.gameserver.model.TradeList;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerItemAdd;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerItemDestroy;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerItemDrop;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerItemTransfer;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.items.type.EtcItemType;
import org.l2junity.gameserver.model.variables.ItemVariables;
import org.l2junity.gameserver.network.client.send.InventoryUpdate;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcInventory extends Inventory
{
	private static final Logger _log = LoggerFactory.getLogger(PcInventory.class);
	
	private final PlayerInstance _owner;
	private ItemInstance _adena;
	private ItemInstance _ancientAdena;
	private ItemInstance _beautyTickets;
	
	private int[] _blockItems = null;
	
	private int _questSlots;
	
	private final Object _lock;
	/**
	 * Block modes:
	 * <UL>
	 * <LI>-1 - no block
	 * <LI>0 - block items from _invItems, allow usage of other items
	 * <LI>1 - allow usage of items from _invItems, block other items
	 * </UL>
	 */
	private int _blockMode = -1;
	
	public PcInventory(PlayerInstance owner)
	{
		_owner = owner;
		_lock = new Object();
	}
	
	@Override
	public PlayerInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	public ItemInstance getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public long getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public long getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	public ItemInstance getBeautyTicketsInstance()
	{
		return _beautyTickets;
	}
	
	@Override
	public long getBeautyTickets()
	{
		return _beautyTickets != null ? _beautyTickets.getCount() : 0;
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @return L2ItemInstance : items in inventory
	 */
	public Collection<ItemInstance> getUniqueItems(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItems(allowAdena, allowAncientAdena, true);
	}
	
	public Collection<ItemInstance> getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		final Collection<ItemInstance> list = new LinkedList<>();
		for (ItemInstance item : _items.values())
		{
			if ((!allowAdena && (item.getId() == ADENA_ID)))
			{
				continue;
			}
			if ((!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID)))
			{
				continue;
			}
			boolean isDuplicate = false;
			for (ItemInstance litem : list)
			{
				if (litem.getId() == item.getId())
				{
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false))))
			{
				list.add(item);
			}
		}
		
		return list;
	}
	
	/**
	 * Returns the list of items in inventory available for transaction Allows an item to appear twice if and only if there is a difference in enchantment level.
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @return L2ItemInstance : items in inventory
	 */
	public Collection<ItemInstance> getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItemsByEnchantLevel(allowAdena, allowAncientAdena, true);
	}
	
	public Collection<ItemInstance> getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		final Collection<ItemInstance> list = new LinkedList<>();
		for (ItemInstance item : _items.values())
		{
			if ((!allowAdena && (item.getId() == ADENA_ID)))
			{
				continue;
			}
			if ((!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID)))
			{
				continue;
			}
			
			boolean isDuplicate = false;
			for (ItemInstance litem : list)
			{
				if ((litem.getId() == item.getId()) && (litem.getEnchantLevel() == item.getEnchantLevel()))
				{
					isDuplicate = true;
					break;
				}
			}
			
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false))))
			{
				list.add(item);
			}
		}
		
		return list;
	}
	
	/**
	 * @param itemId
	 * @return
	 */
	public Collection<ItemInstance> getAllItemsByItemId(int itemId)
	{
		return getAllItemsByItemId(itemId, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * @param itemId : ID of item
	 * @param includeEquipped : include equipped items
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public Collection<ItemInstance> getAllItemsByItemId(int itemId, boolean includeEquipped)
	{
		return getItems(i -> (i.getId() == itemId) && (includeEquipped || !i.isEquipped()));
	}
	
	/**
	 * @param itemId
	 * @param enchantment
	 * @return
	 */
	public Collection<ItemInstance> getAllItemsByItemId(int itemId, int enchantment)
	{
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * @param itemId : ID of item
	 * @param enchantment : enchant level of item
	 * @param includeEquipped : include equipped items
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public Collection<ItemInstance> getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped)
	{
		return getItems(i -> (i.getId() == itemId) && (i.getEnchantLevel() == enchantment) && (includeEquipped || !i.isEquipped()));
	}
	
	/**
	 * @param allowAdena
	 * @param allowNonTradeable
	 * @param feightable
	 * @return the list of items in inventory available for transaction
	 */
	public Collection<ItemInstance> getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean feightable)
	{
		return getItems(i ->
		{
			if (!i.isAvailable(getOwner(), allowAdena, allowNonTradeable) || !canManipulateWithItemId(i.getId()))
			{
				return false;
			}
			else if (feightable)
			{
				return (i.getItemLocation() == ItemLocation.INVENTORY) && i.isFreightable();
			}
			return true;
		});
	}
	
	/**
	 * Returns the list of items in inventory available for transaction adjusted by tradeList
	 * @param tradeList
	 * @return L2ItemInstance : items in inventory
	 */
	public Collection<TradeItem> getAvailableItems(TradeList tradeList)
	{
		//@formatter:off
		return _items.values().stream()
			.filter(i -> i.isAvailable(getOwner(), false, false))
			.map(tradeList::adjustAvailableItem)
			.collect(Collectors.toCollection(LinkedList::new));
		//@formatter:on
	}
	
	/**
	 * Adjust TradeItem according his status in inventory
	 * @param item : L2ItemInstance to be adjusted
	 */
	public void adjustAvailableItem(TradeItem item)
	{
		boolean notAllEquipped = false;
		for (ItemInstance adjItem : getItemsByItemId(item.getItem().getId()))
		{
			if (adjItem.isEquipable())
			{
				if (!adjItem.isEquipped())
				{
					notAllEquipped |= true;
				}
			}
			else
			{
				notAllEquipped |= true;
				break;
			}
		}
		if (notAllEquipped)
		{
			ItemInstance adjItem = getItemByItemId(item.getItem().getId());
			item.setObjectId(adjItem.getObjectId());
			item.setEnchant(adjItem.getEnchantLevel());
			
			if (adjItem.getCount() < item.getCount())
			{
				item.setCount(adjItem.getCount());
			}
			
			return;
		}
		
		item.setCount(0);
	}
	
	/**
	 * Adds adena to PcInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(String process, long count, PlayerInstance actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Adds Beauty Tickets to PcInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of Beauty Tickets to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addBeautyTickets(String process, long count, PlayerInstance actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, BEAUTY_TICKET_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes adena to PcInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return boolean : true if adena was reduced
	 */
	public boolean reduceAdena(String process, long count, PlayerInstance actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, ADENA_ID, count, actor, reference) != null;
		}
		return false;
	}
	
	/**
	 * Removes Beauty Tickets to PcInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of Beauty Tickets to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return boolean : true if adena was reduced
	 */
	public boolean reduceBeautyTickets(String process, long count, PlayerInstance actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, BEAUTY_TICKET_ID, count, actor, reference) != null;
		}
		return false;
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAncientAdena(String process, long count, PlayerInstance actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return boolean : true if adena was reduced
	 */
	public boolean reduceAncientAdena(String process, long count, PlayerInstance actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference) != null;
		}
		return false;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance addItem(String process, ItemInstance item, PlayerInstance actor, Object reference)
	{
		item = super.addItem(process, item, actor, reference);
		
		if (item != null)
		{
			if ((item.getId() == ADENA_ID) && !item.equals(_adena))
			{
				_adena = item;
			}
			else if ((item.getId() == ANCIENT_ADENA_ID) && !item.equals(_ancientAdena))
			{
				_ancientAdena = item;
			}
			else if ((item.getId() == BEAUTY_TICKET_ID) && !item.equals(_beautyTickets))
			{
				_beautyTickets = item;
			}
			
			if (actor != null)
			{
				// Send inventory update packet
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(item);
					actor.sendInventoryUpdate(playerIU);
				}
				else
				{
					actor.sendItemList(false);
				}
				
				// Notify to scripts
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, item), item.getItem());
			}
		}
		
		return item;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param actor : L2PcInstance Player requesting the item creation
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance addItem(String process, int itemId, long count, PlayerInstance actor, Object reference)
	{
		ItemInstance item = super.addItem(process, itemId, count, actor, reference);
		if (item != null)
		{
			if ((item.getId() == ADENA_ID) && !item.equals(_adena))
			{
				_adena = item;
			}
			else if ((item.getId() == ANCIENT_ADENA_ID) && !item.equals(_ancientAdena))
			{
				_ancientAdena = item;
			}
			else if ((item.getId() == BEAUTY_TICKET_ID) && !item.equals(_beautyTickets))
			{
				_beautyTickets = item;
			}
		}
		
		if ((item != null) && (actor != null))
		{
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(item);
				actor.sendInventoryUpdate(playerIU);
			}
			else
			{
				actor.sendItemList(false);
			}
			
			// Notify to scripts
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, item), item.getItem());
		}
		
		return item;
	}
	
	/**
	 * Transfers item to another inventory and checks _adena and _ancientAdena
	 * @param process string Identifier of process triggering this action
	 * @param objectId Item Identifier of the item to be transfered
	 * @param count Quantity of items to be transfered
	 * @param target the item container for the item to be transfered.
	 * @param actor the player requesting the item transfer
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, PlayerInstance actor, Object reference)
	{
		ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemTransfer(actor, item, target), item.getItem());
		return item;
	}
	
	@Override
	public ItemInstance detachItem(String process, ItemInstance item, long count, ItemLocation newLocation, PlayerInstance actor, Object reference)
	{
		item = super.detachItem(process, item, count, newLocation, actor, reference);
		
		if ((item != null) && (actor != null))
		{
			actor.sendItemList(false);
		}
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, PlayerInstance actor, Object reference)
	{
		return this.destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, long count, PlayerInstance actor, Object reference)
	{
		item = super.destroyItem(process, item, count, actor, reference);
		
		if ((_adena != null) && (_adena.getCount() <= 0))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && (_ancientAdena.getCount() <= 0))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (item != null)
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDestroy(actor, item), item.getItem());
		}
		return item;
	}
	
	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, int objectId, long count, PlayerInstance actor, Object reference)
	{
		ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
		{
			return null;
		}
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItemByItemId(String process, int itemId, long count, PlayerInstance actor, Object reference)
	{
		ItemInstance item = getItemByItemId(itemId);
		if (item == null)
		{
			return null;
		}
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance dropItem(String process, ItemInstance item, PlayerInstance actor, Object reference)
	{
		item = super.dropItem(process, item, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (item != null)
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, item, item.getLocation()), item.getItem());
		}
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance dropItem(String process, int objectId, long count, PlayerInstance actor, Object reference)
	{
		ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (item != null)
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, item, item.getLocation()), item.getItem());
		}
		return item;
	}
	
	/**
	 * <b>Overloaded</b>, when removes item from inventory, remove also owner shortcuts.
	 * @param item : L2ItemInstance to be removed from inventory
	 */
	@Override
	protected boolean removeItem(ItemInstance item)
	{
		// Removes any reference to the item from Shortcut bar
		getOwner().removeItemFromShortCut(item.getObjectId());
		
		// Removes active Enchant Scroll
		if (getOwner().isProcessingItem(item.getObjectId()))
		{
			getOwner().removeRequestsThatProcessesItem(item.getObjectId());
		}
		
		if (item.getId() == ADENA_ID)
		{
			_adena = null;
		}
		else if (item.getId() == ANCIENT_ADENA_ID)
		{
			_ancientAdena = null;
		}
		else if (item.getId() == BEAUTY_TICKET_ID)
		{
			_beautyTickets = null;
		}
		
		if (item.isQuestItem())
		{
			synchronized (_lock)
			{
				_questSlots--;
				if (_questSlots < 0)
				{
					_questSlots = 0;
					_log.warn(this + ": QuestInventory size < 0!");
				}
			}
		}
		return super.removeItem(item);
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	public void refreshWeight()
	{
		super.refreshWeight();
		getOwner().refreshOverloaded(true);
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
		_beautyTickets = getItemByItemId(BEAUTY_TICKET_ID);
	}
	
	public static int[][] restoreVisibleInventory(int objectId)
	{
		int[][] paperdoll = new int[33][4];
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement2 = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'"))
		{
			statement2.setInt(1, objectId);
			try (ResultSet invdata = statement2.executeQuery())
			{
				while (invdata.next())
				{
					int slot = invdata.getInt("loc_data");
					final ItemVariables vars = new ItemVariables(invdata.getInt("object_id"));
					paperdoll[slot][0] = invdata.getInt("object_id");
					paperdoll[slot][1] = invdata.getInt("item_id");
					paperdoll[slot][2] = invdata.getInt("enchant_level");
					paperdoll[slot][3] = vars.getInt(ItemVariables.VISUAL_ID, 0);
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Could not restore inventory: " + e.getMessage(), e);
		}
		return paperdoll;
	}
	
	/**
	 * @param itemList the items that needs to be validated.
	 * @param sendMessage if {@code true} will send a message of inventory full.
	 * @param sendSkillMessage if {@code true} will send a message of skill not available.
	 * @return {@code true} if the inventory isn't full after taking new items and items weight add to current load doesn't exceed max weight load.
	 */
	public boolean checkInventorySlotsAndWeight(List<L2Item> itemList, boolean sendMessage, boolean sendSkillMessage)
	{
		int lootWeight = 0;
		int requiredSlots = 0;
		if (itemList != null)
		{
			for (L2Item item : itemList)
			{
				// If the item is not stackable or is stackable and not present in inventory, will need a slot.
				if (!item.isStackable() || (getInventoryItemCount(item.getId(), -1) <= 0))
				{
					requiredSlots++;
				}
				lootWeight += item.getWeight();
			}
		}
		
		boolean inventoryStatusOK = validateCapacity(requiredSlots) && validateWeight(lootWeight);
		if (!inventoryStatusOK && sendMessage)
		{
			_owner.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			if (sendSkillMessage)
			{
				_owner.sendPacket(SystemMessageId.WEIGHT_AND_VOLUME_LIMIT_HAVE_BEEN_EXCEEDED_THAT_SKILL_IS_CURRENTLY_UNAVAILABLE);
			}
		}
		return inventoryStatusOK;
	}
	
	/**
	 * If the item is not stackable or is stackable and not present in inventory, will need a slot.
	 * @param item the item to validate.
	 * @return {@code true} if there is enough room to add the item inventory.
	 */
	public boolean validateCapacity(ItemInstance item)
	{
		int slots = 0;
		if (!item.isStackable() || (getInventoryItemCount(item.getId(), -1) <= 0) || !item.getItem().hasExImmediateEffect())
		{
			slots++;
		}
		return validateCapacity(slots, item.isQuestItem());
	}
	
	/**
	 * If the item is not stackable or is stackable and not present in inventory, will need a slot.
	 * @param itemId the item Id for the item to validate.
	 * @return {@code true} if there is enough room to add the item inventory.
	 */
	public boolean validateCapacityByItemId(int itemId)
	{
		int slots = 0;
		final ItemInstance invItem = getItemByItemId(itemId);
		if ((invItem == null) || !invItem.isStackable())
		{
			slots++;
		}
		return validateCapacity(slots, ItemTable.getInstance().getTemplate(itemId).isQuestItem());
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		return validateCapacity(slots, false);
	}
	
	public boolean validateCapacity(long slots, boolean questItem)
	{
		if (!questItem)
		{
			return (((_items.size() - _questSlots) + slots) <= _owner.getInventoryLimit());
		}
		return (_questSlots + slots) <= _owner.getQuestInventoryLimit();
	}
	
	@Override
	public boolean validateWeight(long weight)
	{
		// Disable weight check for GMs.
		if (_owner.isGM() && _owner.getDietMode() && _owner.getAccessLevel().allowTransaction())
		{
			return true;
		}
		return ((_totalWeight + weight) <= _owner.getMaxLoad());
	}
	
	/**
	 * Set inventory block for specified IDs<br>
	 * array reference is used for {@link PcInventory#_blockItems}
	 * @param items array of Ids to block/allow
	 * @param mode blocking mode {@link PcInventory#_blockMode}
	 */
	public void setInventoryBlock(int[] items, int mode)
	{
		_blockMode = mode;
		_blockItems = items;
		
		_owner.sendItemList(false);
	}
	
	/**
	 * Unblock blocked itemIds
	 */
	public void unblock()
	{
		_blockMode = -1;
		_blockItems = null;
		
		_owner.sendItemList(false);
	}
	
	/**
	 * Check if player inventory is in block mode.
	 * @return true if some itemIds blocked
	 */
	public boolean hasInventoryBlock()
	{
		return ((_blockMode > -1) && (_blockItems != null) && (_blockItems.length > 0));
	}
	
	/**
	 * Block all player items
	 */
	public void blockAllItems()
	{
		// temp fix, some id must be sended
		setInventoryBlock(new int[]
		{
			(ItemTable.getInstance().getArraySize() + 2)
		}, 1);
	}
	
	/**
	 * Return block mode
	 * @return int {@link PcInventory#_blockMode}
	 */
	public int getBlockMode()
	{
		return _blockMode;
	}
	
	/**
	 * Return TIntArrayList with blocked item ids
	 * @return TIntArrayList
	 */
	public int[] getBlockItems()
	{
		return _blockItems;
	}
	
	/**
	 * Check if player can use item by itemid
	 * @param itemId int
	 * @return true if can use
	 */
	public boolean canManipulateWithItemId(int itemId)
	{
		if (((_blockMode == 0) && CommonUtil.contains(_blockItems, itemId)) || ((_blockMode == 1) && !CommonUtil.contains(_blockItems, itemId)))
		{
			return false;
		}
		return true;
	}
	
	@Override
	protected void addItem(ItemInstance item)
	{
		if (item.isQuestItem())
		{
			synchronized (_lock)
			{
				_questSlots++;
			}
		}
		super.addItem(item);
	}
	
	public int getSize(boolean quest)
	{
		if (quest)
		{
			return _questSlots;
		}
		return getSize() - _questSlots;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
	
	/**
	 * Apply skills of inventory items
	 */
	public void applyItemSkills()
	{
		for (ItemInstance item : _items.values())
		{
			item.giveSkillsToOwner();
			item.applyEnchantStats();
			item.applySpecialAbilities();
		}
	}
	
	/**
	 * Reduce the number of arrows/bolts owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consumed).
	 * @param type
	 */
	@Override
	public void reduceArrowCount(EtcItemType type)
	{
		if ((type != EtcItemType.ARROW) && (type != EtcItemType.BOLT))
		{
			_log.warn(type.toString(), " which is not arrow type passed to PlayerInstance.reduceArrowCount()");
			return;
		}
		
		final ItemInstance arrows = getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		
		if ((arrows == null) || (arrows.getItemType() != type))
		{
			return;
		}
		
		if (arrows.getEtcItem().isInfinite()) // Null-safe due to type checks above
		{
			return;
		}
		
		if ((GameTimeController.getInstance().getGameTicks() % 10) == 0)
		{
			updateItemCount(null, arrows, -1, _owner, null);
		}
		else
		{
			updateItemCountNoDbUpdate(null, arrows, -1, _owner, null);
		}
	}
	
	/**
	 * Reduces item count in the stack, destroys item when count reaches 0.
	 * @param process
	 * @param item
	 * @param countDelta Adds items to stack if positive, reduces if negative. If stack count reaches 0 item is destroyed.
	 * @param creator
	 * @param reference
	 * @return Amount of items left.
	 */
	public boolean updateItemCountNoDbUpdate(String process, ItemInstance item, long countDelta, PlayerInstance creator, Object reference)
	{
		final InventoryUpdate iu = new InventoryUpdate();
		final long left = item.getCount() + countDelta;
		try
		{
			if (left > 0)
			{
				synchronized (item)
				{
					if ((process != null) && (process.length() > 0))
					{
						item.changeCount(process, countDelta, creator, reference);
					}
					else
					{
						item.changeCountWithoutTrace(-1, creator, reference);
					}
					item.setLastChange(ItemInstance.MODIFIED);
					refreshWeight();
					iu.addModifiedItem(item);
					return true;
				}
			}
			else if (left == 0)
			{
				iu.addRemovedItem(item);
				destroyItem(process, item, _owner, null);
				return true;
			}
			else
			{
				return false;
			}
		}
		finally
		{
			if (Config.FORCE_INVENTORY_UPDATE)
			{
				_owner.sendItemList(false);
			}
			else
			{
				_owner.sendInventoryUpdate(iu);
			}
		}
	}
	
	/**
	 * Reduces item count in the stack, destroys item when count reaches 0.
	 * @param process
	 * @param item
	 * @param countDelta Adds items to stack if positive, reduces if negative. If stack count reaches 0 item is destroyed.
	 * @param creator
	 * @param reference
	 * @return Amount of items left.
	 */
	public boolean updateItemCount(String process, ItemInstance item, long countDelta, PlayerInstance creator, Object reference)
	{
		if (item != null)
		{
			try
			{
				return updateItemCountNoDbUpdate(process, item, countDelta, creator, reference);
			}
			finally
			{
				if (item.getCount() > 0)
				{
					item.updateDatabase();
				}
			}
		}
		return false;
	}
}
