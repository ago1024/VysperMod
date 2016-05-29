package org.gotti.wurmunlimited.mods.vyspermod;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;

import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;

/**
 * Map game entities to XMPP channel entities.
 *
 * @author ago
 */
public class ChannelMapper {

	/**
	 * chat server entity.
	 */
	private Entity chatServer;

	/**
	 * Create a ChannelMapper.
	 * 
	 * @param chatServer
	 *            chat server entity. The chat rooms are created as entities of the chat server.
	 */
	public ChannelMapper(Entity chatServer) {
		this.chatServer = chatServer;
	}

	/**
	 * Create a chat room entity for a village
	 * 
	 * @param village
	 *            Village
	 * @return chat room entity
	 */
	public Entity getVillageId(Village village) {
		final String roomName = String.format("village%d", village.getId());
		final Entity roomId = createRoomEntity(roomName, village.getName());
		return roomId;
	}

	/**
	 * Create a chat room entity for a kingdom
	 * 
	 * @param kingdom
	 *            Kingdom
	 * @return chat room entity
	 */
	public Entity getKingdomId(Kingdom kingdom) {
		final String roomName = String.format("kingdom%d", kingdom.getId());
		final Entity roomId = createRoomEntity(roomName, kingdom.getName());
		return roomId;
	}

	/**
	 * Create a global chat room entity for a kingdom
	 * 
	 * @param kingdom
	 *            Kingdom
	 * @return chat room entity
	 */
	public Entity getGlobalKingdomId(Kingdom kingdom) {
		final String roomName = String.format("glkingdom%d", kingdom.getId());
		final Entity roomId = createRoomEntity(roomName, kingdom.getName());
		return roomId;
	}

	/**
	 * Create a chat room entity for an alliance
	 * 
	 * @param alliance
	 *            Alliance
	 * @return chat room entity
	 */
	public Entity getAllianceId(PvPAlliance alliance) {
		final String roomName = String.format("alliance%d", alliance.getId());
		final Entity roomId = createRoomEntity(roomName, alliance.getName());
		return roomId;
	}

	/**
	 * Create a trade chat room entity for a kingdom
	 * 
	 * @param kingdom
	 *            Kingdom
	 * @return chat room entity
	 */
	public Entity getTradeId(Kingdom kingdom) {
		final String roomName = String.format("trade%d", kingdom.getId());
		final Entity roomId = createRoomEntity(roomName, kingdom.getName());
		return roomId;
	}

	/**
	 * Create a room entity.
	 * @param roomId Room identifier
	 * @param roomName Room name
	 * @return Room entity
	 */
	private Entity createRoomEntity(final String roomId, final String roomName) {
		return new EntityImpl(roomId, chatServer.getDomain(), roomName);
	}
}
