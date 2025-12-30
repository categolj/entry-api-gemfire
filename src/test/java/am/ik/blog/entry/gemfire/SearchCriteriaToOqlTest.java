package am.ik.blog.entry.gemfire;

import am.ik.blog.entry.gemfire.SearchCriteriaToOql.QueryAndParams;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCriteriaToOqlTest {

	@Test
	void simpleQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("hello", 1);
		assertThat(queryAndParams.query()).isEqualTo("content.toLowerCase() LIKE $1");
		assertThat(queryAndParams.params()).containsExactly("%hello%");
	}

	@Test
	void andQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("hello world", 1);
		assertThat(queryAndParams.query()).isEqualTo("content.toLowerCase() LIKE $1 AND content.toLowerCase() LIKE $2");
		assertThat(queryAndParams.params()).containsExactly("%hello%", "%world%");
	}

	@Test
	void caseInsensitiveQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("Hello World", 1);
		assertThat(queryAndParams.query()).isEqualTo("content.toLowerCase() LIKE $1 AND content.toLowerCase() LIKE $2");
		assertThat(queryAndParams.params()).containsExactly("%hello%", "%world%");
	}

	@Test
	void quotedQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("\"hello world\"", 1);
		assertThat(queryAndParams.query()).isEqualTo("content.toLowerCase() LIKE $1");
		assertThat(queryAndParams.params()).containsExactly("%hello world%");
	}

	@Test
	void orQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("hello or world", 1);
		assertThat(queryAndParams.query()).isEqualTo("content.toLowerCase() LIKE $1 OR content.toLowerCase() LIKE $2");
		assertThat(queryAndParams.params()).containsExactly("%hello%", "%world%");
	}

	@Test
	void notQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("hello -world", 1);
		assertThat(queryAndParams.query())
			.isEqualTo("content.toLowerCase() LIKE $1 AND NOT (content.toLowerCase() LIKE $2)");
		assertThat(queryAndParams.params()).containsExactly("%hello%", "%world%");
	}

	@Test
	void singleNotQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("-hello", 1);
		assertThat(queryAndParams.query()).isEqualTo("NOT (content.toLowerCase() LIKE $1)");
		assertThat(queryAndParams.params()).containsExactly("%hello%");
	}

	@Test
	void hyphenQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("hello-world", 1);
		assertThat(queryAndParams.query()).isEqualTo("content.toLowerCase() LIKE $1");
		assertThat(queryAndParams.params()).containsExactly("%hello-world%");
	}

	@Test
	void nestedQuery() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertQuery("hello (world or java)", 1);
		assertThat(queryAndParams.query()).isEqualTo(
				"content.toLowerCase() LIKE $1 AND (content.toLowerCase() LIKE $2 OR content.toLowerCase() LIKE $3)");
		assertThat(queryAndParams.params()).containsExactly("%hello%", "%world%", "%java%");
	}

	@Test
	void tag() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertTag("foo", 1);
		assertThat(queryAndParams.query()).isEqualTo("$1 IN tags");
		assertThat(queryAndParams.params()).containsExactly("foo");
	}

	@Test
	void categories() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertCategories(List.of("cat1", "cat2", "cat3"), 1);
		// ensure that the categories size is more than or equal to the number of
		// requested categories
		assertThat(queryAndParams.query())
			.isEqualTo("categories.size() >= 3 AND (categories[0] = $1 AND categories[1] = $2 AND categories[2] = $3)");
		assertThat(queryAndParams.params()).containsExactly("cat1", "cat2", "cat3");
	}

	@Test
	void singleCategory() {
		QueryAndParams queryAndParams = SearchCriteriaToOql.convertCategories(List.of("cat1"), 1);
		assertThat(queryAndParams.query()).isEqualTo("categories.size() >= 1 AND (categories[0] = $1)");
		assertThat(queryAndParams.params()).containsExactly("cat1");
	}

}