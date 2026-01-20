package am.ik.blog.entry;

import am.ik.blog.GitHubProps;
import am.ik.blog.MockConfig;
import am.ik.blog.github.GitHubClient;
import am.ik.blog.mockserver.MockServer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MockConfig.class, EntryServiceGitHubDirectUpdateTest.TestConfig.class })
class EntryServiceGitHubDirectUpdateTest {

	@Autowired
	EntryService entryService;

	@Autowired
	MockServer mockServer;

	@Autowired
	GitHubProps gitHubProps;

	@Autowired
	EntryRepository entryRepository;

	@TestConfiguration
	static class TestConfig {

		@Bean
		GitHubProps gitHubProps() {
			GitHubProps props = new GitHubProps();
			props.setDirectUpdate(true);
			props.setContentOwner("test-owner");
			props.setContentRepo("test-repo");
			return props;
		}

		@Bean
		EntryRepository entryRepository() {
			return Mockito.mock(EntryRepository.class);
		}

		@Bean
		GitHubClient gitHubClient(MockServer mockServer) {
			RestClient restClient = RestClient.builder()
				.baseUrl("http://127.0.0.1:" + mockServer.port())
				.defaultStatusHandler(status -> true, (req, res) -> {
				})
				.build();
			HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.build();
			return factory.createClient(GitHubClient.class);
		}

		@Bean
		HttpServiceProxyRegistry httpServiceProxyRegistry(GitHubClient gitHubClient) {
			HttpServiceProxyRegistry registry = Mockito.mock(HttpServiceProxyRegistry.class);
			when(registry.getClient("github", GitHubClient.class)).thenReturn(gitHubClient);
			return registry;
		}

		@Bean
		EntryParser entryParser() {
			return new EntryParser(JsonMapper.builder().build());
		}

		@Bean
		EntryService entryService(EntryRepository entryRepository, GitHubProps gitHubProps,
				HttpServiceProxyRegistry registry, EntryParser entryParser) {
			return new EntryService(entryRepository, gitHubProps, registry, entryParser);
		}

	}

	@BeforeEach
	void setUp() {
		Mockito.reset(this.entryRepository);
		this.mockServer.reset()
			.fallback(MockServer.Response.builder()
				.status(404)
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.body("{\"message\":\"Not Found\"}")
				.build());
	}

	@Test
	void save_shouldCreateFileWhenFileNotExists() {
		Entry entry = createTestEntry(1L);
		String createResponse = """
				{
				  "content": {
				    "name": "00001.md",
				    "path": "content/00001.md",
				    "sha": "newfile123",
				    "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/00001.md",
				    "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/newfile123",
				    "html_url": "https://github.com/test-owner/test-repo/blob/main/content/00001.md",
				    "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/00001.md",
				    "content": "dGVzdA==",
				    "type": "file"
				  },
				  "commit": {
				    "sha": "commit123abc",
				    "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/commit123abc",
				    "html_url": "https://github.com/test-owner/test-repo/commit/commit123abc",
				    "author": {
				      "name": "Test Author",
				      "email": "author@example.com",
				      "date": "2025-06-28T12:00:00Z"
				    },
				    "committer": {
				      "name": "Test Committer",
				      "email": "committer@example.com",
				      "date": "2025-06-28T12:00:00Z"
				    },
				    "tree": {
				      "sha": "tree123",
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree123",
				      "html_url": null
				    },
				    "message": "Create entry 00001",
				    "parents": []
				  }
				}
				""";

		this.mockServer.PUT("/repos/test-owner/test-repo/contents/content/00001.md",
				request -> MockServer.Response.builder()
					.status(201)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body(createResponse)
					.build());

		Entry result = this.entryService.save(null, entry);

		assertThat(result).isEqualTo(entry);
		verify(this.entryRepository, never()).save(entry);
	}

