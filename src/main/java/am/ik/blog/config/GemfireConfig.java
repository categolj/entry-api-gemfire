package am.ik.blog.config;

import am.ik.blog.GemfireProps;
import am.ik.blog.entry.gemfire.EntryEntity;
import java.util.Properties;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.proxy.ProxySocketFactories;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GemfireConfig {

	@Bean
	ClientCache clientCache(GemfireProps props) {
		try {
			// Try to get an existing cache if one exists
			return ClientCacheFactory.getAnyInstance();
		}
		catch (CacheClosedException e) {
			// No existing cache, create a new one
		}
		Properties properties = new Properties();
		if (props.properties() != null) {
			properties.putAll(props.properties());
		}
		ClientCacheFactory cacheFactory = new ClientCacheFactory(properties)
			.setPdxSerializer(new ReflectionBasedAutoSerializer(true, EntryEntity.class.getName()))
		// .setPoolSubscriptionEnabled(true)
		;
		for (var locator : props.locators()) {
			cacheFactory.addPoolLocator(locator.host(), locator.port());
		}
		GemfireProps.Endpoint sniProxy = props.sniProxy();
		if (sniProxy != null) {
			cacheFactory.setPoolSocketFactory(ProxySocketFactories.sni(sniProxy.host(), sniProxy.port()));
		}
		return cacheFactory.create();
	}

	@Bean
	Region<String, EntryEntity> entryRegion(ClientCache clientCache) {
		Region<String, EntryEntity> existing = clientCache.getRegion("Entry");
		if (existing != null) {
			return existing;
		}
		Region<String, EntryEntity> region = clientCache
			.<String, EntryEntity>createClientRegionFactory(ClientRegionShortcut.PROXY)
			.create("Entry");
		// region.registerInterestForAllKeys(InterestResultPolicy.KEYS_VALUES);
		return region;
	}

}