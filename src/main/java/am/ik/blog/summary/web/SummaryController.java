package am.ik.blog.summary.web;

import am.ik.blog.summary.SummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SummaryController {

	private final SummaryService summaryService;

	public SummaryController(SummaryService summaryService) {
		this.summaryService = summaryService;
	}

	@PostMapping(path = "/summarize")
	public SummaryResponse summarize(@RequestBody SummaryRequest request) {
		if (request.content() == null || request.content().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must not be empty");
		}
		String summary = this.summaryService.summarize(request.content());
		return new SummaryResponse(summary);
	}

	public record SummaryRequest(String content) {

	}

	public record SummaryResponse(String summary) {

	}

}
