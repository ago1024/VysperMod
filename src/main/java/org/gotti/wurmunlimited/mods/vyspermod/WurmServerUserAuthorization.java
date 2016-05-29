package org.gotti.wurmunlimited.mods.vyspermod;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.authorization.UserAuthorization;

public class WurmServerUserAuthorization implements UserAuthorization {

	/*
	@Override
	public void addUser(Entity username, String password) throws AccountCreationException {
		throw new AccountCreationException("Account creation is not possible");
	}

	@Override
	public void changePassword(Entity username, String password) throws AccountCreationException {
		throw new AccountCreationException("Password changes are not possible");
	}

	@Override
	public boolean verifyAccountExists(Entity jid) {
		return Players.getInstance().doesPlayerNameExist(jid.getCanonicalizedName());
	}
	*/

	@Override
	public boolean verifyCredentials(Entity jid, String passwordCleartext, Object credentials) {
		return true; // FIXME
	}

	@Override
	public boolean verifyCredentials(String username, String passwordCleartext, Object credentials) {
		return true; // FIXME
	}

}
