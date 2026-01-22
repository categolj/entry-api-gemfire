package am.ik.blog.edit.web;

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
class EditControllerTest {

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

	void setupOpenAiMock(String editedText) {
		String sseResponse = """
				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"%s"},"finish_reason":null}]}

				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

				data: [DONE]

				"""
			.formatted(editedText);

		this.mockServer.POST("/v1/chat/completions",
				request -> Response.builder().status(200).contentType("text/event-stream").body(sseResponse).build());
	}

	@Test
	void edit_success() {
		String editedText = "This is the proofread content.";
		setupOpenAiMock(editedText);

		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
			.headers(headers -> headers.setBasicAuth("admin", "changeme"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "This is the original content"}
					""")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.content")
			.isEqualTo(editedText);
	}

	@Test
	void edit_successWithCompletionMode() {
		String editedText = "This is the completed content with natural additions.";
		setupOpenAiMock(editedText);

		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
			.headers(headers -> headers.setBasicAuth("admin", "changeme"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "This is the original content", "mode": "COMPLETION"}
					""")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.content")
			.isEqualTo(editedText);
	}

	@Test
	void edit_successWithExpansionMode() {
		String editedText = "This is the expanded content with follow-up.";
		setupOpenAiMock(editedText);

		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
			.headers(headers -> headers.setBasicAuth("admin", "changeme"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "This is the original content", "mode": "EXPANSION"}
					""")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.content")
			.isEqualTo(editedText);
	}

	@Test
	void edit_successWithEditor() {
		String editedText = "This is the proofread content.";
		setupOpenAiMock(editedText);

		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
			.headers(headers -> headers.setBasicAuth("editor", "password"))
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"content": "Sample blog content"}
					""")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.content")
			.isEqualTo(editedText);
	}

	@Test
	void edit_emptyContent_returnsBadRequest() {
		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
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
	void edit_blankContent_returnsBadRequest() {
		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
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
	void edit_nullContent_returnsBadRequest() {
		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
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
	void edit_unauthorized() {
		this.client.post().uri("/tenants/{tenantId}/edit", "_").contentType(MediaType.APPLICATION_JSON).body("""
				{"content": "Sample content"}
				""").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void edit_forbidden() {
		this.client.post()
			.uri("/tenants/{tenantId}/edit", "_")
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
	void edit_forbiddenForOtherTenant() {
		this.client.post()
			.uri("/tenants/{tenantId}/edit", "en")
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
