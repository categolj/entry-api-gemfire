package am.ik.blog.entry.web;

import am.ik.blog.GitHubProps;
import am.ik.blog.entry.CacheNames;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryFetcher;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.EntryRepository;
import am.ik.webhook.WebhookAuthenticationException;
import am.ik.webhook.WebhookVerifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static am.ik.webhook.WebhookHttpHeaders.X_HUB_SIGNATURE_256;
import static java.util.stream.Collectors.toUnmodifiableMap;

@RestController
public class WebhookController {

	private final EntryFetcher entryFetcher;

	private final EntryRepository entryRepository;

	private final WebhookVerifier webhookVerifier;

	private final Map<String, WebhookVerifier> tenantsWebhookVerifier;

	private final JsonMapper jsonMapper;

	@Nullable private final CacheManager cacheManager;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public WebhookController(GitHubProps props, EntryFetcher entryFetcher, EntryRepository entryRepository,
			JsonMapper jsonMapper, ObjectProvider<CacheManager> cacheManager) {
		this.entryFetcher = entryFetcher;
		this.entryRepository = entryRepository;
		this.webhookVerifier = WebhookVerifier.gitHubSha256(props.getWebhookSecret());
		this.tenantsWebhookVerifier = props.getTenants()
			.entrySet()
			.stream()
			.collect(toUnmodifiableMap(Map.Entry::getKey,
					e -> WebhookVerifier.gitHubSha256(e.getValue().getWebhookSecret())));
		this.jsonMapper = jsonMapper;
		this.cacheManager = cacheManager.getIfAvailable();
	}

	@PostMapping(path = { "/webhook", "/tenants/{tenantId}/webhook" })
	public ResponseEntity<?> webhook(@RequestHeader(name = X_HUB_SIGNATURE_256) String signature,
			@RequestBody String payload, @PathVariable(required = false) String tenantId) {
		WebhookVerifier verifier = tenantId == null ? this.webhookVerifier
				: this.tenantsWebhookVerifier.getOrDefault(tenantId, this.webhookVerifier);
		try {
			verifier.verify(payload, signature);
		}
		catch (WebhookAuthenticationException e) {
			return ResponseEntity.badRequest()
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid signature: " + signature));
		}
		return this.processWebhook(payload, tenantId)
			.<ResponseEntity<?>>map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.badRequest()
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid payload: " + payload)));
	}

	Optional<List<Map<String, EntryKey>>> processWebhook(String payload, @Nullable String tenantId) {
		final JsonNode node;
		try {
			node = this.jsonMapper.readValue(payload, JsonNode.class);
		}
		catch (JacksonException e) {
			return Optional.empty();
		}
		final JsonNode repository = node.get("repository");
		if (repository == null || !repository.hasNonNull("full_name") || !node.has("commits")) {
			return Optional.empty();
		}
		final String[] ownerAndRepo = repository.get("full_name").asText().split("/", 2);
		if (ownerAndRepo.length != 2) {
			return Optional.empty();
		}
		final String owner = ownerAndRepo[0];
		final String repo = ownerAndRepo[1];
		final Stream<JsonNode> commits = StreamSupport.stream(node.get("commits").spliterator(), false);
		final List<Map<String, EntryKey>> result = new ArrayList<>();
		final List<EntryKey> updatedEntries = new ArrayList<>();
		commits.forEach(commit -> {
			Stream.of("added", "modified").forEach(key -> {
				this.paths(commit.get(key)).forEach(path -> {
					Optional<Entry> fetch = this.entryFetcher.fetch(tenantId, owner, repo, path);
					fetch.ifPresent(entry -> {
						result.add(Map.of(key, entry.entryKey()));
						this.entryRepository.save(entry);
						updatedEntries.add(entry.entryKey());
					});
				});
			});
			this.paths(commit.get("removed")).forEach(path -> {
				Optional<EntryKey> fetch = this.entryFetcher.fetch(tenantId, owner, repo, path).map(Entry::entryKey);
				fetch.ifPresent(entryKey -> {
					result.add(Map.of("removed", entryKey));
					this.entryRepository.deleteById(entryKey);
					updatedEntries.add(entryKey);
				});
			});
		});
		if (this.cacheManager != null) {
			Cache latestEntriesCache = this.cacheManager.getCache(CacheNames.LATEST_ENTRIES);
			Cache entryCache = this.cacheManager.getCache(CacheNames.ENTRY);
			if (entryCache != null) {
				for (EntryKey entryKey : updatedEntries) {
					logger.info("Evicting entry cache for entryKey: {}", entryKey);
					entryCache.evict(entryKey);
				}
			}
			if (latestEntriesCache != null) {
				String key = EntryKey.requireNonNullTenantId(tenantId);
				logger.info("Evicting latestEntries cache for tenantId: {}", key);
				latestEntriesCache.evict(key);
			}
		}
		return Optional.of(result);
	}

	Stream<String> paths(@Nullable JsonNode paths) {
		return paths == null ? Stream.empty() : StreamSupport.stream(paths.spliterator(), false).map(JsonNode::asText);
	}

}
