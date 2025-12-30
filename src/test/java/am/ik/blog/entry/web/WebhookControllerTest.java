package am.ik.blog.entry.web;

import am.ik.blog.MockConfig;
import am.ik.blog.TestcontainersConfiguration;
import am.ik.blog.entry.Author;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.FrontMatter;
import am.ik.blog.entry.MockData;
import am.ik.blog.entry.Tag;
import am.ik.blog.entry.gemfire.GemfireEntryRepository;
import am.ik.blog.mockserver.MockServer;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static am.ik.webhook.WebhookHttpHeaders.X_HUB_SIGNATURE_256;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Import({ TestcontainersConfiguration.class, MockConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "blog.tenant.users[0]=blog-ui|{noop}empty|_=GET,LIST",
				"blog.tenant.users[1]=readonly|{noop}secret|t1=GET,LIST",
				"blog.tenant.users[2]=editor|{noop}password|_=EDIT,DELETE|t1=EDIT,DELETE,GET",
				"blog.github.content-owner=public", "blog.github.content-repo=blog",
				"blog.github.access-token=important", "blog.github.tenants.t1.content-owner=private",
				"blog.github.tenants.t1.content-repo=blog", "blog.github.tenants.t1.access-token=secret" })
class WebhookControllerTest {

	RestClient restClient;

	@Autowired
	GemfireEntryRepository entryRepository;

	@Autowired
	MockServer mockServer;

	@LocalServerPort
	int port;

	@BeforeEach
	void setup(@Autowired RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("http://localhost:" + port)
			.defaultStatusHandler(statusCode -> statusCode == HttpStatus.BAD_REQUEST, (req, res) -> {
			})
			.build();
		this.mockServer.reset()
			.fallback(MockServer.Response.builder()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.status(404)
				.body("""
						{
							"message": "Entry not found"
						}
						""")
				.build());
	}

