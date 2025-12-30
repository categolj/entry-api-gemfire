package am.ik.blog.entry.gemfire;

import am.ik.blog.GitHubProps;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryFetcher;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.EntryRepository;
import am.ik.blog.entry.SearchCriteria;
import am.ik.blog.entry.Tag;
import am.ik.blog.entry.TagAndCount;
import am.ik.pagination.CursorPage;
import am.ik.pagination.CursorPageRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.Struct;
import org.apache.geode.cache.query.TypeMismatchException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Repository
@Observed
public class GemfireEntryRepository implements EntryRepository {

	private final Region<String, EntryEntity> entryRegion;

	private final QueryService queryService;

	private final EntryFetcher entryFetcher;

	private final GitHubProps gitHubProps;

	private final Logger logger = LoggerFactory.getLogger(GemfireEntryRepository.class);

	public GemfireEntryRepository(@Qualifier("entryRegion") Region<String, EntryEntity> entryRegion,
			ClientCache clientCache, EntryFetcher entryFetcher, GitHubProps gitHubProps) {
		this.entryRegion = entryRegion;
		this.queryService = clientCache.getQueryService();
		this.entryFetcher = entryFetcher;
		this.gitHubProps = gitHubProps;
	}

	public boolean exists(EntryKey entryKey) {
		String gemfireKey = EntryEntity.toGemfireKey(entryKey);
		return entryRegion.containsKeyOnServer(gemfireKey);
	}

	@Override
	public Optional<Entry> findById(EntryKey entryKey) {
		String gemfireKey = EntryEntity.toGemfireKey(entryKey);
		if (entryRegion.containsKeyOnServer(gemfireKey)) {
			return Optional.ofNullable(entryRegion.get(gemfireKey)).map(EntryEntity::toModel);
		}
		// Cache Aside
		Optional<Entry> entry;
		if (!entryKey.isDefaultTenant()) {
			GitHubProps tenantProps = this.gitHubProps.getTenants().get(entryKey.tenantId());
			if (tenantProps == null) {
				throw new IllegalStateException("Could not find tenant definition: " + entryKey.tenantId());
			}
			entry = this.entryFetcher.fetch(entryKey.tenantId(), tenantProps.getContentOwner(),
					tenantProps.getContentRepo(), "content/%s.md".formatted(Entry.formatId(entryKey.entryId())));
		}
		else {
			entry = this.entryFetcher.fetch(null, this.gitHubProps.getContentOwner(), this.gitHubProps.getContentRepo(),
					"content/%s.md".formatted(Entry.formatId(entryKey.entryId())));
		}
		return entry.map(this::save);
	}

