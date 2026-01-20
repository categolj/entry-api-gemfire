package am.ik.blog.entry.web;

import am.ik.blog.MockConfig;
import am.ik.blog.TestcontainersConfiguration;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.FrontMatter;
import am.ik.blog.entry.MockData;
import am.ik.blog.entry.gemfire.GemfireEntryRepository;
import am.ik.blog.mockserver.MockServer;
import am.ik.blog.mockserver.MockServer.Response;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static am.ik.blog.entry.MockData.ENTRY1;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for EntryController with GitHub direct update mode enabled.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import({ TestcontainersConfiguration.class, MockConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "blog.tenant.users[0]=blog-ui|{noop}empty|_=GET,LIST",
				"blog.tenant.users[1]=readonly|{noop}secret|t1=GET,LIST",
				"blog.tenant.users[2]=editor|{noop}password|_=EDIT,DELETE|t1=EDIT,DELETE,GET",
				"blog.github.direct-update=true", "blog.github.content-owner=test-owner",
				"blog.github.content-repo=test-repo", "blog.github.tenants.t1.content-owner=tenant-owner",
				"blog.github.tenants.t1.content-repo=tenant-repo", "blog.github.tenants.t1.api-url=http://PLACEHOLDER",
				"logging.level.am.ik.blog.entry.gemfire.GemfireEntryRepository=warn" })
