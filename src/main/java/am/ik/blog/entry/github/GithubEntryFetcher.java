package am.ik.blog.entry.github;

import am.ik.blog.entry.Author;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryFetcher;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.EntryParser;
import am.ik.blog.github.Commit;
import am.ik.blog.github.CommitParameter;
import am.ik.blog.github.File;
import am.ik.blog.github.GitCommitter;
import am.ik.blog.github.GitHubClient;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

@Component
public class GithubEntryFetcher implements EntryFetcher {

	private final EntryParser entryParser;

	private final GitHubClient gitHubClient;

	private final HttpServiceProxyRegistry registry;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public GithubEntryFetcher(EntryParser entryParser, HttpServiceProxyRegistry registry) {
		this.entryParser = entryParser;
		this.gitHubClient = registry.getClient("github", GitHubClient.class);
		this.registry = registry;
	}

	@Override
	public Optional<Entry> fetch(@Nullable String tenantId, String owner, String repo, String path) {
		GitHubClient gitHubClient;
		if (EntryKey.isDefaultTenant(tenantId)) {
			gitHubClient = this.gitHubClient;
		}
		else {
			gitHubClient = this.registry.getClient("github.%s".formatted(tenantId), GitHubClient.class);
		}
		Long entryId = Entry.parseId(Paths.get(path).getFileName().toString());
		EntryKey entryKey = new EntryKey(entryId, tenantId);
		ResponseEntity<File> response = gitHubClient.getFile(owner, repo, path);
		HttpStatusCode statusCode = response.getStatusCode();
		if (statusCode == HttpStatus.OK) {
			File file = response.getBody();
			Assert.notNull(file, "File must not be null");
			logger.info("Retrieved file: {}", file.url());
			List<Commit> commits = gitHubClient.getCommits(owner, repo, new CommitParameter().path(path).queryParams());
			Author created = commits.isEmpty() ? Author.builder().name("unknown").build() : toAuthor(commits.getLast());
			Author updated = commits.isEmpty() ? Author.builder().name("unknown").build()
					: toAuthor(commits.getFirst());
			return Optional.of(this.entryParser.fromMarkdown(entryKey, file.decode(), created, updated).build());
		}
		else if (statusCode.is4xxClientError()) {
			logger.info("Failed to retrieve file statusCode: {}, tenantId: {}, owner: {}, repo: {}, path: {}",
					statusCode.value(), tenantId, owner, repo, path);
			return Optional.empty();
		}
		else {
			throw new ResponseStatusException(statusCode,
					"Unexpected response returned from Github File API :" + statusCode);
		}
	}

	private Author toAuthor(Commit commit) {
		GitCommitter committer = commit.commit().author();
		return new Author(committer.name(), committer.date());
	}

}