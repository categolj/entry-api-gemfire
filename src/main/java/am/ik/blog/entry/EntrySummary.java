package am.ik.blog.entry;

import am.ik.yavi.input.ObjectInputValidator;
import am.ik.yavi.validator.Yavi;

/**
 * The summary of an entry.
 */
public final class EntrySummary {

	public static final ObjectInputValidator<EntrySummary> SCHEMA = Yavi.schema()
		._string("summary", c -> c.notBlank().message("Summary must not be empty"))
		.required("Summary must not be empty")
		.fromMap(EntrySummary::new);

	private final String value;

	private EntrySummary(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof EntrySummary that && this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return "EntrySummary[value=" + this.value + "]";
	}

}
