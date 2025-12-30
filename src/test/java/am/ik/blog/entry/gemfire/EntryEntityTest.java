package am.ik.blog.entry.gemfire;

import am.ik.blog.entry.Author;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.FrontMatter;
import am.ik.blog.entry.Tag;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntryEntityTest {

	@Test
	void fromModel_convertsEntryToEntityWithAllFields() {
		Instant createdAt = Instant.parse("2024-01-15T10:30:00Z");
		Instant updatedAt = Instant.parse("2024-01-20T15:45:00Z");

		Entry entry = Entry.builder()
			.entryKey(new EntryKey(123L))
			.frontMatter(FrontMatter.builder()
				.title("Test Title")
				.summary("Test summary")
				.categories(new Category("Tech"), new Category("Java"))
				.tags(new Tag("spring"), new Tag("boot"))
				.build())
			.content("Test content body")
			.created(Author.builder().name("creator").date(createdAt).build())
			.updated(Author.builder().name("updater").date(updatedAt).build())
			.build();

		EntryEntity entity = EntryEntity.fromModel(entry);

		assertThat(entity.getEntryKey()).isEqualTo("00123");
		assertThat(entity.getTitle()).isEqualTo("Test Title");
		assertThat(entity.getSummary()).isEqualTo("Test summary");
		assertThat(entity.getCategories()).containsExactly("Tech", "Java");
		assertThat(entity.getJoinedCategories()).isEqualTo("Tech|Java");
		assertThat(entity.getTags()).containsExactly("spring", "boot");
		assertThat(entity.getTagWithVersions()).isEmpty();
		assertThat(entity.getContent()).isEqualTo("Test content body");
		assertThat(entity.getCreatedBy()).isEqualTo("creator");
		assertThat(entity.getCreatedAt()).isEqualTo(createdAt.toEpochMilli());
		assertThat(entity.getUpdatedBy()).isEqualTo("updater");
		assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt.toEpochMilli());
		assertThat(entity.getTenantId()).isEqualTo("_");
	}

	@Test
	void fromModel_convertsEntryWithTagVersions() {
		Instant now = Instant.now();

		Entry entry = Entry.builder()
			.entryKey(new EntryKey(456L))
			.frontMatter(FrontMatter.builder()
				.title("Versioned Tags")
				.categories(new Category("Programming"))
				.tags(Tag.builder().name("java").version("21").build(), new Tag("spring"),
						Tag.builder().name("postgresql").version("16.0").build())
				.build())
			.content("Content with versioned tags")
			.created(Author.builder().name("author").date(now).build())
			.updated(Author.builder().name("author").date(now).build())
			.build();

		EntryEntity entity = EntryEntity.fromModel(entry);

		assertThat(entity.getTags()).containsExactly("java", "spring", "postgresql");
		assertThat(entity.getTagWithVersions()).containsExactlyInAnyOrder("java|21", "postgresql|16.0");
	}

	@Test
	void fromModel_handlesNullContent() {
		Instant now = Instant.now();

		Entry entry = Entry.builder()
			.entryKey(new EntryKey(789L))
			.frontMatter(FrontMatter.builder()
				.title("No Content Entry")
				.categories(new Category("Blog"))
				.tags(new Tag("test"))
				.build())
			.content(null)
			.created(Author.builder().name("author").date(now).build())
			.updated(Author.builder().name("author").date(now).build())
			.build();

		EntryEntity entity = EntryEntity.fromModel(entry);

		assertThat(entity.getContent()).isEmpty();
	}

	@Test
	void fromModel_handlesNullAuthorDate() {
		Entry entry = Entry.builder()
			.entryKey(new EntryKey(111L))
			.frontMatter(FrontMatter.builder()
				.title("No Date Entry")
				.categories(new Category("Blog"))
				.tags(new Tag("test"))
				.build())
			.content("Content")
			.created(Author.builder().name("author").date(null).build())
			.updated(Author.builder().name("author").date(null).build())
			.build();

		EntryEntity entity = EntryEntity.fromModel(entry);

		assertThat(entity.getCreatedAt()).isZero();
		assertThat(entity.getUpdatedAt()).isZero();
	}

	@Test
	void fromModel_handlesTenantId() {
		Instant now = Instant.now();

		Entry entry = Entry.builder()
			.entryKey(new EntryKey(100L, "tenant1"))
			.frontMatter(FrontMatter.builder()
				.title("Tenant Entry")
				.categories(new Category("Blog"))
				.tags(new Tag("test"))
				.build())
			.content("Content")
			.created(Author.builder().name("author").date(now).build())
			.updated(Author.builder().name("author").date(now).build())
			.build();

		EntryEntity entity = EntryEntity.fromModel(entry);

		assertThat(entity.getEntryKey()).isEqualTo("00100|tenant1");
		assertThat(entity.getTenantId()).isEqualTo("tenant1");
	}

	@Test
	void toModel_convertsEntityToEntryWithAllFields() {
		long createdAt = Instant.parse("2024-01-15T10:30:00Z").toEpochMilli();
		long updatedAt = Instant.parse("2024-01-20T15:45:00Z").toEpochMilli();

		EntryEntity entity = EntryEntity.builder()
			.entryKey("00123")
			.title("Test Title")
			.summary("Test summary")
			.categories(List.of("Tech", "Java"))
			.joinedCategories("Tech|Java")
			.tags(new LinkedHashSet<>(List.of("spring", "boot")))
			.tagWithVersions(Set.of())
			.content("Test content body")
			.createdBy("creator")
			.createdAt(createdAt)
			.updatedBy("updater")
			.updatedAt(updatedAt)
			.tenantId("_")
			.build();

		Entry entry = entity.toModel();

		assertThat(entry.entryKey().entryId()).isEqualTo(123L);
		assertThat(entry.entryKey().tenantId()).isEqualTo("_");
		assertThat(entry.frontMatter().title()).isEqualTo("Test Title");
		assertThat(entry.frontMatter().summary()).isEqualTo("Test summary");
		assertThat(entry.frontMatter().categories()).extracting(Category::name).containsExactly("Tech", "Java");
		assertThat(entry.frontMatter().tags()).extracting(Tag::name).containsExactly("spring", "boot");
		assertThat(entry.frontMatter().tags()).extracting(Tag::version).containsOnlyNulls();
		assertThat(entry.content()).isEqualTo("Test content body");
		assertThat(entry.created().name()).isEqualTo("creator");
		assertThat(entry.created().date()).isEqualTo(Instant.ofEpochMilli(createdAt));
		assertThat(entry.updated().name()).isEqualTo("updater");
		assertThat(entry.updated().date()).isEqualTo(Instant.ofEpochMilli(updatedAt));
	}

	@Test
	void toModel_convertsEntityWithTagVersions() {
		long now = Instant.now().toEpochMilli();

		EntryEntity entity = EntryEntity.builder()
			.entryKey("00456")
			.title("Versioned Tags")
			.summary("")
			.categories(List.of("Programming"))
			.joinedCategories("Programming")
			.tags(new LinkedHashSet<>(List.of("java", "spring", "postgresql")))
			.tagWithVersions(new LinkedHashSet<>(List.of("java|21", "postgresql|16.0")))
			.content("Content with versioned tags")
			.createdBy("author")
			.createdAt(now)
			.updatedBy("author")
			.updatedAt(now)
			.tenantId("_")
			.build();

		Entry entry = entity.toModel();

		List<Tag> tags = entry.frontMatter().tags();
		assertThat(tags).hasSize(3);

		Tag javaTag = tags.stream().filter(t -> "java".equals(t.name())).findFirst().orElseThrow();
		assertThat(javaTag.version()).isEqualTo("21");

		Tag springTag = tags.stream().filter(t -> "spring".equals(t.name())).findFirst().orElseThrow();
		assertThat(springTag.version()).isNull();

		Tag postgresqlTag = tags.stream().filter(t -> "postgresql".equals(t.name())).findFirst().orElseThrow();
		assertThat(postgresqlTag.version()).isEqualTo("16.0");
	}

	@Test
	void toModel_handlesTenantIdInEntryKey() {
		long now = Instant.now().toEpochMilli();

		EntryEntity entity = EntryEntity.builder()
			.entryKey("00100|tenant1")
			.title("Tenant Entry")
			.summary("")
			.categories(List.of("Blog"))
			.joinedCategories("Blog")
			.tags(new LinkedHashSet<>(List.of("test")))
			.tagWithVersions(Set.of())
			.content("Content")
			.createdBy("author")
			.createdAt(now)
			.updatedBy("author")
			.updatedAt(now)
			.tenantId("tenant1")
			.build();

		Entry entry = entity.toModel();

		assertThat(entry.entryKey().tenantId()).isEqualTo("tenant1");
		assertThat(entry.entryKey().entryId()).isEqualTo(100L);
	}

	@Test
	void roundTrip_fromModelToModelPreservesData() {
		Instant createdAt = Instant.parse("2024-06-01T12:00:00Z");
		Instant updatedAt = Instant.parse("2024-06-15T18:30:00Z");

		Entry original = Entry.builder()
			.entryKey(new EntryKey(999L, "myTenant"))
			.frontMatter(FrontMatter.builder()
				.title("Round Trip Test")
				.summary("Round trip summary")
				.categories(new Category("Category1"), new Category("Category2"), new Category("Category3"))
				.tags(Tag.builder().name("java").version("21").build(), new Tag("spring"),
						Tag.builder().name("docker").version("24.0").build())
				.build())
			.content("This is the original content for round trip testing.")
			.created(Author.builder().name("originalCreator").date(createdAt).build())
			.updated(Author.builder().name("originalUpdater").date(updatedAt).build())
			.build();

		EntryEntity entity = EntryEntity.fromModel(original);
		Entry restored = entity.toModel();

		assertThat(restored.entryKey()).isEqualTo(original.entryKey());
		assertThat(restored.frontMatter().title()).isEqualTo(original.frontMatter().title());
		assertThat(restored.frontMatter().summary()).isEqualTo(original.frontMatter().summary());
		assertThat(restored.frontMatter().categories()).isEqualTo(original.frontMatter().categories());
		assertThat(restored.frontMatter().tags()).containsExactlyInAnyOrderElementsOf(original.frontMatter().tags());
		assertThat(restored.content()).isEqualTo(original.content());
		assertThat(restored.created().name()).isEqualTo(original.created().name());
		assertThat(restored.created().date()).isEqualTo(original.created().date());
		assertThat(restored.updated().name()).isEqualTo(original.updated().name());
		assertThat(restored.updated().date()).isEqualTo(original.updated().date());
	}

}
