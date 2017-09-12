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
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.ChannelMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerLoginListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerPollListener;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.mods.vyspermod.ui.ManageVysperAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.wurmonline.server.Message;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

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
	
	private ManageVysperAction manageVysperAction;
	
	@Override
	public void onServerStarted() {
		startServer();
		
		manageVysperAction = new ManageVysperAction();
		ModActions.registerActionPerformer(manageVysperAction);
	}

	@Override
	public void preInit() {
		LOGGER.info("preInit");
		
		try {
			// "com.wurmonline.server.behaviours.ManageMenu.getBehavioursFor(Creature)"
			final ClassPool classPool = HookManager.getInstance().getClassPool();
			final String descriptor = Descriptor.ofMethod(classPool.get(List.class.getName()), new CtClass[] { classPool.get("com.wurmonline.server.creatures.Creature") });
			final CtClass ctManageMenu = classPool.get("com.wurmonline.server.behaviours.ManageMenu");
			final CtMethod ctGetBehavioursFor = ctManageMenu.getMethod("getBehavioursFor", descriptor);
			
			final Object callback = new Object() {
				@SuppressWarnings("unused")
				public void appendManageAction(List<ActionEntry> actions) {
					if (manageVysperAction == null) {
						// Fail fast if the action is not initialized (should have been done in onServerStarted)
						return;
					}
					// Increment the submenu size if a submenu is created
					if (!actions.isEmpty() && actions.get(0).getNumber() < 0) {
						ActionEntry oldHead = actions.get(0);
						actions.set(0, new ActionEntry((short) (oldHead.getNumber() - 1), oldHead.getActionString(), oldHead.getVerbString()));
					}
					// Add the action
					actions.add(new ActionEntry(manageVysperAction.getActionId(), "Vysper", "managing"));
				}
			};
			HookManager.getInstance().addCallback(ctManageMenu, "vyspermod", callback);
			ctGetBehavioursFor.insertAfter("vyspermod.appendManageAction($_);");
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
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
			
			final Player player = (Player) message.getSender();
			String prefix = "<" + player.getName() + "> ";
			String text = message.getMessage();
			if (text.startsWith(prefix)) {
				text = text.substring(prefix.length());
			}
			
			PlayerSession session = getPlayerSession(player);
			
			Stanza stanza = new StanzaBuilder("message")
					.addAttribute("type", "groupchat")
					.addAttribute("from", session.getPlayerEntity().getFullQualifiedName())
					.addAttribute("to", room.getJID().getFullQualifiedName())
					.startInnerElement("body")
					.addText(text)
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
		if (!room.isInRoom(playerEntity)) {
			
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
		if (room.isInRoom(playerEntity)) {
			
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
