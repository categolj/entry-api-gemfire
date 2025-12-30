package am.ik.blog.entry.gemfire;

import am.ik.query.Query;
import am.ik.query.ast.AndNode;
import am.ik.query.ast.FieldNode;
import am.ik.query.ast.FuzzyNode;
import am.ik.query.ast.Node;
import am.ik.query.ast.NodeVisitor;
import am.ik.query.ast.NotNode;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.PhraseNode;
import am.ik.query.ast.RangeNode;
import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.ast.WildcardNode;
import am.ik.query.parser.QueryParser;
import am.ik.query.util.QueryPrinter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SearchCriteriaToOql {

	private static final String CONTENT_FIELD = "content.toLowerCase()";

	private static final QueryParser queryParser = QueryParser.create();

	private static final Logger logger = LoggerFactory.getLogger(SearchCriteriaToOql.class);

	static QueryAndParams convertQuery(String query, int index) {
		Query parsedQuery = queryParser.parse(query);
		if (logger.isTraceEnabled()) {
			logger.trace("{}", QueryPrinter.toPrettyString(parsedQuery));
		}
		OqlVisitor visitor = new OqlVisitor(new AtomicInteger(index));
		String oql = parsedQuery.accept(visitor);
		return new QueryAndParams(oql, visitor.getParams());
	}

	static QueryAndParams convertTag(String tag, int index) {
		return new QueryAndParams("$" + index + " IN tags", List.of(tag));
	}

	static QueryAndParams convertCategories(List<String> categories, int index) {
		List<String> params = new ArrayList<>();
		StringBuilder categoriesQuery = new StringBuilder("categories.size() >= ").append(categories.size())
			.append(" AND ");
		categoriesQuery.append("(");
		for (int i = 0; i < categories.size(); i++) {
			String category = categories.get(i);
			if (i > 0) {
				categoriesQuery.append(" AND ");
			}
			categoriesQuery.append("categories[").append(i).append("] = $").append(index + i);
			params.add(category);
		}
		categoriesQuery.append(")");
		return new QueryAndParams(categoriesQuery.toString(), params);
	}

	private static class OqlVisitor implements NodeVisitor<String> {

		private final AtomicInteger index;

		private final List<String> params = new ArrayList<>();

		OqlVisitor(AtomicInteger index) {
			this.index = index;
		}

		List<String> getParams() {
			return params.stream().map(String::toLowerCase).toList();
		}

		@Override
		public String visitToken(TokenNode node) {
			String paramName = "$" + index.getAndIncrement();
			params.add("%" + node.value() + "%");
			return CONTENT_FIELD + " LIKE " + paramName;
		}

		@Override
		public String visitRoot(RootNode node) {
			return processChildren(node, " AND ");
		}

		@Override
		public String visitAnd(AndNode node) {
			String result = processChildren(node, " AND ");
			return node.parent() != null ? "(" + result + ")" : result;
		}

		@Override
		public String visitOr(OrNode node) {
			String result = processChildren(node, " OR ");
			return node.parent() != null ? "(" + result + ")" : result;
		}

		@Override
		public String visitNot(NotNode node) {
			if (!node.children().isEmpty()) {
				Node child = node.children().getFirst();
				if (child instanceof TokenNode) {
					String paramName = "$" + index.getAndIncrement();
					params.add("%" + child.value() + "%");
					return "NOT (" + CONTENT_FIELD + " LIKE " + paramName + ")";
				}
				else {
					String childResult = child.accept(this);
					return "NOT (" + childResult + ")";
				}
			}
			return "";
		}

		@Override
		public String visitPhrase(PhraseNode node) {
			String paramName = "$" + index.getAndIncrement();
			params.add("%" + node.value() + "%");
			return CONTENT_FIELD + " LIKE " + paramName;
		}

		@Override
		public String visitWildcard(WildcardNode node) {
			String paramName = "$" + index.getAndIncrement();
			// Convert wildcard pattern to OQL LIKE pattern
			String pattern = node.value().replace("*", "%").replace("?", "_");
			params.add(pattern);
			return CONTENT_FIELD + " LIKE " + paramName;
		}

		// Ignore field queries, fuzzy queries, and ranges for this use-case
		@Override
		public String visitField(FieldNode node) {
			return "";
		}

		@Override
		public String visitFuzzy(FuzzyNode node) {
			return "";
		}

		@Override
		public String visitRange(RangeNode node) {
			return "";
		}

		private String processChildren(Node node, String operator) {
			List<Node> children = node.children();
			if (children.isEmpty()) {
				return "";
			}

			List<String> childResults = new ArrayList<>();
			for (Node child : children) {
				String result = child.accept(this);
				if (!result.isEmpty()) {
					childResults.add(result);
				}
			}

			return String.join(operator, childResults);
		}

	}

	record QueryAndParams(String query, List<String> params) {
	}

}