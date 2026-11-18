package am.ik.blog.entry;

import am.ik.blog.markdown.MarkdownToJson;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EntryParser {

	private final JsonMapper jsonMapper;

	public EntryParser(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public Entry.Builder fromMarkdown(EntryKey entryKey, Markdown markdown, Author created, Author updated) {
		Map<String, Object> json = MarkdownToJson.convert(markdown.value());
		Map<String, Object> frontMatter = (Map<String, Object>) json.get("frontMatter");
		Assert.notNull(frontMatter, "FrontMatter must not be null");
		return this.jsonMapper.convertValue(json, Entry.class)
			.toBuilder()
			.entryKey(entryKey)
			.created(frontMatter.containsKey(FrontMatter.DATE_FIELD)
					? created.withDate(((Date) frontMatter.get(FrontMatter.DATE_FIELD)).toInstant()) : created)
			.updated(frontMatter.containsKey(FrontMatter.UPDATE_FIELD)
					? updated.withDate(((Date) frontMatter.get(FrontMatter.UPDATE_FIELD)).toInstant()) : updated);
	}

}
