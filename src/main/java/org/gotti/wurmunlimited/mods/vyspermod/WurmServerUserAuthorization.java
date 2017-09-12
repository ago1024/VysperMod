package org.gotti.wurmunlimited.mods.vyspermod;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.UserAuthorization;
import org.gotti.wurmunlimited.mods.vyspermod.util.VysperPlayers;
import org.gotti.wurmunlimited.mods.vyspermod.util.VysperProperties;

public class WurmServerUserAuthorization implements UserAuthorization {

	private static final Logger LOGGER = Logger.getLogger(WurmServerUserAuthorization.class.getName());

	@Override
	public boolean verifyCredentials(Entity jid, String passwordCleartext, Object credentials) {
		final long playerId = VysperPlayers.getPlayerId(jid.getNode());
		if (playerId == -10) {
			return false;
		}
		return VysperProperties.isVysperEnabled(playerId) &&  VysperProperties.isVysperPasswordValid(playerId, passwordCleartext);
	}

	@Override
	public boolean verifyCredentials(String username, String passwordCleartext, Object credentials) {
		try {
			final Entity jid = EntityImpl.parse(username);
			return verifyCredentials(jid, passwordCleartext, credentials);
		} catch (EntityFormatException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
	}

}
