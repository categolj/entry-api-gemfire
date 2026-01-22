package am.ik.blog.edit;

public enum EditMode {

	PROOFREADING("""
			You need to proofread user-entered text (blog posts).
			Edits that significantly alter the intent of the text are not required.
			"""), //
	COMPLETION(
			"""
					In addition to proofreading the article, your role is to fill in any unnatural parts of the text with natural ones.
					You do not need to add any new facts.
					"""), //
	EXPANSION(
			"""
					In addition to proofreading and completing the article, you will need to thoughtfully write a follow-up to the article.
					""");

	private final String systemPrompt;

	EditMode(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public String systemPrompt() {
		return """
				You are a professional technical editor.
				%s
				Please reply only the edited text.
				""".formatted(systemPrompt);
	}

}
