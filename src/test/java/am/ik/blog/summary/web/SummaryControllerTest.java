package am.ik.blog.summary.web;

import am.ik.blog.MockConfig;
import am.ik.blog.TestcontainersConfiguration;
import am.ik.blog.mockserver.MockServer;
import am.ik.blog.mockserver.MockServer.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Import({ TestcontainersConfiguration.class, MockConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "blog.tenant.users[0]=blog-ui|{noop}empty|_=GET,LIST",
				"blog.tenant.users[1]=readonly|{noop}secret|_=GET,LIST",
				"blog.tenant.users[2]=editor|{noop}password|_=EDIT" })
class SummaryControllerTest {

	RestTestClient client;

	@Autowired
	MockServer mockServer;

	@LocalServerPort
	int port;

	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
		this.mockServer.reset();
	}

	void setupOpenAiMock(String summaryText) {
		String sseResponse = """
				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"%s"},"finish_reason":null}]}

				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

				data: [DONE]

				"""
			.formatted(summaryText);

		this.mockServer.POST("/v1/chat/completions",
				request -> Response.builder().status(200).contentType("text/event-stream").body(sseResponse).build());
	}

	@Test
	void summarize_success() {
		String summaryText = "This article explains how to get started with Spring Boot.";
		setupOpenAiMock(summaryText);

		this.client.post()
			.uri("/tenants/{tenantId}/summary", "_")
			.headers(headers -> headers.setBasicAuth("admin", "changeme"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "Sample blog content about Spring Boot"}
					""")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.summary")
			.isEqualTo(summaryText);
	}

	@Test
	void summarize_successWithEditor() {
		String summaryText = "This article explains how to get started with Spring Boot.";
		setupOpenAiMock(summaryText);

		this.client.post()
			.uri("/tenants/{tenantId}/summary", "_")
			.headers(headers -> headers.setBasicAuth("editor", "password"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "Sample blog content about Spring Boot"}
					""")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.summary")
			.isEqualTo(summaryText);
	}

	@Test
	void summarize_emptyContent_returnsBadRequest() {
		this.client.post()
			.uri("/tenants/{tenantId}/summary", "_")
			.headers(headers -> headers.setBasicAuth("admin", "changeme"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": ""}
					""")
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody()
			.jsonPath("$.detail")
			.isEqualTo("Content must not be empty");
	}

	@Test
	void summarize_blankContent_returnsBadRequest() {
		this.client.post()
			.uri("/tenants/{tenantId}/summary", "_")
			.headers(headers -> headers.setBasicAuth("admin", "changeme"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "   "}
					""")
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody()
			.jsonPath("$.detail")
			.isEqualTo("Content must not be empty");
	}

	@Test
	void summarize_nullContent_returnsBadRequest() {
		this.client.post()
			.uri("/tenants/{tenantId}/summary", "_")
			.headers(headers -> headers.setBasicAuth("admin", "changeme"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": null}
					""")
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody()
			.jsonPath("$.detail")
			.isEqualTo("Content must not be empty");
	}

	@Test
	void summarize_unauthorized() {
		this.client.post().uri("/tenants/{tenantId}/summary", "_").contentType(MediaType.APPLICATION_JSON).body("""
				{"content": "Sample content"}
				""").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void summarize_forbidden() {
		this.client.post()
			.uri("/tenants/{tenantId}/summary", "_")
			.headers(headers -> headers.setBasicAuth("readonly", "secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "Sample content"}
					""")
			.exchange()
			.expectStatus()
			.isForbidden();
	}

	@Test
	void summarize_forbiddenForOtherTenant() {
		this.client.post()
			.uri("/tenants/{tenantId}/summary", "en")
			.headers(headers -> headers.setBasicAuth("editor", "password"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "Sample content"}
					""")
			.exchange()
			.expectStatus()
			.isForbidden();
	}

}
