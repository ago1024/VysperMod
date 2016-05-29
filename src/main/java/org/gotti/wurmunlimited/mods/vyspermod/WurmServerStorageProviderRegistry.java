package org.gotti.wurmunlimited.mods.vyspermod;

import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;

public class WurmServerStorageProviderRegistry extends OpenStorageProviderRegistry {

	public WurmServerStorageProviderRegistry() {
		add(new WurmServerUserAuthorization());
		add(new MemoryRosterManager());
		
		// provider from external modules, low coupling, fail when modules are not present
		add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider");
		add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeInMemoryStorageProvider");
	}
}
