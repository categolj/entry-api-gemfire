package am.ik.blog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;

/**
 * Testcontainers configuration for S3 testing using MinIO.
 */
@TestConfiguration(proxyBeanMethods = false)
public class S3TestcontainersConfiguration {

	public static final String BUCKET_NAME = "test-bucket";

	private static final String MINIO_ACCESS_KEY = "minioadmin";

	private static final String MINIO_SECRET_KEY = "minioadmin";

	private static final int MINIO_PORT = 9000;

	@Bean
	GenericContainer<?> minioContainer() {
		GenericContainer<?> container = new GenericContainer<>("minio/minio:latest").withExposedPorts(MINIO_PORT)
			.withEnv("MINIO_ACCESS_KEY", MINIO_ACCESS_KEY)
			.withEnv("MINIO_SECRET_KEY", MINIO_SECRET_KEY)
			.withCommand("server", "/data")
			.waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready")
				.forPort(MINIO_PORT)
				.withStartupTimeout(Duration.ofSeconds(60)));
		container.start();
		return container;
	}

	@Bean
	DynamicPropertyRegistrar s3DynamicPropertyRegistrar(GenericContainer<?> minioContainer) {
		return registry -> {
			String endpoint = "http://%s:%d".formatted(minioContainer.getHost(),
					minioContainer.getMappedPort(MINIO_PORT));
			registry.add("spring.cloud.aws.credentials.access-key", () -> MINIO_ACCESS_KEY);
			registry.add("spring.cloud.aws.credentials.secret-key", () -> MINIO_SECRET_KEY);
			registry.add("spring.cloud.aws.s3.endpoint", () -> endpoint);
			registry.add("spring.cloud.aws.s3.path-style-access-enabled", () -> "true");
			registry.add("spring.cloud.aws.region.static", () -> "us-east-1");
			registry.add("blog.s3.backet-name", () -> BUCKET_NAME);
			registry.add("blog.s3.create-bucket", () -> "true");
		};
	}

}
