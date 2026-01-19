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

	@Test
	void createFile_shouldReturnFileCommitResponseOnSuccess() {
		String originalContent = "# New File\n\nThis is a new file.";
		String encodedContent = Base64.getEncoder().encodeToString(originalContent.getBytes());
		String jsonResponse = """
				{
				  "content": {
				    "name": "new-file.md",
				    "path": "content/new-file.md",
				    "sha": "newfile123",
				    "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/new-file.md",
				    "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/newfile123",
				    "html_url": "https://github.com/test-owner/test-repo/blob/main/content/new-file.md",
				    "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/new-file.md",
				    "content": "%s",
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
				    "message": "Create new-file.md",
				    "parents": []
				  }
				}
				""".formatted(encodedContent);

		this.mockServer.PUT("/repos/test-owner/test-repo/contents/content/new-file.md",
				request -> MockServer.Response.builder()
					.status(201)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body(jsonResponse)
					.build());

		CreateFileRequest createRequest = new CreateFileRequest("Create new-file.md", encodedContent);
		ResponseEntity<FileCommitResponse> response = this.gitHubClient.createFile("test-owner", "test-repo",
				"content/new-file.md", createRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().content()).isNotNull();

		File content = response.getBody().content();
		assertThat(content.name()).isEqualTo("new-file.md");
		assertThat(content.path()).isEqualTo("content/new-file.md");
		assertThat(content.sha()).isEqualTo("newfile123");
		assertThat(response.getBody().commit().sha()).isEqualTo("commit123abc");
		assertThat(response.getBody().commit().message()).isEqualTo("Create new-file.md");
		assertThat(response.getBody().commit().author().name()).isEqualTo("Test Author");
	}

	@Test
	void createFile_shouldReturn409WhenFileExists() {
		String encodedContent = Base64.getEncoder().encodeToString("content".getBytes());
		String errorResponse = """
				{
				  "message": "Invalid request.\\n\\n\\"sha\\" wasn't supplied.",
				  "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents"
				}
				""";

		this.mockServer.PUT("/repos/test-owner/test-repo/contents/content/existing-file.md",
				request -> MockServer.Response.builder()
					.status(409)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body(errorResponse)
					.build());

		CreateFileRequest createRequest = new CreateFileRequest("Create file", encodedContent);
		ResponseEntity<FileCommitResponse> response = this.gitHubClient.createFile("test-owner", "test-repo",
				"content/existing-file.md", createRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void updateFile_shouldReturnFileCommitResponseOnSuccess() {
		String originalContent = "# Updated File\n\nThis file has been updated.";
		String encodedContent = Base64.getEncoder().encodeToString(originalContent.getBytes());
		String jsonResponse = """
				{
				  "content": {
				    "name": "updated-file.md",
				    "path": "content/updated-file.md",
				    "sha": "updatedsha456",
				    "url": "https://api.github.com/repos/test-owner/test-repo/contents/content/updated-file.md",
				    "git_url": "https://api.github.com/repos/test-owner/test-repo/git/blobs/updatedsha456",
				    "html_url": "https://github.com/test-owner/test-repo/blob/main/content/updated-file.md",
				    "download_url": "https://raw.githubusercontent.com/test-owner/test-repo/main/content/updated-file.md",
				    "content": "%s",
				    "type": "file"
				  },
				  "commit": {
				    "sha": "updatecommit789",
				    "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/updatecommit789",
				    "html_url": "https://github.com/test-owner/test-repo/commit/updatecommit789",
				    "author": {
				      "name": "Update Author",
				      "email": "update@example.com",
				      "date": "2025-06-28T14:00:00Z"
				    },
				    "committer": {
				      "name": "Update Committer",
				      "email": "update@example.com",
				      "date": "2025-06-28T14:00:00Z"
				    },
				    "tree": {
				      "sha": "tree789",
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree789",
				      "html_url": null
				    },
				    "message": "Update updated-file.md",
				    "parents": [
				      {
				        "sha": "parent123",
				        "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/parent123",
				        "html_url": "https://github.com/test-owner/test-repo/commit/parent123"
				      }
				    ]
				  }
				}
				"""
			.formatted(encodedContent);

		this.mockServer.PUT("/repos/test-owner/test-repo/contents/content/updated-file.md",
				request -> MockServer.Response.json(jsonResponse));

		UpdateFileRequest updateRequest = new UpdateFileRequest("Update updated-file.md", encodedContent, "oldsha123");
		ResponseEntity<FileCommitResponse> response = this.gitHubClient.updateFile("test-owner", "test-repo",
				"content/updated-file.md", updateRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().content()).isNotNull();

		File content = response.getBody().content();
		assertThat(content.name()).isEqualTo("updated-file.md");
		assertThat(content.path()).isEqualTo("content/updated-file.md");
		assertThat(content.sha()).isEqualTo("updatedsha456");
		assertThat(response.getBody().commit().sha()).isEqualTo("updatecommit789");
		assertThat(response.getBody().commit().message()).isEqualTo("Update updated-file.md");
		assertThat(response.getBody().commit().parents()).hasSize(1);
		assertThat(response.getBody().commit().parents().getFirst().sha()).isEqualTo("parent123");
	}

	@Test
	void updateFile_shouldReturn409OnShaMismatch() {
		String encodedContent = Base64.getEncoder().encodeToString("content".getBytes());
		String errorResponse = """
				{
				  "message": "content/file.md does not match abc123",
				  "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents"
				}
				""";

		this.mockServer.PUT("/repos/test-owner/test-repo/contents/content/file.md",
				request -> MockServer.Response.builder()
					.status(409)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body(errorResponse)
					.build());

		UpdateFileRequest updateRequest = new UpdateFileRequest("Update file", encodedContent, "wrongsha");
		ResponseEntity<FileCommitResponse> response = this.gitHubClient.updateFile("test-owner", "test-repo",
				"content/file.md", updateRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void deleteFile_shouldReturnFileCommitResponseOnSuccess() {
		String jsonResponse = """
				{
				  "content": null,
				  "commit": {
				    "sha": "deletecommit123",
				    "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/deletecommit123",
				    "html_url": "https://github.com/test-owner/test-repo/commit/deletecommit123",
				    "author": {
				      "name": "Delete Author",
				      "email": "delete@example.com",
				      "date": "2025-06-28T16:00:00Z"
				    },
				    "committer": {
				      "name": "Delete Committer",
				      "email": "delete@example.com",
				      "date": "2025-06-28T16:00:00Z"
				    },
				    "tree": {
				      "sha": "tree999",
				      "url": "https://api.github.com/repos/test-owner/test-repo/git/trees/tree999",
				      "html_url": null
				    },
				    "message": "Delete file.md",
				    "parents": [
				      {
				        "sha": "parent456",
				        "url": "https://api.github.com/repos/test-owner/test-repo/git/commits/parent456",
				        "html_url": "https://github.com/test-owner/test-repo/commit/parent456"
				      }
				    ]
				  }
				}
				""";

		this.mockServer.DELETE("/repos/test-owner/test-repo/contents/content/file.md",
				request -> MockServer.Response.json(jsonResponse));

		DeleteFileRequest deleteRequest = new DeleteFileRequest("Delete file.md", "sha123");
		ResponseEntity<FileCommitResponse> response = this.gitHubClient.deleteFile("test-owner", "test-repo",
				"content/file.md", deleteRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();

		FileCommitResponse body = response.getBody();
		assertThat(body.content()).isNull();
		assertThat(body.commit().sha()).isEqualTo("deletecommit123");
		assertThat(body.commit().message()).isEqualTo("Delete file.md");
		assertThat(body.commit().author().name()).isEqualTo("Delete Author");
		assertThat(body.commit().parents()).hasSize(1);
		assertThat(body.commit().parents().getFirst().sha()).isEqualTo("parent456");
	}

	@Test
	void deleteFile_shouldReturn409OnShaMismatch() {
		String errorResponse = """
				{
				  "message": "content/file.md does not match wrongsha",
				  "documentation_url": "https://docs.github.com/rest/repos/contents#delete-a-file"
				}
				""";

		this.mockServer.DELETE("/repos/test-owner/test-repo/contents/content/file.md",
				request -> MockServer.Response.builder()
					.status(409)
					.contentType(MediaType.APPLICATION_JSON_VALUE)
					.body(errorResponse)
					.build());

		DeleteFileRequest deleteRequest = new DeleteFileRequest("Delete file", "wrongsha");
		ResponseEntity<FileCommitResponse> response = this.gitHubClient.deleteFile("test-owner", "test-repo",
				"content/file.md", deleteRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

}
