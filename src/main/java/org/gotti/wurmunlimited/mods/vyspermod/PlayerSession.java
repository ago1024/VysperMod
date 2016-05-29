package org.gotti.wurmunlimited.mods.vyspermod;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.vysper.stanzasession.StanzaSessionContext;
import org.apache.vysper.xml.fragment.Renderer;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.stanza.Stanza;

import com.wurmonline.server.players.Player;

public class PlayerSession {

	private static final Logger LOGGER = Logger.getLogger(PlayerSession.class.getName());
	private StanzaSessionContext sessionContext;
	private Entity entity;
	private IdSequence sequence;

	protected PlayerSession(StanzaSessionContext sessionContext, Player player, Entity boundEntity, IdSequence sequence) {
		this.sessionContext = sessionContext;
		this.entity = boundEntity;
		this.sequence = sequence;
	}

	public void send(Stanza stanza) {
		logStanza(stanza);
		sessionContext.sendStanzaToServer(stanza);
	}

	public void setIsSecure() {
		sessionContext.switchToTLS(true, false);
	}

	public Stanza poll() {
		Stanza stanza = sessionContext.getNextStanza();
		if (stanza != null) {
			logStanza(stanza);
		}
		return stanza;
	}

	public Entity getPlayerEntity() {
		return entity;
	}
	
	public IdSequence getIdSequence() {
		return sequence;
	}
	
	private void logStanza(Stanza stanza) {
		LOGGER.log(Level.INFO, "\n" + new Renderer(stanza).getComplete());
	}
}
