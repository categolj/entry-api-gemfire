package am.ik.blog.edit.web;

import am.ik.blog.edit.EditMode;
import am.ik.blog.edit.EditService;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
public class EditController {

	private final EditService editService;

	public EditController(EditService editService) {
		this.editService = editService;
	}

	@PostMapping(path = "/tenants/{tenantId}/edit")
	public EditResponse edit(@PathVariable String tenantId, @RequestBody EditRequest request) {
		if (request.content() == null || request.content().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must not be empty");
		}
		String content = this.editService.edit(request.content(),
				Objects.requireNonNullElse(request.mode(), EditMode.PROOFREADING));
		return new EditResponse(content);
	}

	public record EditRequest(@Nullable String content, @Nullable EditMode mode) {

	}

	public record EditResponse(String content) {
	}

}
