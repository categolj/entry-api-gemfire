package am.ik.blog.s3.web;

import am.ik.blog.MockConfig;
import am.ik.blog.S3TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("mock")
@Import({ S3TestcontainersConfiguration.class, MockConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "blog.tenant.users[0]=blog-ui|{noop}empty|_=GET,LIST",
				"blog.tenant.users[1]=readonly|{noop}secret|_=GET,LIST",
				"blog.tenant.users[2]=editor|{noop}password|_=EDIT" })
class S3ControllerTest {

	RestTestClient client;

	@LocalServerPort
	int port;

	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	void presign_uploadAndPublicAccess_success() throws IOException {
		String tenantId = "_";
		String testFileName = "test-image.png";
		byte[] imageContent = createTestPngImage();

		// Step 1: Get presigned URL
		byte[] responseBody = this.client.post()
			.uri("/tenants/{tenantId}/s3/presign", tenantId)
			.headers(headers -> headers.setBasicAuth("editor", "password"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"fileName": "%s"}
					""".formatted(testFileName))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.url")
			.value(url -> assertThat(url.toString()).contains(tenantId + "/" + testFileName))
			.returnResult()
			.getResponseBody();

		// Extract URL from JSON response
		String presignedUrl = extractUrlFromJson(new String(responseBody));
		assertThat(presignedUrl).isNotNull();

		// Step 2: Upload PNG image using presigned URL
		RestTestClient minioClient = RestTestClient.bindToServer().build();
		minioClient.put()
			.uri(URI.create(presignedUrl))
			.contentType(MediaType.IMAGE_PNG)
			.body(imageContent)
			.exchange()
			.expectStatus()
			.isOk();

		// Step 3: Verify public access
		String publicUrl = presignedUrl.split("\\?")[0];
		minioClient.get()
			.uri(URI.create(publicUrl))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(byte[].class)
			.value(body -> assertThat(body).isEqualTo(imageContent));
	}

	private byte[] createTestPngImage() throws IOException {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		// Fill with a simple color pattern
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				image.setRGB(x, y, (x * 25) << 16 | (y * 25) << 8 | 128);
			}
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		return baos.toByteArray();
	}

	@Test
	void presign_unauthorized() {
		this.client.post().uri("/tenants/_/s3/presign").contentType(MediaType.APPLICATION_JSON).body("""
				{"fileName": "test.png"}
				""").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void presign_forbidden() {
		this.client.post()
			.uri("/tenants/_/s3/presign")
			.headers(headers -> headers.setBasicAuth("readonly", "secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"fileName": "test.png"}
					""")
			.exchange()
			.expectStatus()
			.isForbidden();
	}

	@Test
	void presign_forbiddenForOtherTenant() {
		// editor has EDIT permission only for tenant "_", not for tenant "en"
		this.client.post()
			.uri("/tenants/{tenantId}/s3/presign", "en")
			.headers(headers -> headers.setBasicAuth("editor", "password"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"fileName": "test.png"}
					""")
			.exchange()
			.expectStatus()
			.isForbidden();
	}

	private String extractUrlFromJson(String json) {
		// Simple JSON parsing for {"url":"..."}
		int startIndex = json.indexOf("\"url\":\"") + 7;
		int endIndex = json.indexOf("\"", startIndex);
		return json.substring(startIndex, endIndex);
	}

}