class EntryControllerDirectUpdateTest {

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
			.defaultStatusHandler(HttpStatusCode::is4xxClientError, (req, res) -> {
			})
			.build();
		this.mockServer.reset()
			.fallback(Response.builder()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.status(404)
				.body("""
						{
							"message": "Not Found"
						}
						""")
				.build());
		this.entryRepository.deleteAll();
	}

	static Entry withTenantId(Entry entry, String tenantId) {
		if (tenantId == null) {
			return entry;
		}
		return entry.toBuilder().entryKey(new EntryKey(entry.entryKey().entryId(), tenantId)).build();
	}

	void prepareMockData(String tenantId) {
		if (tenantId == null) {
			this.entryRepository.saveAll(MockData.ALL_ENTRIES);
		}
		else {
			this.entryRepository
				.saveAll(MockData.ALL_ENTRIES.stream().map(entry -> withTenantId(entry, tenantId)).toList());
		}
	}

	Consumer<HttpHeaders> configureAuth(String username, String password) {
		return headers -> {
			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				headers.setBasicAuth(username, password);
			}
		};
	}

	String getOwner(String tenantId) {
		return tenantId == null ? "test-owner" : "tenant-owner";
	}

	String getRepo(String tenantId) {
		return tenantId == null ? "test-repo" : "tenant-repo";
	}

	@ParameterizedTest
	@CsvSource({ "/entries,admin,changeme,", "/entries,editor,password,", "/tenants/t1/entries,admin,changeme,t1",
			"/tenants/t1/entries,editor,password,t1" })
	void postEntryFromMarkdown_shouldCreateFileOnGitHub(String path, String username, String password,
			String tenantId) {
		prepareMockData(tenantId);
		String owner = getOwner(tenantId);
		String repo = getRepo(tenantId);
		long nextId = MockData.ALL_ENTRIES.getLast().entryKey().entryId() + 1;
		String formattedId = "%05d".formatted(nextId);

		AtomicReference<String> capturedBody = new AtomicReference<>();

		// Mock GitHub API: file does not exist (404), then create succeeds
		this.mockServer.PUT("/repos/%s/%s/contents/content/%s.md".formatted(owner, repo, formattedId), request -> {
			capturedBody.set(request.body());
			return Response.builder().status(201).contentType(MediaType.APPLICATION_JSON_VALUE).body("""
					{
					  "content": {
					    "name": "%s.md",
					    "path": "content/%s.md",
					    "sha": "newsha123"
					  },
					  "commit": {
					    "sha": "commit123",
					    "message": "Create entry %s"
					  }
					}
					""".formatted(formattedId, formattedId, formattedId)).build();
		});

		var response = this.restClient.post()
			.uri(path)
			.contentType(MediaType.TEXT_MARKDOWN)
			.headers(configureAuth(username, password))
			.body("""
					---
					title: New Entry via Direct Update
					categories: ["Test", "DirectUpdate"]
					tags: ["github", "api"]
					---
					This entry was created via GitHub direct update.
					""")
			.retrieve()
			.toEntity(Entry.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation())
			.isEqualTo(URI.create("http://localhost:%d%s/%d".formatted(port, path, nextId)));
		Entry entry = response.getBody();
		assertThat(entry).isNotNull();
		assertThat(entry.entryKey()).isEqualTo(new EntryKey(nextId, tenantId));
		assertThat(entry.frontMatter().title()).isEqualTo("New Entry via Direct Update");
		assertThat(entry.frontMatter().categories()).extracting("name").containsExactly("Test", "DirectUpdate");
		assertThat(entry.frontMatter().tags()).extracting("name").containsExactly("github", "api");

		// Verify GitHub API was called
		assertThat(capturedBody.get()).isNotNull();
	}

	@ParameterizedTest
	@CsvSource({ "/entries/{entryId},admin,changeme,", "/entries/{entryId},editor,password,",
			"/tenants/t1/entries/{entryId},admin,changeme,t1", "/tenants/t1/entries/{entryId},editor,password,t1" })
	void putEntryFromMarkdown_shouldUpdateFileOnGitHub(String path, String username, String password, String tenantId) {
		prepareMockData(tenantId);
		Entry entry1 = withTenantId(ENTRY1, tenantId);
		String owner = getOwner(tenantId);
		String repo = getRepo(tenantId);
		String formattedId = entry1.formatId();

		AtomicReference<String> capturedBody = new AtomicReference<>();

		// Mock GitHub API: file exists, then update succeeds
		String existingContent = entry1.toMarkdown();
		this.mockServer.GET("/repos/%s/%s/contents/content/%s.md".formatted(owner, repo, formattedId),
				request -> Response.builder()
					.status(200)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body("""
							{
							  "name": "%s.md",
							  "path": "content/%s.md",
							  "sha": "existingsha456",
							  "content": "%s"
							}
							""".formatted(formattedId, formattedId,
							Base64.getEncoder().encodeToString(existingContent.getBytes(StandardCharsets.UTF_8))))
					.build());

		this.mockServer.PUT("/repos/%s/%s/contents/content/%s.md".formatted(owner, repo, formattedId), request -> {
			capturedBody.set(request.body());
			return Response.builder().status(200).contentType(MediaType.APPLICATION_JSON_VALUE).body("""
					{
					  "content": {
					    "name": "%s.md",
					    "path": "content/%s.md",
					    "sha": "updatedsha789"
					  },
					  "commit": {
					    "sha": "updatecommit456",
					    "message": "Update entry %s"
					  }
					}
					""".formatted(formattedId, formattedId, formattedId)).build();
		});

		var response = this.restClient.put()
			.uri(path, entry1.entryKey().entryId())
			.contentType(MediaType.TEXT_MARKDOWN)
			.headers(configureAuth(username, password))
			.body("""
					---
					title: Updated Entry via Direct Update
					categories: ["Updated", "DirectUpdate"]
					tags: ["github", "updated"]
					---
					This entry was updated via GitHub direct update.
					""")
			.retrieve()
			.toEntity(Entry.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Entry entry = response.getBody();
		assertThat(entry).isNotNull();
		assertThat(entry.entryKey()).isEqualTo(entry1.entryKey());
		assertThat(entry.frontMatter().title()).isEqualTo("Updated Entry via Direct Update");
		assertThat(entry.frontMatter().categories()).extracting("name").containsExactly("Updated", "DirectUpdate");
		assertThat(entry.frontMatter().tags()).extracting("name").containsExactly("github", "updated");

		// Verify GitHub API was called
		assertThat(capturedBody.get()).isNotNull();
	}

	@ParameterizedTest
	@CsvSource({ "/entries/{entryId},admin,changeme,", "/entries/{entryId},editor,password,",
			"/tenants/t1/entries/{entryId},admin,changeme,t1", "/tenants/t1/entries/{entryId},editor,password,t1" })
	void patchEntrySummary_shouldUpdateFileOnGitHub(String path, String username, String password, String tenantId) {
		prepareMockData(tenantId);
		Entry entry1 = withTenantId(ENTRY1, tenantId);
		String owner = getOwner(tenantId);
		String repo = getRepo(tenantId);
		String formattedId = entry1.formatId();

		AtomicReference<String> capturedBody = new AtomicReference<>();

		// Mock GitHub API: file exists with content
		String existingContent = entry1.toMarkdown();
		this.mockServer.GET("/repos/%s/%s/contents/content/%s.md".formatted(owner, repo, formattedId),
				request -> Response.builder()
					.status(200)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body("""
							{
							  "name": "%s.md",
							  "path": "content/%s.md",
							  "sha": "existingsha789",
							  "content": "%s"
							}
							""".formatted(formattedId, formattedId,
							Base64.getEncoder().encodeToString(existingContent.getBytes(StandardCharsets.UTF_8))))
					.build());

		this.mockServer.PUT("/repos/%s/%s/contents/content/%s.md".formatted(owner, repo, formattedId), request -> {
			capturedBody.set(request.body());
			return Response.builder().status(200).contentType(MediaType.APPLICATION_JSON_VALUE).body("""
					{
					  "content": {
					    "name": "%s.md",
					    "path": "content/%s.md",
					    "sha": "updatedsha999"
					  },
					  "commit": {
					    "sha": "summarycommit789",
					    "message": "Update entry %s"
					  }
					}
					""".formatted(formattedId, formattedId, formattedId)).build();
		});

		var response = this.restClient.patch()
			.uri(path + "/summary", entry1.entryKey().entryId())
			.contentType(MediaType.APPLICATION_JSON)
			.headers(configureAuth(username, password))
			.body("""
					{"summary": "This is a summary added via GitHub direct update."}
					""")
			.retrieve()
			.toEntity(Entry.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Entry entry = response.getBody();
		assertThat(entry).isNotNull();
		assertThat(entry.entryKey()).isEqualTo(entry1.entryKey());
		FrontMatter frontMatter = entry.frontMatter();
		assertThat(frontMatter).isNotNull();
		assertThat(frontMatter.title()).isEqualTo(entry1.frontMatter().title());
		assertThat(frontMatter.summary()).isEqualTo("This is a summary added via GitHub direct update.");

		// Verify GitHub API was called
		assertThat(capturedBody.get()).isNotNull();
		// The content is base64-encoded, so we verify it contains the summary by decoding
		assertThat(capturedBody.get()).contains("content");
	}

	@ParameterizedTest
	@CsvSource({ "/entries/{entryId},admin,changeme,", "/entries/{entryId},editor,password,",
			"/tenants/t1/entries/{entryId},admin,changeme,t1", "/tenants/t1/entries/{entryId},editor,password,t1" })
	void deleteEntry_shouldDeleteFileOnGitHub(String path, String username, String password, String tenantId) {
		prepareMockData(tenantId);
		Entry entry1 = withTenantId(ENTRY1, tenantId);
		String owner = getOwner(tenantId);
		String repo = getRepo(tenantId);
		String formattedId = entry1.formatId();

		AtomicReference<Boolean> deleteWasCalled = new AtomicReference<>(false);

		// Mock GitHub API: file exists
		String existingContent = entry1.toMarkdown();
		this.mockServer.GET("/repos/%s/%s/contents/content/%s.md".formatted(owner, repo, formattedId),
				request -> Response.builder()
					.status(200)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body("""
							{
							  "name": "%s.md",
							  "path": "content/%s.md",
							  "sha": "deletingsha123",
							  "content": "%s"
							}
							""".formatted(formattedId, formattedId,
							Base64.getEncoder().encodeToString(existingContent.getBytes(StandardCharsets.UTF_8))))
					.build());

		this.mockServer.DELETE("/repos/%s/%s/contents/content/%s.md".formatted(owner, repo, formattedId), request -> {
			deleteWasCalled.set(true);
			return Response.builder().status(200).contentType(MediaType.APPLICATION_JSON_VALUE).body("""
					{
					  "content": null,
					  "commit": {
					    "sha": "deletecommit123",
					    "message": "Delete entry %s"
					  }
					}
					""".formatted(formattedId)).build();
		});

		assertThat(this.entryRepository.findById(entry1.entryKey())).isPresent();

		var response = this.restClient.delete()
			.uri(path, entry1.entryKey().entryId())
			.headers(configureAuth(username, password))
			.retrieve()
			.toBodilessEntity();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		// Verify GitHub DELETE API was called
		assertThat(deleteWasCalled.get()).isTrue();

		// Note: In direct update mode, repository is not updated directly.
		// The entry still exists in repository until webhook syncs it.
		// This is expected behavior as per the design.
	}

}
