package org.gotti.wurmunlimited.mods.vyspermod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.vysper.xmpp.addressing.Entity;

import com.wurmonline.server.Servers;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;

/**
 * Channel mapper for player characters
 * 
 * @author ago
 */
public abstract class PlayerChannelMapper extends ChannelMapper {

	/**
	 * Get the village of the player.
	 * 
	 * @return the village of the player
	 */
	abstract Optional<Village> getVillage();

	/**
	 * Get the kingdom of the player.
	 * 
	 * @return the kingdom of the player
	 */
	abstract Optional<Kingdom> getKingdom();

	/**
	 * Create a PlayerChannelMapper.
	 * 
	 * @param chatServer
	 *            chat server entity
	 */
	protected PlayerChannelMapper(Entity chatServer) {
		super(chatServer);
	}

	/**
	 * Get the alliance room entity.
	 * 
	 * @return the alliance room entity
	 */
	public Optional<Entity> getAllianceRoom() {
		return getVillage()
				.map(village -> PvPAlliance.getPvPAlliance(village.getAllianceNumber()))
				.map(alliance -> getAllianceId(alliance));
	}

	/**
	 * Get the kingdom room entity
	 * 
	 * @return the kingdom room entity
	 */
	public Optional<Entity> getKingdomRoom() {
		return getKingdom()
				.map(kingdom -> getKingdomId(kingdom));
	}

	/**
	 * Get the global kingdom room entity
	 * 
	 * @return the global kingdom room entity
	 */
	public Optional<Entity> getGlobalKingdomRoom() {
		return getKingdom()
				.map(kingdom -> getGlobalKingdomId(kingdom));
	}

	/**
	 * Get the village room entity.
	 * 
	 * @return the village room entity
	 */
	public Optional<Entity> getVillageRoom() {
		return getVillage()
				.map(village -> getVillageId(village));
	}

	/**
	 * Get the trade room entity.
	 * 
	 * @return the trade room entity
	 */
	public Optional<Entity> getTradeRoom() {
		return getKingdom()
				.map(kingdom -> getTradeId(kingdom));
	}

	/**
	 * Get all rooms for the player.
	 * 
	 * @return a list with all available room entities
	 */
	public List<Entity> getRooms() {
		List<Entity> entities = new ArrayList<Entity>();

		getAllianceRoom().ifPresent(entities::add);
		getVillageRoom().ifPresent(entities::add);
		getKingdomRoom().ifPresent(entities::add);
		getGlobalKingdomRoom().ifPresent(entities::add);
		getTradeRoom().ifPresent(entities::add);

		return entities;
	}

	/**
	 * Create a channel mapper for a {@link PlayerInfo}
	 * 
	 * @param playerInfo
	 *            {@link PlayerInfo} object
	 * @param chatServer
	 *            chat server entity
	 * @return a {@link PlayerChannelMapper}
	 */
	public static PlayerChannelMapper create(PlayerInfo playerInfo, Entity chatServer) {
		return new PlayerChannelMapper(chatServer) {

			private Kingdom getPlayerKingdom(PlayerInfo playerInfo) {
				if (!Servers.localServer.PVPSERVER) {
					return Kingdoms.getKingdom(Kingdom.KINGDOM_FREEDOM);
				} else {
					for (Kingdom kingdom : Kingdoms.getAllKingdoms()) {
						if (kingdom.getMember(playerInfo.getPlayerId()) != null) {
							return kingdom;
						}
					}
					return null;
				}
			}

			@Override
			Optional<Village> getVillage() {
				return Optional.ofNullable(Villages.getVillageForCreature(playerInfo.getPlayerId()));
			}

			@Override
			Optional<Kingdom> getKingdom() {
				return Optional.ofNullable(getPlayerKingdom(playerInfo));
			}
		};
	}

	/**
	 * Create a channel mapper for a {@link Player}
	 * 
	 * @param player
	 *            {@link Player} object
	 * @param chatServer
	 *            chat server entity
	 * @return a {@link PlayerChannelMapper}
	 */
	public static PlayerChannelMapper create(Player player, Entity chatServer) {
		return new PlayerChannelMapper(chatServer) {

			@Override
			Optional<Village> getVillage() {
				return Optional.ofNullable(player.getCitizenVillage());
			}

			@Override
			Optional<Kingdom> getKingdom() {
				return Optional.ofNullable(Kingdoms.getKingdom(player.getKingdomId()));
			}
		};
	}
}
