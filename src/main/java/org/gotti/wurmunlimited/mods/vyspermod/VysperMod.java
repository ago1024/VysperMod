package org.gotti.wurmunlimited.mods.vyspermod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.interfaces.ChannelMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerLoginListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerPollListener;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.wurmonline.server.Message;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;

public class VysperMod implements WurmServerMod, PreInitable, Initable, ServerStartedListener, PlayerLoginListener, ServerPollListener, ChannelMessageListener {

	private static final Logger LOGGER = Logger.getLogger(VysperMod.class.getName());
	
	private String xmppDomain = "tamias.ago.vpn"; // FIXME
	private String xmppCertificate = "mods/vyspermod/vysper.p12";
	private String xmppCertPassword = "";
	
	private String[] xmppModuleNames = {
			"org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule",
			"org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule",
			"org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingModule",
			"org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AdhocCommandsModule",
			"org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PublishSubscribeModule"
	};
	
	private StorageProviderRegistry providerRegistry;
	private XMPPServer server;

	private WurmChannelConference conference;

	private MUCModule mucModule;

	private Entity fullDomain;

	private PlayerSessionFactory sessionFactory;
	
	@Override
	public void onServerStarted() {
		startServer();
	}

	@Override
	public void preInit() {
		LOGGER.info("preInit");
	}

	private Optional<Entity> getRoom(Message message) {
		String window = message.getWindow();
		if (message.getSender() instanceof Player) {
			Player sender = (Player) message.getSender();
			PlayerChannelMapper mapper = PlayerChannelMapper.create(sender, fullDomain);
			if ("Alliance".equals(window)) {
				return mapper.getAllianceRoom();
			} else if ("Village".equals(window)) {
				return mapper.getVillageRoom();
			} else if ("Trade".equals(window)) {
				return mapper.getTradeRoom();
			} else if (Kingdoms.isKingdomChat(window)) {
				return mapper.getKingdomRoom();
			} else if (Kingdoms.isGlobalKingdomChat(window)) {
				return mapper.getGlobalKingdomRoom();
			}
		}
		return Optional.empty();
	}
	
	private void sendMessageToRooms(Message message) {
		getRoom(message).map(conference::getOrCreateRoom).ifPresent(room -> {
			
			PlayerSession session = getPlayerSession((Player) message.getSender());
			
			Stanza stanza = new StanzaBuilder("message")
					.addAttribute("type", "groupchat")
					.addAttribute("from", session.getPlayerEntity().getFullQualifiedName())
					.addAttribute("to", room.getJID().getFullQualifiedName())
					.startInnerElement("body")
					.addText(message.getMessage())
					.endInnerElement().build();
			
			session.send(stanza);
		});
	}
	
	@Override
	public void init() {
	}
	
