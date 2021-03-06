package scw.json;

import scw.lang.Nullable;
import scw.value.Value;

public interface JsonElement extends Value, JSONAware {
	@Nullable
	JsonArray getAsJsonArray();

	@Nullable
	JsonObject getAsJsonObject();

	default boolean isJsonArray() {
		String text = getAsString();
		return text.startsWith(JsonArray.PREFIX)
				&& text.endsWith(JsonArray.SUFFIX);
	}

	default boolean isJsonObject() {
		String text = getAsString();
		return text.startsWith(JsonObject.PREFIX)
				&& text.endsWith(JsonObject.SUFFIX);
	}
}