	@Test
	void save_shouldUpdateFileWhenFileExists() {
		Entry entry = createTestEntry(2L);
		String existingSha = "existingsha456";
		String existingFileResponse = """
				{
				  "name": "00002.md",
				  "path": "content/00002.md",
				  "sha": "%s",
				  "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/00002.md",
				  "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/%s",
				  "html_url": "https://github.com/test-owner/test-repo/blob/main/content/00002.md",
				  "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/00002.md",
				  "content": "b2xkIGNvbnRlbnQ=",
				  "type": "file"
				}
				""".formatted(existingSha, existingSha);
		String updateResponse = """
				{
				  "content": {
				    "name": "00002.md",
				    "path": "content/00002.md",
				    "sha": "updatedsha789",
				    "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/00002.md",
				    "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/updatedsha789",
				    "html_url": "https://github.com/test-owner/test-repo/blob/main/content/00002.md",
				    "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/00002.md",
				    "content": "dGVzdA==",
				    "type": "file"
				  },
				  "commit": {
				    "sha": "updatecommit789",
				    "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/updatecommit789",
				    "html_url": "https://github.com/test-owner/test-repo/commit/updatecommit789",
				    "author": {
				      "name": "Test Author",
				      "email": "author@example.com",
				      "date": "2025-06-28T14:00:00Z"
				    },
				    "committer": {
				      "name": "Test Committer",
				      "email": "committer@example.com",
				      "date": "2025-06-28T14:00:00Z"
				    },
				    "tree": {
				      "sha": "tree789",
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree789",
				      "html_url": null
				    },
				    "message": "Update entry 00002",
				    "parents": []
				  }
				}
				""";

		this.mockServer.GET("/repos/test-owner/test-repo/contents/content/00002.md",
				request -> MockServer.Response.json(existingFileResponse));
		this.mockServer.PUT("/repos/test-owner/test-repo/contents/content/00002.md",
				request -> MockServer.Response.json(updateResponse));

		Entry result = this.entryService.save(null, entry);

		assertThat(result).isEqualTo(entry);
		verify(this.entryRepository, never()).save(entry);
	}

	@Test
	void deleteById_shouldDeleteFileWhenFileExists() {
		EntryKey entryKey = new EntryKey(3L, null);
		String existingSha = "deletingsha789";
		String existingFileResponse = """
				{
				  "name": "00003.md",
				  "path": "content/00003.md",
				  "sha": "%s",
				  "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/00003.md",
				  "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/%s",
				  "html_url": "https://github.com/test-owner/test-repo/blob/main/content/00003.md",
				  "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/00003.md",
				  "content": "c29tZSBjb250ZW50",
				  "type": "file"
				}
				""".formatted(existingSha, existingSha);
		String deleteResponse = """
				{
				  "content": null,
				  "commit": {
				    "sha": "deletecommit123",
				    "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/deletecommit123",
				    "html_url": "https://github.com/test-owner/test-repo/commit/deletecommit123",
				    "author": {
				      "name": "Test Author",
				      "email": "author@example.com",
				      "date": "2025-06-28T16:00:00Z"
				    },
				    "committer": {
				      "name": "Test Committer",
				      "email": "committer@example.com",
				      "date": "2025-06-28T16:00:00Z"
				    },
				    "tree": {
				      "sha": "tree999",
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree999",
				      "html_url": null
				    },
				    "message": "Delete entry 00003",
				    "parents": []
				  }
				}
				""";

		this.mockServer.GET("/repos/test-owner/test-repo/contents/content/00003.md",
				request -> MockServer.Response.json(existingFileResponse));
		this.mockServer.DELETE("/repos/test-owner/test-repo/contents/content/00003.md",
				request -> MockServer.Response.json(deleteResponse));

		this.entryService.deleteById(null, entryKey);

		verify(this.entryRepository, never()).deleteById(entryKey);
	}

