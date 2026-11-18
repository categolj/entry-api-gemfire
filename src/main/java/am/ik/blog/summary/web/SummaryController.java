package am.ik.blog.summary.web;

import am.ik.blog.json.JsonNodeReader;
import am.ik.blog.summary.SummaryContent;
import am.ik.blog.summary.SummaryService;
import am.ik.yavi.core.ValueValidator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class SummaryController {

	static final ValueValidator<JsonNode, SummaryContent> VALIDATOR = SummaryContent.SCHEMA
		.readingWith(JsonNodeReader.INSTANCE);

	private final SummaryService summaryService;

	public SummaryController(SummaryService summaryService) {
		this.summaryService = summaryService;
	}

	@PostMapping(path = "/tenants/{tenantId}/summary")
	public SummaryResponse summarize(@PathVariable String tenantId, @RequestBody JsonNode body) {
		SummaryContent content = VALIDATOR.validated(body);
		return new SummaryResponse(this.summaryService.summarize(content));
	}

	public record SummaryResponse(String summary) {

	}

}
