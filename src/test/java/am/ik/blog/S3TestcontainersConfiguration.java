package am.ik.blog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;

/**
 * Testcontainers configuration for S3 testing using RustFS (MinIO compatible).
 */
@TestConfiguration(proxyBeanMethods = false)
public class S3TestcontainersConfiguration {

	public static final String BUCKET_NAME = "test-bucket";

	private static final String S3_ACCESS_KEY = "rustfsadmin";

	private static final String S3_SECRET_KEY = "rustfsadmin";

	private static final int S3_PORT = 9000;

	@Bean
	GenericContainer<?> s3Container() {
		GenericContainer<?> container = new GenericContainer<>("rustfs/rustfs:1.0.3").withExposedPorts(S3_PORT)
			.withEnv("RUSTFS_ACCESS_KEY", S3_ACCESS_KEY)
			.withEnv("RUSTFS_SECRET_KEY", S3_SECRET_KEY)
			// Allow browser uploads via presigned URLs from the console
			.withEnv("RUSTFS_CORS_ALLOWED_ORIGINS", "*")
			.waitingFor(new HttpWaitStrategy().forPath("/health")
				.forPort(S3_PORT)
				.withStartupTimeout(Duration.ofSeconds(60)));
		container.start();
		return container;
	}

	@Bean
	DynamicPropertyRegistrar s3DynamicPropertyRegistrar(GenericContainer<?> s3Container) {
		return registry -> {
			String endpoint = "http://%s:%d".formatted(s3Container.getHost(), s3Container.getMappedPort(S3_PORT));
			registry.add("spring.cloud.aws.credentials.access-key", () -> S3_ACCESS_KEY);
			registry.add("spring.cloud.aws.credentials.secret-key", () -> S3_SECRET_KEY);
			registry.add("spring.cloud.aws.s3.endpoint", () -> endpoint);
			registry.add("spring.cloud.aws.s3.path-style-access-enabled", () -> "true");
			registry.add("spring.cloud.aws.region.static", () -> "us-east-1");
			registry.add("blog.s3.backet-name", () -> BUCKET_NAME);
			registry.add("blog.s3.create-bucket", () -> "true");
		};
	}

}
