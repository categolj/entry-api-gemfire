package am.ik.blog.github;

import am.ik.blog.MockConfig;
import am.ik.blog.mockserver.MockServer;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MockConfig.class, GitHubClientTest.TestConfig.class })
class GitHubClientTest {

	@Autowired
	GitHubClient gitHubClient;

	@Autowired
	MockServer mockServer;

	@TestConfiguration
	static class TestConfig {

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

	}

	@BeforeEach
	void setUp() {
		this.mockServer.reset()
			.fallback(MockServer.Response.builder()
				.status(404)
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.body("{\"message\":\"Not Found\"}")
				.build());
	}

	@Test
	void getFile_shouldReturnFileOnSuccess() {
		String originalContent = "# Hello World\n\nThis is a test file.";
		String encodedContent = Base64.getEncoder().encodeToString(originalContent.getBytes());
		String jsonResponse = """
				{
				  "name": "00001.md",
				  "path": "content/00001.md",
				  "sha": "abc123def456",
				  "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/00001.md",
				  "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/abc123def456",
				  "html_url": "https://github.com/test-owner/test-repo/blob/main/content/00001.md",
				  "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/00001.md",
				  "content": "%s",
				  "type": "file"
				}
				""".formatted(encodedContent);

		this.mockServer.GET("/repos/test-owner/test-repo/contents/content/00001.md",
				request -> MockServer.Response.json(jsonResponse));

		ResponseEntity<File> response = this.gitHubClient.getFile("test-owner", "test-repo", "content/00001.md");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();

		File file = response.getBody();
		assertThat(file.name()).isEqualTo("00001.md");
		assertThat(file.path()).isEqualTo("content/00001.md");
		assertThat(file.sha()).isEqualTo("abc123def456");
		assertThat(file.type()).isEqualTo("file");
		assertThat(file.decode()).isEqualTo(originalContent);
	}

	@Test
	void getFile_shouldReturn404WhenFileNotFound() {
		ResponseEntity<File> response = this.gitHubClient.getFile("test-owner", "test-repo", "content/nonexistent.md");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getCommits_shouldReturnCommitsOnSuccess() {
		String jsonResponse = """
				[
				  {
				    "sha": "sha123abc",
				    "url": "https://api.github.com/repos/test-owner/test-repo/commits/sha123abc",
				    "html_url": "https://github.com/test-owner/test-repo/commit/sha123abc",
				    "comments_url": "https://api.github.com/repos/test-owner/test-repo/commits/sha123abc/comments",
				    "commit": {
				      "sha": null,
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/sha123abc",
				      "html_url": null,
				      "author": {
				        "name": "Test Author",
				        "email": "test@example.com",
				        "date": "2025-06-27T15:55:20Z"
				      },
				      "committer": {
				        "name": "Test Committer",
				        "email": "committer@example.com",
				        "date": "2025-06-27T16:00:00Z"
				      },
				      "tree": {
				        "sha": "tree123",
				        "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree123",
				        "html_url": null
				      },
				      "message": "Initial commit",
				      "parents": []
				    },
				    "author": null,
				    "committer": null,
				    "parents": []
				  }
				]
				""";

		this.mockServer.GET("/repos/test-owner/test-repo/commits", request -> MockServer.Response.json(jsonResponse));

		List<Commit> commits = this.gitHubClient.getCommits("test-owner", "test-repo",
				new CommitParameter().queryParams());

		assertThat(commits).hasSize(1);

		Commit commit = commits.getFirst();
		assertThat(commit.sha()).isEqualTo("sha123abc");
		assertThat(commit.commit()).isNotNull();
		assertThat(commit.commit().author().name()).isEqualTo("Test Author");
		assertThat(commit.commit().author().email()).isEqualTo("test@example.com");
		assertThat(commit.commit().message()).isEqualTo("Initial commit");
	}

	@Test
	void getCommits_shouldPassPathParameter() {
		String jsonResponse = """
				[
				  {
				    "sha": "pathcommit123",
				    "url": "https://api.github.com/repos/test-owner/test-repo/commits/pathcommit123",
				    "html_url": "https://github.com/test-owner/test-repo/commit/pathcommit123",
				    "comments_url": "https://api.github.com/repos/test-owner/test-repo/commits/pathcommit123/comments",
				    "commit": {
				      "sha": null,
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/pathcommit123",
				      "html_url": null,
				      "author": {
				        "name": "Path Author",
				        "email": "path@example.com",
				        "date": "2025-06-28T10:00:00Z"
				      },
				      "committer": {
				        "name": "Path Committer",
				        "email": "path@example.com",
				        "date": "2025-06-28T10:00:00Z"
				      },
				      "tree": {
				        "sha": "tree456",
				        "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree456",
				        "html_url": null
				      },
				      "message": "Update specific file",
				      "parents": []
				    },
				    "author": null,
				    "committer": null,
				    "parents": []
				  }
				]
				""";

		this.mockServer.GET("/repos/test-owner/test-repo/commits", request -> {
			assertThat(request.queryParam("path")).isEqualTo("content/specific-file.md");
			return MockServer.Response.json(jsonResponse);
		});

		List<Commit> commits = this.gitHubClient.getCommits("test-owner", "test-repo",
				new CommitParameter().path("content/specific-file.md").queryParams());

		assertThat(commits).hasSize(1);
		assertThat(commits.getFirst().sha()).isEqualTo("pathcommit123");
		assertThat(commits.getFirst().commit().message()).isEqualTo("Update specific file");
	}

	@Test
	void getCommits_shouldReturnEmptyListWhenNoCommits() {
		this.mockServer.GET("/repos/test-owner/test-repo/commits", request -> MockServer.Response.json("[]"));

		List<Commit> commits = this.gitHubClient.getCommits("test-owner", "test-repo",
				new CommitParameter().queryParams());

		assertThat(commits).isEmpty();
	}

}