	private void startServer() {
		providerRegistry = new WurmServerStorageProviderRegistry();
		server = new XMPPServer(xmppDomain);
		server.addEndpoint(new TCPEndpoint());
		server.setStorageProviderRegistry(providerRegistry);
		
		try {
			server.setTLSCertificateInfo(new File(xmppCertificate), xmppCertPassword);
			server.start();
			
			List<Module> xmppModules = new ArrayList<>();
			
			for (String className : xmppModuleNames) {
				try {
					xmppModules.add(Class.forName(className).asSubclass(Module.class).newInstance());
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
			}
			
			final String subDomain = "chat";
			fullDomain = EntityUtils.createComponentDomain(subDomain, server.getServerRuntimeContext());
			conference = new WurmChannelConference("WurmChannelConference", fullDomain);
			mucModule = new MUCModule(subDomain, conference);
			xmppModules.add(mucModule);
			
			final String gameSubDomain = "game";
			this.sessionFactory = new PlayerSessionFactory(server, gameSubDomain);
			
			((DefaultServerRuntimeContext)server.getServerRuntimeContext()).addModules(xmppModules);
			
			final ChannelMapper channelMapper = new ChannelMapper(fullDomain);
			
			for (Village village : Villages.getVillages()) {
				final Entity villageId = channelMapper.getVillageId(village);
				conference.findOrCreateRoom(villageId.getBareJID(), villageId.getResource());
			}
			for (PvPAlliance alliance : PvPAlliance.getAllAlliances()) {
				final Entity allianceId = channelMapper.getAllianceId(alliance);
				conference.findOrCreateRoom(allianceId.getBareJID(), allianceId.getResource());
			}
			for (Kingdom kingdom : Kingdoms.getAllKingdoms()) {
				final Entity tradeId = channelMapper.getTradeId(kingdom);
				conference.findOrCreateRoom(tradeId.getBareJID(), tradeId.getResource());
				
				final Entity kingdomId = channelMapper.getKingdomId(kingdom);
				conference.findOrCreateRoom(kingdomId.getBareJID(), kingdomId.getResource());
				
				final Entity glKingdomId = channelMapper.getGlobalKingdomId(kingdom);
				conference.findOrCreateRoom(glKingdomId.getBareJID(), glKingdomId.getResource());
			}
			
			final Entity gameDomain = EntityUtils.createComponentDomain(gameSubDomain, server.getServerRuntimeContext());
			((DefaultServerRuntimeContext)server.getServerRuntimeContext()).registerComponent(new GameStanzaProcessor(conference, gameDomain, gameSubDomain, fullDomain));
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}
	
	private void addPlayerToRoom(Player player, Entity roomEntity) {
		PlayerSession session = getPlayerSession(player);
		Entity playerEntity = session.getPlayerEntity();

		Room room = conference.getOrCreateRoom(roomEntity);
		if (!room.isInRoom(playerEntity.getBareJID())) {
			
			Stanza stanza = new StanzaBuilder("presence")
					.addAttribute("from", playerEntity.getBareJID().getFullQualifiedName())
					.addAttribute("to", new EntityImpl(room.getJID().getBareJID(), playerEntity.getNode()).getFullQualifiedName())
					.build();
			
			session.send(stanza);
		}
	}
	
	private void removePlayerFromRoom(Player player, Entity roomEntity) {
		PlayerSession session = getPlayerSession(player);
		Entity playerEntity = session.getPlayerEntity();
		
		Room room = conference.getOrCreateRoom(roomEntity);
		if (room.isInRoom(playerEntity.getBareJID())) {
			
			Stanza stanza = new StanzaBuilder("presence")
					.addAttribute("from", playerEntity.getBareJID().getFullQualifiedName())
					.addAttribute("to", new EntityImpl(room.getJID().getBareJID(), playerEntity.getNode()).getFullQualifiedName())
					.addAttribute("type", "unavailable")
					.build();
			
			session.send(stanza);
		}
	}
	
	private ConcurrentHashMap<String, PlayerSession> playerSessions = new ConcurrentHashMap<>();

	private PlayerSession getPlayerSession(Player player) {
		return playerSessions.computeIfAbsent(player.getName(), name -> sessionFactory.createSession(player));
	}

	@Override
	public void onPlayerLogin(Player player) {
		PlayerChannelMapper.create(player, fullDomain).getRooms()
			.forEach(roomEntity -> addPlayerToRoom(player, roomEntity));
	}
	
	@Override
	public void onPlayerLogout(Player player) {
		PlayerChannelMapper.create(player, fullDomain).getRooms().stream()
			.forEach(roomEntity -> removePlayerFromRoom(player, roomEntity));
	}
	
	private void pollSession(PlayerSession session) {
		Stanza stanza;
		while ((stanza = session.poll()) != null) {
			LOGGER.log(Level.INFO, stanza.toString());
		}
	}
	
	@Override
	public void onServerPoll() {
		playerSessions.forEachValue(Long.MAX_VALUE, this::pollSession);
	}
	
	@Override
	public MessagePolicy onAllianceMessage(PvPAlliance alliance, Message message) {
		sendMessageToRooms(message);
		return MessagePolicy.PASS;
	}
	
	@Override
	public MessagePolicy onKingdomMessage(Message message) {
		sendMessageToRooms(message);
		return MessagePolicy.PASS;
	}
	
	@Override
	public MessagePolicy onVillageMessage(Village village, Message message) {
		sendMessageToRooms(message);
		return MessagePolicy.PASS;
	}
}
