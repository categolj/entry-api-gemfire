package am.ik.blog;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	GemFireCluster cluster() {
		GemFireCluster cluster = new GemFireCluster("gemfire/gemfire:10.3.0-jdk25", 1, 1);
		cluster.acceptLicense();
		cluster.withPdx("am\\.ik\\.blog\\.entry\\..+", true);
		cluster.start();
		cluster.gfsh(false, "create region --name=Entry --type=PARTITION_REDUNDANT_PERSISTENT");
		cluster.gfsh(false,
				"create index --name=idx_tenant_updated_at --expression=\"tenantId, updatedAt\" --region=/Entry");
		return cluster;
	}

	@Bean
	DynamicPropertyRegistrar dynamicPropertyRegistrar(GemFireCluster cluster) {
		return registry -> {
			registry.add("gemfire.locators", () -> "127.0.0.1:%d".formatted(cluster.getLocatorPort()));
			// The test cluster does not use TLS, so disable TLS for every component (an
			// empty value means none). This keeps a local settings file such as
			// application-default.properties, which points at a TLS-enabled remote
			// cluster, from breaking the tests.
			registry.add("gemfire.properties.ssl-enabled-components", () -> "");
		};
	}

}
