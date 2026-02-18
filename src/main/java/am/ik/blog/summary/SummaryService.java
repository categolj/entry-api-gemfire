package am.ik.blog.summary;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SummaryService {

	private final ChatClient chatClient;

	private final String chatModel;

	private final Logger logger = LoggerFactory.getLogger(SummaryService.class);

	public SummaryService(ChatClient.Builder chatClientBuilder,
			@Value("${spring.ai.openai.chat.options.model:N/A}") String chatModel) {
		this.chatClient = chatClientBuilder.build();
		this.chatModel = chatModel;
	}

	public String summarize(String content) {
		logger.info("action=start_summarization model={}", chatModel);
		long start = System.currentTimeMillis();
		String text = Objects.requireNonNull(this.chatClient.prompt()
			.system("""
					You are a professional editor. Your role is to create a concise summary of the text (blog article) that the user inputs. Please summarize it within the character limit that can be posted on X/Twitter (about 140 chars). Also, assuming it will be used as the OGP description for an SNS post introducing the blog article, it is preferable that the content is clear from the first sentence.
					Use the same language as the input text. Do not include markdown/HTML in the summary text. Also do not use markup such as `code` formatting. The summary should be in a format that introduces the blog article, such as "This is an article about..." or "In this article...".
					Your response should contain only the summary text and nothing else.
					""")
			.user(u -> u.text(content))
			.stream()
			.content()
			.collectList()
			.map(list -> String.join("", list))
			.block());
		long end = System.currentTimeMillis();
		logger.info("action=finish_summarization model={} duration={}", chatModel, end - start);
		return text;
	}

}
