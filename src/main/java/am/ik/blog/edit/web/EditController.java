package am.ik.blog.edit.web;

import am.ik.blog.edit.EditCommand;
import am.ik.blog.edit.EditService;
import am.ik.blog.json.JsonNodeReader;
import am.ik.yavi.core.ValueValidator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class EditController {

	static final ValueValidator<JsonNode, EditCommand> VALIDATOR = EditCommand.SCHEMA
		.readingWith(JsonNodeReader.INSTANCE);

	private final EditService editService;

	public EditController(EditService editService) {
		this.editService = editService;
	}

	@PostMapping(path = "/tenants/{tenantId}/edit")
	public EditResponse edit(@PathVariable String tenantId, @RequestBody JsonNode body) {
		EditCommand command = VALIDATOR.validated(body);
		return new EditResponse(this.editService.edit(command));
	}

	public record EditResponse(String content) {
	}

}
