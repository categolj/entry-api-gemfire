package am.ik.blog.config;

import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.core.ConstraintViolationsException;
import java.util.stream.Collectors;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Converts a YAVI validation failure into a 400 response.
 */
@RestControllerAdvice
public class ConstraintViolationsExceptionHandler {

	@ExceptionHandler(ConstraintViolationsException.class)
	public ResponseEntity<ProblemDetail> handle(ConstraintViolationsException e) {
		String detail = e.violations().stream().map(ConstraintViolation::message).collect(Collectors.joining(", "));
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, detail);
		problemDetail.setProperty("errors", e.violations().details());
		return ResponseEntity.badRequest().body(problemDetail);
	}

}
