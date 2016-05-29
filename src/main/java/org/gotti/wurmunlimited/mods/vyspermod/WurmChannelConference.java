package org.gotti.wurmunlimited.mods.vyspermod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.Item;

import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.villages.PvPAlliance;

public class WurmChannelConference extends Conference {

	private Entity fullDomain;

	public WurmChannelConference(String name, Entity fullDomain) {
		super(name);
		this.fullDomain = fullDomain;
	}

	private Item getItem(Room room) {
		return new Item(room.getJID(), room.getName());
	}

	@Override
	public List<Item> getItemsFor(InfoRequest request) {
		String playerName = request.getFrom().getNode();
		return getRooms(playerName).stream()
				.map(this::getOrCreateRoom)
				.map(this::getItem)
				.collect(Collectors.toList());
	}

	public Room getOrCreateRoom(final Entity roomId) {
		return findOrCreateRoom(roomId.getBareJID(), roomId.getResource());
	}

	public Entity getKingdomId(Kingdom kingdom) {
		return new ChannelMapper(fullDomain).getKingdomId(kingdom);
	}

	public Entity getAllianceId(PvPAlliance alliance) {
		return new ChannelMapper(fullDomain).getAllianceId(alliance);
	}

	private List<Entity> getRooms(String playerName) {
		try {
			final Player player = Players.getInstance().getPlayer(playerName);
			return PlayerChannelMapper.create(player, fullDomain).getRooms();
		} catch (NoSuchPlayerException e) {
		}
		try {
			final PlayerInfo playerInfo = PlayerInfoFactory.createPlayerInfo(playerName);
			playerInfo.load();
			return PlayerChannelMapper.create(playerInfo, fullDomain).getRooms();
		} catch (IOException e) {
		}
		return new ArrayList<Entity>();
	}
}
