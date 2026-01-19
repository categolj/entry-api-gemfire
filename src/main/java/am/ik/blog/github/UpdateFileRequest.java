package am.ik.blog.github;

import org.jspecify.annotations.Nullable;

/**
 * Request DTO for updating an existing file via GitHub Contents API.
 *
 * @param message commit message
 * @param content Base64 encoded file content
 * @param sha SHA of the file being replaced
 * @param branch target branch (optional)
 * @param committer committer info (optional)
 * @param author author info (optional)
 */
public record UpdateFileRequest(String message, String content, String sha, @Nullable String branch,
		@Nullable GitCommitter committer, @Nullable GitCommitter author) {

	public UpdateFileRequest(String message, String content, String sha) {
		this(message, content, sha, null, null, null);
	}

	public UpdateFileRequest(String message, String content, String sha, String branch) {
		this(message, content, sha, branch, null, null);
	}

}
