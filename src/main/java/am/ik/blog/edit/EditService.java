package am.ik.blog.edit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EditService {

	private final ChatClient chatClient;

	private final String chatModel;

	private final Logger logger = LoggerFactory.getLogger(EditService.class);

	public EditService(ChatClient.Builder chatClientBuilder,
			@Value("${spring.ai.openai.chat.options.model:N/A}") String chatModel) {
		this.chatClient = chatClientBuilder.build();
		this.chatModel = chatModel;
	}

	public String edit(String content, EditMode editMode) {
		logger.info("action=start_edit mode={} model={}", editMode, chatModel);
		long start = System.currentTimeMillis();
		String text = this.chatClient.prompt()
			.system(editMode.systemPrompt())
			.user(u -> u.text(content))
			.stream()
			.content()
			.collectList()
			.map(list -> String.join("", list))
			.block();
		long end = System.currentTimeMillis();
		logger.info("action=finish_edit mode={} model={} duration={}", editMode, chatModel, end - start);
		return text == null ? "" : text;
	}

}
