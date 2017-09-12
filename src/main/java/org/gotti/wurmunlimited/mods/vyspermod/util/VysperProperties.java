package org.gotti.wurmunlimited.mods.vyspermod.util;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.properties.ModPlayerProperties;
import org.gotti.wurmunlimited.modsupport.properties.Property;

public class VysperProperties {
	
	private static final Logger LOGGER = Logger.getLogger(VysperProperties.class.getName());

	private static final ModPlayerProperties modProps = ModPlayerProperties.getInstance();
	
	private static final String PROPERTY_VYSPER_ENABLED = "vysper.enabled";
	private static final String PROPERTY_VYSPER_HASH = "vysper.hash";
	
	public static boolean isVysperEnabled(long playerId) {
		return !modProps.getPlayerProperties(PROPERTY_VYSPER_ENABLED, playerId).isEmpty();
	}
	
	public static boolean isVysperPasswordSet(long playerId) {
		return !modProps.getPlayerProperties(PROPERTY_VYSPER_HASH, playerId).isEmpty();
	}
	
	public static boolean isVysperPasswordValid(long playerId, String password) {
		final Optional<String> hash = modProps.getPlayerProperties(PROPERTY_VYSPER_HASH, playerId).stream().findFirst().map(Property::getStrValue);
		try {
			if (hash.isPresent() && PasswordHash.validatePassword(password, hash.get())) {
				return true;
			}
			return false;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
	}

	public static void setVysperEnabled(long playerId, boolean value) {
		// Delete the old setting
		modProps.deletePlayerProperties(PROPERTY_VYSPER_ENABLED, playerId);
		if (value) {
			modProps.setPlayerProperty(PROPERTY_VYSPER_ENABLED, playerId, Boolean.toString(value));
		}
	}

	public static void setPlayerPasswordHash(long playerId, String newPassword) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String hash = PasswordHash.createHash(newPassword);
		modProps.deletePlayerProperties(PROPERTY_VYSPER_HASH, playerId);
		modProps.setPlayerProperty(PROPERTY_VYSPER_HASH, playerId, hash);
	}


}
