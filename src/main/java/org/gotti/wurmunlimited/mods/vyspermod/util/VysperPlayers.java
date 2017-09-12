package org.gotti.wurmunlimited.mods.vyspermod.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;

public class VysperPlayers {
	
	private static final Logger LOGGER = Logger.getLogger(VysperPlayers.class.getName());
	
	public static long getPlayerId(String playerName) {
		try {
			return Players.getInstance().getWurmIdFor(playerName);
		} catch (NoSuchPlayerException e) {
			return -10;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			return -10;
		}
	}

}