	@Override
	public List<Entry> findAll(List<EntryKey> entryKeys) {
		List<String> gemfireKeys = entryKeys.stream().map(EntryEntity::toGemfireKey).toList();
		return this.entryRegion.getAll(gemfireKeys)
			.values()
			.stream()
			.filter(Objects::nonNull)
			.peek(entry -> entry.setContent("")) // for backward-compatibility
			.sorted(Comparator.comparing(EntryEntity::getEntryKey))
			.map(EntryEntity::toModel)
			.toList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public CursorPage<Entry, Instant> findOrderByUpdated(@Nullable String tenantId, SearchCriteria searchCriteria,
			CursorPageRequest<Instant> pageRequest) {
		try {
			Optional<Instant> cursor = pageRequest.cursorOptional();
			int pageSizePlus1 = pageRequest.pageSize() + 1;
			List<Object> params = new ArrayList<>(Arrays.asList(EntryKey.requireNonNullTenantId(tenantId),
					cursor.map(Instant::toEpochMilli).orElse(Long.MAX_VALUE), pageSizePlus1));
			String query = """
					SELECT
					    entryKey,
					    title,
					    summary,
					    categories,
					    tags,
					    tagWithVersions,
					    createdBy,
					    createdAt,
					    updatedBy,
					    updatedAt,
					    tenantId
					FROM
					    /Entry
					WHERE
					    tenantId = $1
					    /* QUERY */
					    /* TAG */
					    /* CATEGORIES */
					    AND updatedAt < $2
					ORDER BY
					    updatedAt DESC
					LIMIT $3
					""";
			if (StringUtils.hasText(searchCriteria.query())) {
				var queryAndParams = SearchCriteriaToOql.convertQuery(searchCriteria.query(), params.size() + 1);
				query = query.replace("/* QUERY */", "AND (" + queryAndParams.query() + ")");
				params.addAll(queryAndParams.params());
			}
			if (StringUtils.hasText(searchCriteria.tag())) {
				var queryAndParams = SearchCriteriaToOql.convertTag(searchCriteria.tag(), params.size() + 1);
				query = query.replace("/* TAG */", "AND (" + queryAndParams.query() + ")");
				params.addAll(queryAndParams.params());
			}
			if (!CollectionUtils.isEmpty(searchCriteria.categories())) {
				var queryAndParams = SearchCriteriaToOql.convertCategories(searchCriteria.categories(),
						params.size() + 1);
				query = query.replace("/* CATEGORIES */", "AND (" + queryAndParams.query() + ")");
				params.addAll(queryAndParams.params());
			}
			logger.debug("Executing query: {}, params: {}", query, params);
			List<Entry> contentPlus1 = ((SelectResults<Struct>) this.queryService.newQuery(query)
				.execute(params.toArray()))
				.stream()
				.map(struct -> EntryEntity.builder()
					.entryKey((String) struct.get("entryKey"))
					.title((String) struct.get("title"))
					.summary((String) struct.get("summary"))
					.categories((List<String>) struct.get("categories"))
					.tags((Set<String>) struct.get("tags"))
					.tagWithVersions((Set<String>) struct.get("tagWithVersions"))
					.content("")
					.createdBy((String) struct.get("createdBy"))
					.createdAt((Long) struct.get("createdAt"))
					.updatedBy((String) struct.get("updatedBy"))
					.updatedAt((Long) struct.get("updatedAt"))
					.tenantId((String) struct.get("tenantId"))
					.build())
				.map(EntryEntity::toModel)
				.toList();
			boolean hasPrevious = cursor.isPresent();
			boolean hasNext = contentPlus1.size() == pageSizePlus1;
			List<Entry> content = hasNext ? contentPlus1.subList(0, pageRequest.pageSize()) : contentPlus1;
			return new CursorPage<>(content, pageRequest.pageSize(), entry -> Objects.requireNonNull(entry.toCursor()),
					hasPrevious, hasNext);
		}
		catch (FunctionDomainException | QueryInvocationTargetException | NameResolutionException
				| TypeMismatchException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Entry save(Entry entry) {
		Assert.notNull(entry, "entry must not be null");
		Assert.notNull(entry.entryKey(), "entryId must not be null");
		String gemfireKey = EntryEntity.toGemfireKey(entry.entryKey());
		this.entryRegion.put(gemfireKey, EntryEntity.fromModel(entry));
		return entry;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Long nextId(@Nullable String tenantId) {
		try {
			List<String> results = ((SelectResults<String>) this.queryService.newQuery("""
					SELECT
					    entryKey
					FROM
					    /Entry
					WHERE
					    tenantId = $1
					ORDER BY
					    entryKey DESC
					LIMIT 1
					""").execute(EntryKey.requireNonNullTenantId(tenantId))).asList();
			if (results.isEmpty()) {
				return 1L; // If no entries exist, start with ID 1
			}
			String entryKey = results.getFirst();
			return EntryKey.valueOf(entryKey).entryId() + 1;
		}
		catch (FunctionDomainException | QueryInvocationTargetException | NameResolutionException
				| TypeMismatchException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<List<Category>> findAllCategories(@Nullable String tenantId) {
		try {
			return ((SelectResults<Struct>) this.queryService.newQuery("""
					SELECT DISTINCT
					    categories,
					    joinedCategories
					FROM
					    /Entry
					ORDER BY
					    joinedCategories
					""").execute(EntryKey.requireNonNullTenantId(tenantId))).stream().map(struct -> {
				List<String> categories = (List<String>) struct.get("categories");
				return categories.stream().map(Category::new).toList();
			}).toList();
		}
		catch (FunctionDomainException | QueryInvocationTargetException | NameResolutionException
				| TypeMismatchException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TagAndCount> findAllTags(@Nullable String tenantId) {
		try {
			return ((SelectResults<Struct>) this.queryService.newQuery("""
					SELECT
					    tag,
					    COUNT(*) AS "count"
					FROM
					    /Entry e,
					    e.tags tag
					WHERE
					    e.tenantId = $1
					GROUP BY
					    tag
					ORDER BY
					    tag
					""").execute(EntryKey.requireNonNullTenantId(tenantId))).stream().map(struct -> {
				String tagName = (String) struct.get("tag");
				Integer count = (Integer) struct.get("count");
				return new TagAndCount(new Tag(tagName), count);
			}).toList();
		}
		catch (FunctionDomainException | QueryInvocationTargetException | NameResolutionException
				| TypeMismatchException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void saveAll(Entry... entries) {
		this.saveAll(Arrays.asList(entries));
	}

	@Override
	public void saveAll(List<Entry> entries) {
		this.entryRegion.putAll(entries.stream()
			.map(EntryEntity::fromModel)
			.collect(Collectors.toMap(EntryEntity::getEntryKey, Function.identity())));
	}

	@Override
	public void deleteById(EntryKey entryKey) {
		String gemfireKey = EntryEntity.toGemfireKey(entryKey);
		this.entryRegion.remove(gemfireKey);
	}

	@Override
	public void updateSummary(EntryKey entryKey, String summary) {
		this.findById(entryKey).ifPresent(entry -> {
			this.save(entry.toBuilder().frontMatter(entry.frontMatter().toBuilder().summary(summary).build()).build());
		});
	}

	public void deleteAll() {
		this.entryRegion.removeAll(this.entryRegion.keySetOnServer());
	}

}
