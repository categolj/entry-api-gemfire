package am.ik.blog.github;

import org.jspecify.annotations.Nullable;

/**
 * Request DTO for creating a new file via GitHub Contents API.
 *
 * @param message commit message
 * @param content Base64 encoded file content
 * @param branch target branch (optional)
 * @param committer committer info (optional)
 * @param author author info (optional)
 */
public record CreateFileRequest(String message, String content, @Nullable String branch,
		@Nullable GitCommitter committer, @Nullable GitCommitter author) {

	public CreateFileRequest(String message, String content) {
		this(message, content, null, null, null);
	}

	public CreateFileRequest(String message, String content, String branch) {
		this(message, content, branch, null, null);
	}

}
