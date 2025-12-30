package am.ik.blog.entry.gemfire;

import am.ik.blog.MockConfig;
import am.ik.blog.TestcontainersConfiguration;
import am.ik.blog.entry.Author;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.FrontMatter;
import am.ik.blog.entry.MockData;
import am.ik.blog.entry.SearchCriteria;
import am.ik.blog.entry.Tag;
import am.ik.pagination.CursorPage;
import am.ik.pagination.CursorPageRequest;
import am.ik.pagination.CursorPageRequest.Navigation;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties = { "logging.level.am.ik.blog.entry.gemfire.GemfireEntryRepository=warn" })
@Testcontainers(disabledWithoutDocker = true)
@Import({ TestcontainersConfiguration.class, MockConfig.class })
class GemfireEntryRepositoryTest {

	@Autowired
	GemfireEntryRepository entryRepository;

	@BeforeEach
	void setup() {
		this.entryRepository.deleteAll();
		this.entryRepository.saveAll(MockData.ALL_ENTRIES);
	}

	@Test
	void saveAndFindAndUpdateAndDelete() {
		Instant now = Instant.now();
		EntryKey entryKey = new EntryKey(11L);
		Entry entry = Entry.builder()
			.entryKey(entryKey)
			.frontMatter(FrontMatter.builder()
				.title("Test Entry Title")
				.summary("This is a brief summary of the test entry content.")
				.tags(Tag.builder().name("postgresql").version("15.0").build(), Tag.builder().name("database").build(),
						Tag.builder().name("aurora").version("2.0").build())
				.categories(new Category("Technology"), new Category("Programming"))
				.build())
			.content(
					"This is a test content for the entry. It contains some sample text to demonstrate the content field.")
			.created(Author.builder().name("test").date(now).build())
			.updated(Author.builder().name("test").date(now).build())
			.build();
		this.entryRepository.save(entry);
		assertThat(this.entryRepository.findById(entryKey)).hasValueSatisfying(e -> compareIgnoringDate(e, entry));
		Entry updated = entry.toBuilder()
			.frontMatter(entry.frontMatter().toBuilder().title("New Title").build())
			.content("Updated content")
			.build();
		this.entryRepository.save(updated);
		assertThat(this.entryRepository.findById(entryKey)).hasValueSatisfying(e -> compareIgnoringDate(e, updated));
		this.entryRepository.deleteById(entryKey);
		assertThat(this.entryRepository.exists(entryKey)).isFalse();
	}

	void compareIgnoringDate(Entry actual, Entry expected) {
		assertThat(ignoreDate(actual)).isEqualTo(ignoreDate(expected));
	}

	Entry ignoreDate(Entry entry) {
		return entry.toBuilder()
			.created(entry.created().toBuilder().date(null).build())
			.updated(entry.updated().toBuilder().date(null).build())
			.build();
	}

	@Test
	void findAllWithEntryKeys() {
		List<Entry> entries = this.entryRepository
			.findAll(List.of(new EntryKey(1L), new EntryKey(3L), new EntryKey(5L), new EntryKey(15L)));
		assertThat(entries).hasSize(3);
		assertThat(entries).extracting(e -> e.entryKey().entryId()).containsExactly(1L, 3L, 5L);
		assertThat(entries).extracting(e -> e.frontMatter().title())
			.containsExactly("Getting Started with Spring Boot", "Building RESTful APIs with Node.js and Express",
					"Database Design Best Practices");

	}

	@Test
	void findOrderByUpdated() {
		SearchCriteria searchCriteria = SearchCriteria.builder().build();
		int pageSize = 6;
		CursorPage<Entry, Instant> page1 = this.entryRepository.findOrderByUpdated(null, searchCriteria,
				new CursorPageRequest<>(null, pageSize, Navigation.NEXT));
		assertThat(page1.content()).extracting(e -> e.entryKey().entryId()).containsExactly(10L, 9L, 8L, 7L, 6L, 5L);
		assertThat(page1.hasNext()).isTrue();
		assertThat(page1.hasPrevious()).isFalse();
		CursorPage<Entry, Instant> page2 = this.entryRepository.findOrderByUpdated(null, searchCriteria,
				new CursorPageRequest<>(page1.head(), pageSize, Navigation.NEXT));
		assertThat(page2.content()).extracting(e -> e.entryKey().entryId()).containsExactly(4L, 3L, 2L, 1L);
		assertThat(page2.hasNext()).isFalse();
		assertThat(page2.hasPrevious()).isTrue();
	}

	@Test
	void findOrderByUpdatedByTag() {
		SearchCriteria searchCriteria = SearchCriteria.builder().tag("aws").build();
		int pageSize = 3;
		CursorPage<Entry, Instant> page1 = this.entryRepository.findOrderByUpdated(null, searchCriteria,
				new CursorPageRequest<>(null, pageSize, Navigation.NEXT));
		assertThat(page1.content()).extracting(e -> e.entryKey().entryId()).containsExactly(10L);
		assertThat(page1.hasNext()).isFalse();
	}

	@Test
	void findOrderByUpdatedByCategories() {
		SearchCriteria searchCriteria = SearchCriteria.builder().categories(List.of("Programming")).build();
		int pageSize = 3;
		CursorPage<Entry, Instant> page1 = this.entryRepository.findOrderByUpdated(null, searchCriteria,
				new CursorPageRequest<>(null, pageSize, Navigation.NEXT));
		assertThat(page1.content()).extracting(e -> e.entryKey().entryId()).containsExactly(4L, 3L, 1L);
		assertThat(page1.hasNext()).isFalse();
	}

	@Test
	void findOrderByUpdatedByCategoriesTwoLayers() {
		SearchCriteria searchCriteria = SearchCriteria.builder()
			.categories(List.of("Programming", "JavaScript"))
			.build();
		int pageSize = 3;
		CursorPage<Entry, Instant> page1 = this.entryRepository.findOrderByUpdated(null, searchCriteria,
				new CursorPageRequest<>(null, pageSize, Navigation.NEXT));
		assertThat(page1.content()).extracting(e -> e.entryKey().entryId()).containsExactly(4L, 3L);
		assertThat(page1.hasNext()).isFalse();
	}

	@Test
	void findOrderByUpdatedByQuery() {
		SearchCriteria searchCriteria = SearchCriteria.builder().query("install").build();
		int pageSize = 3;
		CursorPage<Entry, Instant> page1 = this.entryRepository.findOrderByUpdated(null, searchCriteria,
				new CursorPageRequest<>(null, pageSize, Navigation.NEXT));
		assertThat(page1.content()).extracting(e -> e.entryKey().entryId()).containsExactly(6L, 3L, 2L);
		assertThat(page1.hasNext()).isFalse();
	}

	@Test
	void findOrderByUpdatedByQueryAnd() {
		SearchCriteria searchCriteria = SearchCriteria.builder().query("npm install").build();
		int pageSize = 3;
		CursorPage<Entry, Instant> page1 = this.entryRepository.findOrderByUpdated(null, searchCriteria,
				new CursorPageRequest<>(null, pageSize, Navigation.NEXT));
		assertThat(page1.content()).extracting(e -> e.entryKey().entryId()).containsExactly(3L);
		assertThat(page1.hasNext()).isFalse();
	}

	@Test
	void nextId() {
		{
			Long nextId = this.entryRepository.nextId(null);
			assertThat(nextId).isEqualTo(11L);
		}
		{
			Long nextId = this.entryRepository.nextId("foo");
			assertThat(nextId).isEqualTo(1L);
		}
	}

}