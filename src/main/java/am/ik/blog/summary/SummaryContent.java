package am.ik.blog.summary;

import am.ik.yavi.input.ObjectInputValidator;
import am.ik.yavi.validator.Yavi;

/**
 * The content of a summarization request.
 */
public final class SummaryContent {

	public static final ObjectInputValidator<SummaryContent> SCHEMA = Yavi.schema()
		._string("content", c -> c.notBlank().message("Content must not be empty"))
		.required("Content must not be empty")
		.fromMap(SummaryContent::new);

	private final String value;

	private SummaryContent(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof SummaryContent that && this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return "SummaryContent[value=" + this.value + "]";
	}

}