	@ParameterizedTest
	@CsvSource({ "/webhook,public/blog,db30487ad2364ad60b2fdb9046f869049bdac9b190295eaf671a4aef38408998",
			"/tenants/t1/webhook,private/blog,e1530f44f750268650cf306781e34839dbb516e8d4a33b5f4088363cb0e378e7" })
	void webhookAdded(String path, String repo, String signature) {
		String tenantId = path.startsWith("/tenants/") ? path.split("/")[2] : null;
		Entry entry = Entry.builder()
			.entryKey(new EntryKey(100L, tenantId))
			.content("""
					# Cache Aside Entry
					This is a cache aside entry.
					""".trim())
			.frontMatter(FrontMatter.builder()
				.title("Cache Aside Entry")
				.categories(List.of(new Category("Programming"), new Category("Java")))
				.tags(List.of(new Tag("cache"), new Tag("aside")))
				.build())
			.created(Author.builder().name("demo").build())
			.updated(Author.builder().name("demo").build())
			.build();
		this.mockServer
			.GET("/repos/%s/contents/content/00100.md".formatted(repo),
					req -> MockServer.Response.builder()
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.status(200)
						.body("""
								{"content":"%s", "url": "http://127.0.0.1:%d/repos/%s/contents/content/00100.md"}
								""".formatted(Base64.getEncoder().encodeToString(entry.toMarkdown().getBytes(UTF_8)),
								port, repo))
						.build())
			.route(req -> ("/repos/%s/commits".formatted(repo)).equals(req.path())
					&& "content/00100.md".equals(req.queryParam("path")),
					req -> MockServer.Response.builder()
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.status(200)
						.body("""
								[{"commit":{"author":{"name":"Test User2","date":"2025-06-27T15:55:20Z"}}},{"commit":{"author":{"name":"Test User1","date":"2025-06-27T15:45:58Z"}}}]
								""")
						.build());
		var response = this.restClient.post()
			.uri(path)
			.header(X_HUB_SIGNATURE_256, "sha256=" + signature)
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{
					  "repository": {"full_name": "%s"},
					  "commits": [
					    {"added": ["content/00100.md"], "modified": [],"removed": []}
					  ]
					}
					""".formatted(repo))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<List<Map<String, Object>>>() {
			});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(List
			.of(Map.of("added", Map.of("entryId", 100, "tenantId", EntryKey.requireNonNullTenantId(tenantId)))));
		assertThat(this.entryRepository.findById(new EntryKey(100L, tenantId))).contains(entry.toBuilder()
			.updated(Author.builder().name("Test User2").date(Instant.parse("2025-06-27T15:55:20Z")).build())
			.created(Author.builder().name("Test User1").date(Instant.parse("2025-06-27T15:45:58Z")).build())
			.build());
	}

	@ParameterizedTest
	@CsvSource({ "/webhook,public/blog,c7ea28fd87477e248dab1ed0b07be97a5dc288b01c16a1fb2fc282f4f5a9823f",
			"/tenants/t1/webhook,private/blog,0d3e938aeee5f5089a68da65cd97a70eb18f870f061ac2bcca2669274f04820f" })
	void webhookModified(String path, String repo, String signature) {
		String tenantId = path.startsWith("/tenants/") ? path.split("/")[2] : null;
		Entry entry = Entry.builder()
			.entryKey(new EntryKey(100L, tenantId))
			.content("""
					# Cache Aside Entry
					This is a cache aside entry.
					""".trim())
			.frontMatter(FrontMatter.builder()
				.title("Cache Aside Entry")
				.categories(List.of(new Category("Programming"), new Category("Java")))
				.tags(List.of(new Tag("cache"), new Tag("aside")))
				.build())
			.created(Author.builder().name("demo").build())
			.updated(Author.builder().name("demo").build())
			.build();
		this.mockServer
			.GET("/repos/%s/contents/content/00100.md".formatted(repo),
					req -> MockServer.Response.builder()
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.status(200)
						.body("""
								{"content":"%s", "url": "http://127.0.0.1:%d/repos/%s/contents/content/00100.md"}
								""".formatted(Base64.getEncoder().encodeToString(entry.toMarkdown().getBytes(UTF_8)),
								port, repo))
						.build())
			.route(req -> ("/repos/%s/commits".formatted(repo)).equals(req.path())
					&& "content/00100.md".equals(req.queryParam("path")),
					req -> MockServer.Response.builder()
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.status(200)
						.body("""
								[{"commit":{"author":{"name":"Test User2","date":"2025-06-27T15:55:20Z"}}},{"commit":{"author":{"name":"Test User1","date":"2025-06-27T15:45:58Z"}}}]
								""")
						.build());
		var response = this.restClient.post()
			.uri(path)
			.header(X_HUB_SIGNATURE_256, "sha256=" + signature)
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{
					  "repository": {"full_name": "%s"},
					  "commits": [
					    {"added": [], "modified": ["content/00100.md"],"removed": []}
					  ]
					}
					""".formatted(repo))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<List<Map<String, Object>>>() {
			});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(List
			.of(Map.of("modified", Map.of("entryId", 100, "tenantId", EntryKey.requireNonNullTenantId(tenantId)))));
		assertThat(this.entryRepository.findById(new EntryKey(100L, tenantId))).contains(entry.toBuilder()
			.updated(Author.builder().name("Test User2").date(Instant.parse("2025-06-27T15:55:20Z")).build())
			.created(Author.builder().name("Test User1").date(Instant.parse("2025-06-27T15:45:58Z")).build())
			.build());
	}

	@ParameterizedTest
	@CsvSource({ "/webhook,public/blog,5e81a7d2a15b751aca058964c07b064a35c110fa3eaf5d2e11d01de36411e15b",
			"/tenants/t1/webhook,private/blog,0d68dc73f3ab9603e7863527fe3f105f23b01e830eec8052cfeff9a993390f4e" })
	void webhookRemoved(String path, String repo, String signature) {
		String tenantId = path.startsWith("/tenants/") ? path.split("/")[2] : null;
		Entry entry = MockData.ENTRY1;
		this.entryRepository.save(EntryControllerTest.withTenantId(entry, tenantId));
		assertThat(this.entryRepository.findById(new EntryKey(1L, tenantId))).isPresent();
		this.mockServer
			.GET("/repos/%s/contents/content/00001.md".formatted(repo),
					req -> MockServer.Response.builder()
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.status(200)
						.body("""
								{"content":"%s", "url": "http://127.0.0.1:%d/repos/%s/contents/content/00001.md"}
								""".formatted(Base64.getEncoder().encodeToString(entry.toMarkdown().getBytes(UTF_8)),
								port, repo))
						.build())
			.route(req -> ("/repos/%s/commits".formatted(repo)).equals(req.path())
					&& "content/00001.md".equals(req.queryParam("path")),
					req -> MockServer.Response.builder()
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.status(200)
						.body("""
								[{"commit":{"author":{"name":"Test User2","date":"2025-06-27T15:55:20Z"}}},{"commit":{"author":{"name":"Test User1","date":"2025-06-27T15:45:58Z"}}}]
								""")
						.build());
		var response = this.restClient.post()
			.uri(path)
			.header(X_HUB_SIGNATURE_256, "sha256=" + signature)
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{
					  "repository": {"full_name": "%s"},
					  "commits": [
					    {"added": [], "modified": [],"removed": ["content/00001.md"]}
					  ]
					}
					""".formatted(repo))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<List<Map<String, Object>>>() {
			});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(List
			.of(Map.of("removed", Map.of("entryId", 1, "tenantId", EntryKey.requireNonNullTenantId(tenantId)))));
		assertThat(this.entryRepository.exists(new EntryKey(1L, tenantId))).isFalse();
	}

	@ParameterizedTest
	@CsvSource({ "/webhook,public/blog", "/tenants/t1/webhook,private/blog" })
	void invalidSignature(String path) {
		var response = this.restClient.post()
			.uri(path)
			.header(X_HUB_SIGNATURE_256, "sha256=deadbeef")
			.contentType(MediaType.APPLICATION_JSON)
			.body("{}")
			.retrieve()
			.toEntity(ProblemDetail.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getStatus()).isEqualTo(400);
		assertThat(response.getBody().getDetail()).isEqualTo("Invalid signature: sha256=deadbeef");
	}

}
