package org.gotti.wurmunlimited.mods.vyspermod;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;

import com.wurmonline.server.Message;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;
import com.wurmonline.server.players.Player;

public class GameStanzaProcessor implements StanzaProcessor, Component {

	private static final Logger LOGGER = Logger.getLogger(GameStanzaProcessor.class.getName());

	private Conference conference;
	private Entity gameDomain;
	private Entity chatDomain;
	private String gameSubDomain;
	
	public GameStanzaProcessor(WurmChannelConference conference, Entity gameDomain, String gameSubDomain, Entity chatDomain) {
		this.conference = conference;
		this.gameDomain = gameDomain;
		this.chatDomain = chatDomain;
		this.gameSubDomain = gameSubDomain;
	}

	private boolean matches(Entity channel, Optional<Entity> roomJid) {
		return roomJid
				.filter(entity -> channel.equals(entity.getBareJID()))
				.isPresent();
		
	}
	
	@Override
	public void processTLSEstablished(SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
	}
	
	@Override
	public void processStanza(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, Stanza stanza, SessionStateHolder sessionStateHolder) {
		try {
			if (!MessageStanza.isOfType(stanza)) {
				return;
			}
			MessageStanza messageStanza = new MessageStanza(stanza);
			Room room = conference.getRoomStorageProvider().findRoom(messageStanza.getFrom().getBareJID());
			if (room == null) {
				return;
			}
			
			final Player player = Players.getInstance().getPlayer(messageStanza.getTo().getNode());
			final String fromNick = messageStanza.getFrom().getResource();
			
			Occupant occupant = room.findOccupantByNick(fromNick);
			if (occupant == null) {
				return;
			}
			
			if (occupant.getJid().getDomain().equals(gameDomain.getDomain())) {
				// Skip messages from ingame senders
				return;
			}
			
			byte messageType;
			String messageWindow;
			
			PlayerChannelMapper mapper = PlayerChannelMapper.create(player, chatDomain);
			if (matches(messageStanza.getFrom().getBareJID(), mapper.getAllianceRoom())) {
				messageType = Message.ALLIANCE;
				messageWindow = "Alliance";
			} else if (matches(messageStanza.getFrom().getBareJID(), mapper.getVillageRoom())) {
				messageType = Message.VILLAGE;
				messageWindow = "Village";
			} else if (matches(messageStanza.getFrom().getBareJID(), mapper.getTradeRoom())) {
				messageType = Message.TRADE;
				messageWindow = "Trade";
			} else {
				messageType = Message.TELL;
				messageWindow = room.getName();
				for (Kingdom kingdom : Kingdoms.getAllKingdoms()) {
					if (matches(messageStanza.getFrom().getBareJID(), Optional.of(mapper.getKingdomId(kingdom)))) {
						messageType = Message.KINGDOM;
						messageWindow = kingdom.getChatName();
					} else if (matches(messageStanza.getFrom().getBareJID(), Optional.of(mapper.getGlobalKingdomId(kingdom)))) {
						messageType = Message.GLOBKINGDOM;
						messageWindow = "GL-" + kingdom.getChatName();
					} else if (matches(messageStanza.getFrom().getBareJID(), Optional.of(mapper.getTradeId(kingdom)))) {
						messageType = Message.TRADE;
						messageWindow = "Trade";
					} else {
						continue;
					}
					break;
				}
			}
			Message message = new Message(player, messageType, messageWindow, "<" + fromNick + "> "+ messageStanza.getSingleInnerElementsNamed("body").getInnerText().getText());
			player.getCommunicator().sendMessage(message);
		} catch (NoSuchPlayerException | XMLSemanticError e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}

	@Override
	public String getSubdomain() {
		return gameSubDomain;
	}
	
	@Override
	public StanzaProcessor getStanzaProcessor() {
		return this;
	}
	
}
