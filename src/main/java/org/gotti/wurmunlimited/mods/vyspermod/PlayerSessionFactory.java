package org.gotti.wurmunlimited.mods.vyspermod;


import org.apache.vysper.stanzasession.StanzaSessionContext;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.EntityUtils;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;

import com.wurmonline.server.players.Player;

public class PlayerSessionFactory {

	private XMPPServer server;
	private Entity gameDomain;

	public PlayerSessionFactory(XMPPServer server, String subDomain) {
		this.server = server;
		this.gameDomain = EntityUtils.createComponentDomain(subDomain, server.getServerRuntimeContext());
	}
	
	
	public PlayerSession createSession(Player player) {

		SessionStateHolder stateHolder = new SessionStateHolder();
		stateHolder.setState(SessionState.INITIATED);
		StanzaSessionContext sessionContext = new StanzaSessionContext(server.getServerRuntimeContext(), stateHolder);

		Entity initiatingEntity = new EntityImpl(player.getName(), gameDomain.getDomain(), null);
		sessionContext.setInitiatingEntity(initiatingEntity);
		stateHolder.setState(SessionState.AUTHENTICATED);
		
		Entity boundEntity;
		try {
			String resourceId = sessionContext.bindResource();
			 boundEntity = new EntityImpl(initiatingEntity, resourceId);
		} catch (BindException e) {
			throw new RuntimeException(e);
		}
		
		IdSequence sequence = new IdSequence(player.getName());
		final PlayerSession vysperPlayerSession = new PlayerSession(sessionContext, player, boundEntity, sequence);
		
		return vysperPlayerSession;
	}
}
