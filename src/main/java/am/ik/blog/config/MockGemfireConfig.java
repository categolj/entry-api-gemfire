package am.ik.blog.config;

import am.ik.blog.entry.gemfire.EntryEntity;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Proxy;

@Configuration
@Profile("mock")
public class MockGemfireConfig {

	@Bean
	ClientCache clientCache() {
		return (ClientCache) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { ClientCache.class },
				(proxy, method, args) -> null);
	}

	@SuppressWarnings("unchecked")
	@Bean
	Region<String, EntryEntity> entryRegion() {
		return (Region<String, EntryEntity>) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { Region.class }, (proxy, method, args) -> null);
	};

}
