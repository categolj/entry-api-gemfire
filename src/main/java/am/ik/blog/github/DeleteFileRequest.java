package am.ik.blog.github;

import org.jspecify.annotations.Nullable;

/**
 * Request DTO for deleting a file via GitHub Contents API.
 *
 * @param message commit message
 * @param sha SHA of the file being deleted
 * @param branch target branch (optional)
 * @param committer committer info (optional)
 * @param author author info (optional)
 */
public record DeleteFileRequest(String message, String sha, @Nullable String branch, @Nullable GitCommitter committer,
		@Nullable GitCommitter author) {

	public DeleteFileRequest(String message, String sha) {
		this(message, sha, null, null, null);
	}

	public DeleteFileRequest(String message, String sha, String branch) {
		this(message, sha, branch, null, null);
	}

}