	@Test
	void save_shouldUseRepositoryWhenDirectUpdateIsFalse() {
		this.gitHubProps.setDirectUpdate(false);
		try {
			Entry entry = createTestEntry(4L);
			when(this.entryRepository.save(entry)).thenReturn(entry);

			Entry result = this.entryService.save(null, entry);

			assertThat(result).isEqualTo(entry);
			verify(this.entryRepository).save(entry);
		}
		finally {
			this.gitHubProps.setDirectUpdate(true);
		}
	}

	@Test
	void updateSummary_shouldUpdateFileOnGitHub() {
		EntryKey entryKey = new EntryKey(5L, null);
		String existingSha = "existingsha555";
		String existingContent = """
				---
				title: Test Entry
				tags: ["test"]
				categories: ["category"]
				---

				Test content
				""";
		String existingFileResponse = """
				{
				  "name": "00005.md",
				  "path": "content/00005.md",
				  "sha": "%s",
				  "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/00005.md",
				  "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/%s",
				  "html_url": "https://github.com/test-owner/test-repo/blob/main/content/00005.md",
				  "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/00005.md",
				  "content": "%s",
				  "type": "file"
				}
				""".formatted(existingSha, existingSha,
				Base64.getEncoder().encodeToString(existingContent.getBytes(StandardCharsets.UTF_8)));
		String updateResponse = """
				{
				  "content": {
				    "name": "00005.md",
				    "path": "content/00005.md",
				    "sha": "updatedsha555",
				    "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/00005.md",
				    "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/updatedsha555",
				    "html_url": "https://github.com/test-owner/test-repo/blob/main/content/00005.md",
				    "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/00005.md",
				    "content": "dGVzdA==",
				    "type": "file"
				  },
				  "commit": {
				    "sha": "updatecommit555",
				    "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/updatecommit555",
				    "html_url": "https://github.com/test-owner/test-repo/commit/updatecommit555",
				    "author": {
				      "name": "Test Author",
				      "email": "author@example.com",
				      "date": "2025-06-28T18:00:00Z"
				    },
				    "committer": {
				      "name": "Test Committer",
				      "email": "committer@example.com",
				      "date": "2025-06-28T18:00:00Z"
				    },
				    "tree": {
				      "sha": "tree555",
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree555",
				      "html_url": null
				    },
				    "message": "Update summary for entry 00005",
				    "parents": []
				  }
				}
				""";

		this.mockServer.GET("/repos/test-owner/test-repo/contents/content/00005.md",
				request -> MockServer.Response.json(existingFileResponse));
		this.mockServer.PUT("/repos/test-owner/test-repo/contents/content/00005.md",
				request -> MockServer.Response.json(updateResponse));

		this.entryService.updateSummary(null, entryKey, "This is a new summary");

		verify(this.entryRepository, never()).updateSummary(entryKey, "This is a new summary");
	}

	@Test
	void updateSummary_shouldThrowExceptionWhenFileNotFound() {
		EntryKey entryKey = new EntryKey(6L, null);

		assertThatThrownBy(() -> this.entryService.updateSummary(null, entryKey, "Some summary"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Entry not found");

		verify(this.entryRepository, never()).updateSummary(entryKey, "Some summary");
	}

	@Test
	void updateSummary_shouldUseRepositoryWhenDirectUpdateIsFalse() {
		this.gitHubProps.setDirectUpdate(false);
		try {
			EntryKey entryKey = new EntryKey(7L, null);

			this.entryService.updateSummary(null, entryKey, "Some summary");

			verify(this.entryRepository).updateSummary(entryKey, "Some summary");
		}
		finally {
			this.gitHubProps.setDirectUpdate(true);
		}
	}

	private Entry createTestEntry(Long id) {
		Instant now = Instant.now();
		return Entry.builder()
			.entryKey(new EntryKey(id, null))
			.frontMatter(FrontMatter.builder()
				.title("Test Entry " + id)
				.tags(List.of(new Tag("test")))
				.categories(List.of(new Category("category")))
				.build())
			.content("Test content for entry " + id)
			.created(Author.builder().name("author").date(now).build())
			.updated(Author.builder().name("author").date(now).build())
			.build();
	}

}
