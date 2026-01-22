package am.ik.blog.edit;

public enum EditMode {

	PROOFREADING("""
			You need to proofread user-entered text (blog posts) for formatting and stylistic issues only.
			Fix typos, punctuation errors, and grammatical mistakes.
			Do not change the content, structure, or meaning of the text.
			Do not add or remove any sentences.
			"""), //
	COMPLETION("""
			In addition to proofreading, fill in missing sentences or explanations that are lacking.
			If a sentence is incomplete or an explanation is insufficient, complete it naturally.
			Do not add entirely new topics or sections.
			"""), //
	EXPANSION(
			"""
					In addition to proofreading and completing the article, imagine what the author would write next and continue the article naturally.
					Do not add headings like "Follow-up" or "Continuation". Just seamlessly extend the content.
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
