package am.ik.blog.entry;

import am.ik.blog.markdown.MarkdownToJson;
import am.ik.yavi.arguments.StringValidator;
import am.ik.yavi.validator.Yavi;

/**
 * Markdown text with YAML front matter.
 */
public final class Markdown {

	public static final StringValidator<Markdown> SCHEMA = Yavi
		._string("markdown",
				c -> c.notBlank()
					.message("Markdown must not be empty")
					.predicate(markdown -> markdown.isBlank() || hasFrontMatter(markdown), "entry.markdown.frontMatter",
							"Markdown must contain front matter"))
		.map(Markdown::new);

	private static boolean hasFrontMatter(String markdown) {
		try {
			return MarkdownToJson.convert(markdown).containsKey("frontMatter");
		}
		catch (RuntimeException e) {
			return false;
		}
	}

	private final String value;

	private Markdown(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Markdown that && this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return "Markdown[value=" + this.value + "]";
	}

}
