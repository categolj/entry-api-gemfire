package am.ik.blog.github;

import org.jspecify.annotations.Nullable;

/**
 * Response DTO for GitHub content creation/update/delete API.
 *
 * @param content file metadata (null for delete operations)
 * @param commit commit details
 */
public record FileCommitResponse(@Nullable File content, GitCommit commit) {
}
