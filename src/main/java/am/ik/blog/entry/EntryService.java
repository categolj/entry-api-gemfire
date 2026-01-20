package am.ik.blog.entry;

import am.ik.blog.GitHubProps;
import am.ik.blog.github.CreateFileRequest;
import am.ik.blog.github.DeleteFileRequest;
import am.ik.blog.github.File;
import am.ik.blog.github.GitHubClient;
import am.ik.blog.github.UpdateFileRequest;
import am.ik.blog.security.Authorized;
import am.ik.blog.security.Privilege;
import am.ik.pagination.CursorPage;
import am.ik.pagination.CursorPageRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

@Service
public class EntryService {

	private final Logger logger = LoggerFactory.getLogger(EntryService.class);

	public static final int DEFAULT_PAGE_SIZE = 30;

	private static final CursorPageRequest<Instant> DEFAULT_CURSOR_REQUEST = new CursorPageRequest<>(null,
			DEFAULT_PAGE_SIZE, CursorPageRequest.Navigation.NEXT);

	private final EntryRepository entryRepository;

	private final GitHubProps gitHubProps;

	private final GitHubClient gitHubClient;

	private final HttpServiceProxyRegistry registry;

	private final EntryParser entryParser;

	public EntryService(EntryRepository entryRepository, GitHubProps gitHubProps, HttpServiceProxyRegistry registry,
			EntryParser entryParser) {
		this.entryRepository = entryRepository;
		this.gitHubProps = gitHubProps;
		this.registry = registry;
		this.gitHubClient = registry.getClient("github", GitHubClient.class);
		this.entryParser = entryParser;
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.GET)
	public Optional<Entry> findById(@Nullable @P("tenantId") String tenantId, EntryKey entryKey) {
		return entryRepository.findById(entryKey);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public List<Entry> findAll(@Nullable @P("tenantId") String tenantId, List<EntryKey> entryKeys) {
		return entryRepository.findAll(entryKeys);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public CursorPage<Entry, Instant> findOrderByUpdated(@Nullable @P("tenantId") String tenantId,
			SearchCriteria searchCriteria, CursorPageRequest<Instant> pageRequest) {
		return entryRepository.findOrderByUpdated(tenantId, searchCriteria, pageRequest);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public CursorPage<Entry, Instant> findLatest(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.findOrderByUpdated(tenantId, SearchCriteria.NULL_CRITERIA, DEFAULT_CURSOR_REQUEST);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public List<List<Category>> findAllCategories(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.findAllCategories(tenantId);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public List<TagAndCount> findAllTags(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.findAllTags(tenantId);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	public Entry save(@Nullable @P("tenantId") String tenantId, Entry entry) {
		if (this.gitHubProps.isDirectUpdate()) {
			return saveToGitHub(tenantId, entry);
		}
		return entryRepository.save(entry);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	public Long nextId(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.nextId(tenantId);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	public void saveAll(@Nullable @P("tenantId") String tenantId, Entry... entries) {
		entryRepository.saveAll(entries);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	public void saveAll(@Nullable @P("tenantId") String tenantId, List<Entry> entries) {
		entryRepository.saveAll(entries);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.DELETE)
	public void deleteById(@Nullable @P("tenantId") String tenantId, EntryKey entryKey) {
		if (this.gitHubProps.isDirectUpdate()) {
			deleteFromGitHub(tenantId, entryKey);
			return;
		}
		entryRepository.deleteById(entryKey);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	public void updateSummary(@Nullable @P("tenantId") String tenantId, EntryKey entryKey, String summary) {
		if (this.gitHubProps.isDirectUpdate()) {
			Entry entry = fetchFromGitHub(tenantId, entryKey);
			FrontMatter updatedFrontMatter = entry.frontMatter().toBuilder().summary(summary).build();
			Entry updatedEntry = entry.toBuilder().frontMatter(updatedFrontMatter).build();
			saveToGitHub(tenantId, updatedEntry);
			return;
		}
		entryRepository.updateSummary(entryKey, summary);
	}

	private Entry saveToGitHub(@Nullable String tenantId, Entry entry) {
		String owner = getOwner(tenantId);
		String repo = getRepo(tenantId);
		String path = getFilePath(entry.entryKey());
		GitHubClient client = getGitHubClient(tenantId);
		String content = Base64.getEncoder().encodeToString(entry.toMarkdown().getBytes(StandardCharsets.UTF_8));
		String formattedId = Entry.formatId(entry.entryKey().entryId());
		ResponseEntity<File> getResponse = client.getFile(owner, repo, path);
		if (getResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
			String message = "Create entry %s".formatted(formattedId);
			logger.info("action=create_file tenantId={} owner={} repo={} path={}", tenantId, owner, repo, path);
			ResponseEntity<?> createResponse = client.createFile(owner, repo, path,
					new CreateFileRequest(message, content));
			if (createResponse.getStatusCode().isError()) {
				throw new ResponseStatusException(HttpStatus.valueOf(createResponse.getStatusCode().value()),
						"Failed to create file on GitHub: " + createResponse.getStatusCode());
			}
		}
		else {
			File file = getResponse.getBody();
			String sha = (file != null) ? file.sha() : "";
			String message = "Update entry %s".formatted(formattedId);
			logger.info("action=update_file tenantId={} owner={} repo={} path={} sha={}", tenantId, owner, repo, path,
					sha);
			ResponseEntity<?> updateResponse = client.updateFile(owner, repo, path,
					new UpdateFileRequest(message, content, sha));
			if (updateResponse.getStatusCode().isError()) {
				throw new ResponseStatusException(HttpStatus.valueOf(updateResponse.getStatusCode().value()),
						"Failed to update file on GitHub: " + updateResponse.getStatusCode());
			}
		}
		// Also update the repository
		this.entryRepository.save(entry);
		return entry;
	}

	private void deleteFromGitHub(@Nullable String tenantId, EntryKey entryKey) {
		String owner = getOwner(tenantId);
		String repo = getRepo(tenantId);
		String path = getFilePath(entryKey);
		GitHubClient client = getGitHubClient(tenantId);
		String formattedId = Entry.formatId(entryKey.entryId());
		ResponseEntity<File> getResponse = client.getFile(owner, repo, path);
		if (getResponse.getStatusCode() != HttpStatus.NOT_FOUND) {
			File file = getResponse.getBody();
			String sha = (file != null) ? file.sha() : "";
			String message = "Delete entry %s".formatted(formattedId);
			logger.info("action=delete_file tenantId={} owner={} repo={} path={} sha={}", tenantId, owner, repo, path,
					sha);
			ResponseEntity<?> deleteResponse = client.deleteFile(owner, repo, path,
					new DeleteFileRequest(message, sha));
			if (deleteResponse.getStatusCode().isError()) {
				throw new ResponseStatusException(HttpStatus.valueOf(deleteResponse.getStatusCode().value()),
						"Failed to delete file on GitHub: " + deleteResponse.getStatusCode());
			}
		}
		else {
			logger.info("action=skip_delete tenantId={} reason=not_found owner={} repo={} path={}", tenantId, owner,
					repo, path);
		}
		// Also delete from the repository
		this.entryRepository.deleteById(entryKey);
	}

	private Entry fetchFromGitHub(@Nullable String tenantId, EntryKey entryKey) {
		String owner = getOwner(tenantId);
		String repo = getRepo(tenantId);
		String path = getFilePath(entryKey);
		GitHubClient client = getGitHubClient(tenantId);
		logger.info("action=fetch_file tenantId={} owner={} repo={} path={}", tenantId, owner, repo, path);
		ResponseEntity<File> response = client.getFile(owner, repo, path);
		if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
			logger.warn("action=fetch_file tenantId={} status=not_found owner={} repo={} path={}", tenantId, owner,
					repo, path);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found: " + entryKey);
		}
		File file = response.getBody();
		if (file == null) {
			logger.warn("action=fetch_file tenantId={} status=body_null owner={} repo={} path={}", tenantId, owner,
					repo, path);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found: " + entryKey);
		}
		Author unknownAuthor = Author.builder().name("unknown").build();
		return this.entryParser.fromMarkdown(entryKey, file.decode(), unknownAuthor, unknownAuthor).build();
	}

	private String getFilePath(EntryKey entryKey) {
		return "content/%s.md".formatted(Entry.formatId(entryKey.entryId()));
	}

	private String getOwner(@Nullable String tenantId) {
		if (EntryKey.isDefaultTenant(tenantId)) {
			return this.gitHubProps.getContentOwner();
		}
		GitHubProps tenantProps = this.gitHubProps.getTenants().get(tenantId);
		if (tenantProps == null) {
			throw new IllegalArgumentException("Unknown tenant: " + tenantId);
		}
		return tenantProps.getContentOwner();
	}

	private String getRepo(@Nullable String tenantId) {
		if (EntryKey.isDefaultTenant(tenantId)) {
			return this.gitHubProps.getContentRepo();
		}
		GitHubProps tenantProps = this.gitHubProps.getTenants().get(tenantId);
		if (tenantProps == null) {
			throw new IllegalArgumentException("Unknown tenant: " + tenantId);
		}
		return tenantProps.getContentRepo();
	}

	private GitHubClient getGitHubClient(@Nullable String tenantId) {
		if (EntryKey.isDefaultTenant(tenantId)) {
			return this.gitHubClient;
		}
		return this.registry.getClient("github.%s".formatted(tenantId), GitHubClient.class);
	}

}
