package am.ik.blog.edit;

import am.ik.yavi.input.ObjectInputValidator;
import am.ik.yavi.validator.Yavi;

/**
 * The input of an editing request.
 */
public final class EditCommand {

	public static final ObjectInputValidator<EditCommand> SCHEMA = Yavi.schema()
		._string("content", c -> c.notBlank().message("Content must not be empty"))
		.required("Content must not be empty")
		._enum("mode", EditMode.class)
		.defaultValue(EditMode.PROOFREADING)
		.fromMap(EditCommand::new);

	private final String content;

	private final EditMode mode;

	private EditCommand(String content, EditMode mode) {
		this.content = content;
		this.mode = mode;
	}

	public String content() {
		return this.content;
	}

	public EditMode mode() {
		return this.mode;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof EditCommand that && this.content.equals(that.content) && this.mode == that.mode;
	}

	@Override
	public int hashCode() {
		return 31 * this.content.hashCode() + this.mode.hashCode();
	}

	@Override
	public String toString() {
		return "EditCommand[content=" + this.content + ", mode=" + this.mode + "]";
	}

}
