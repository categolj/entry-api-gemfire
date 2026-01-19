package am.ik.blog.github;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "/repos/{owner}/{repo}")
public interface GitHubClient {

	@GetExchange(url = "/contents/{path}")
	ResponseEntity<@NonNull File> getFile(@PathVariable("owner") String owner, @PathVariable("repo") String repo,
			@PathVariable("path") String path);

	@GetExchange(url = "/commits")
	List<Commit> getCommits(@PathVariable("owner") String owner, @PathVariable("repo") String repo,
			@RequestParam MultiValueMap<String, String> params);

	@PutExchange(url = "/contents/{path}")
	ResponseEntity<FileCommitResponse> createFile(@PathVariable("owner") String owner,
			@PathVariable("repo") String repo, @PathVariable("path") String path,
			@RequestBody CreateFileRequest request);

	@PutExchange(url = "/contents/{path}")
	ResponseEntity<FileCommitResponse> updateFile(@PathVariable("owner") String owner,
			@PathVariable("repo") String repo, @PathVariable("path") String path,
			@RequestBody UpdateFileRequest request);

	@DeleteExchange(url = "/contents/{path}")
	ResponseEntity<FileCommitResponse> deleteFile(@PathVariable("owner") String owner,
			@PathVariable("repo") String repo, @PathVariable("path") String path,
			@RequestBody DeleteFileRequest request);

}
