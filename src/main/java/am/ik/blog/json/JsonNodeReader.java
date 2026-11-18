package am.ik.blog.json;

import am.ik.yavi.core.CoercibleInputReader;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Reads a Jackson JSON tree for YAVI schemas.
 */
public final class JsonNodeReader implements CoercibleInputReader<JsonNode> {

	public static final JsonNodeReader INSTANCE = new JsonNodeReader();

	private JsonNodeReader() {
	}

	@Override
	public boolean isNull(JsonNode node) {
		return node.isNull() || node.isMissingNode();
	}

	@Override
	public Object value(JsonNode node) {
		if (node.isString()) {
			return node.stringValue();
		}
		if (node.isNumber()) {
			return node.numberValue();
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		return node;
	}

	@Override
	public boolean isObject(JsonNode node) {
		return node.isObject();
	}

	@Override
	public @Nullable JsonNode property(JsonNode object, String name) {
		return object.get(name);
	}

	@Override
	public Iterable<Map.Entry<String, JsonNode>> properties(JsonNode object) {
		return object.properties();
	}

	@Override
	public boolean isArray(JsonNode node) {
		return node.isArray();
	}

	@Override
	public Iterable<JsonNode> elements(JsonNode array) {
		return array;
	}

}
