import { Category, CreateEntryRequest, Entry, PaginationResult, ProblemDetail, SearchCriteria, TagAndCount, UpdateEntryRequest } from '../types';
import { createMarkdownWithFrontMatter } from '../utils';

const DEFAULT_TENANT = '_';

// Auth context for getting auth header
let getAuthHeader: (() => string | null) | null = null;
let skipAuthRedirect = false;

export function setAuthHeaderProvider(provider: () => string | null) {
  getAuthHeader = provider;
}

export function setSkipAuthRedirect(skip: boolean) {
  skipAuthRedirect = skip;
}

class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public problemDetail?: ProblemDetail
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// Spring Boot default error response format
interface SpringBootError {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}

// Extract error message from various error response formats
function extractErrorMessage(errorBody: ProblemDetail | SpringBootError): string {
  // ProblemDetail format (RFC 7807)
  if ('detail' in errorBody && errorBody.detail) {
    return errorBody.detail;
  }
  // Spring Boot default error format
  if ('message' in errorBody && errorBody.message) {
    return errorBody.message;
  }
  // Fallback to error field
  if ('error' in errorBody && errorBody.error) {
    return errorBody.error;
  }
  return 'API request failed';
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // Handle authentication errors
    if (response.status === 401 || response.status === 403) {
      if (!skipAuthRedirect) {
        // Store the error message for the login page
        sessionStorage.setItem('authError', response.status === 401 ? 'Authentication failed. Please check your credentials.' : 'Access denied.');
        // Redirect to login page
        window.location.href = '/';
      }
      throw new ApiError('Authentication required', response.status);
    }

    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      try {
        const errorBody = await response.json() as ProblemDetail | SpringBootError;
        const message = extractErrorMessage(errorBody);
        // Store as ProblemDetail if it has ProblemDetail fields
        const problemDetail: ProblemDetail | undefined = 'detail' in errorBody ? errorBody : undefined;
        throw new ApiError(message, response.status, problemDetail);
      } catch (e) {
        if (e instanceof ApiError) throw e;
        throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status);
      }
    } else {
      throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status);
    }
  }

  const contentType = response.headers.get('content-type');
  if (contentType?.includes('application/json')) {
    return response.json() as Promise<T>;
  } else {
    const text = await response.text();
    return text as T;
  }
}

function buildUrl(tenantId: string, path: string): string {
  const tenant = tenantId || DEFAULT_TENANT;
  return `/tenants/${tenant}${path}`;
}

function buildHeaders(): HeadersInit {
  const headers: HeadersInit = {};
  
  if (getAuthHeader) {
    const authHeader = getAuthHeader();
    if (authHeader) {
      headers['Authorization'] = authHeader;
    }
  }
  
  return headers;
}

export const api = {
  // Entry operations
  async getEntries(tenantId: string, criteria: SearchCriteria = {}): Promise<PaginationResult<Entry>> {
    const params = new URLSearchParams();
    if (criteria.query) params.append('query', criteria.query);
    if (criteria.tag) params.append('tag', criteria.tag);
    if (criteria.category) params.append('category', criteria.category);
    if (criteria.keyword) params.append('keyword', criteria.keyword);
    if (criteria.size) params.append('size', criteria.size.toString());
    if (criteria.cursor) params.append('cursor', criteria.cursor);

    const url = buildUrl(tenantId, '/entries') + (params.toString() ? `?${params.toString()}` : '');
    const response = await fetch(url, {
      headers: buildHeaders(),
    });
    return handleResponse<PaginationResult<Entry>>(response);
  },

  async getEntry(tenantId: string, entryId: number): Promise<Entry> {
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      headers: buildHeaders(),
    });
    return handleResponse<Entry>(response);
  },

  async createEntry(tenantId: string, request: CreateEntryRequest): Promise<Entry> {
    // Convert to markdown format for API
    const markdownContent = createMarkdownWithFrontMatter(request.frontMatter, request.content);
    
    const response = await fetch(buildUrl(tenantId, '/entries'), {
      method: 'POST',
      headers: {
        'Content-Type': 'text/markdown',
        ...buildHeaders(),
      },
      body: markdownContent,
    });
    return handleResponse<Entry>(response);
  },

  async createEntryWithId(tenantId: string, entryId: number, request: CreateEntryRequest): Promise<Entry> {
    // Convert to markdown format for API
    const markdownContent = createMarkdownWithFrontMatter(request.frontMatter, request.content);
    
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      method: 'PUT',
      headers: {
        'Content-Type': 'text/markdown',
        ...buildHeaders(),
      },
      body: markdownContent,
    });
    return handleResponse<Entry>(response);
  },

  async updateEntry(tenantId: string, entryId: number, request: UpdateEntryRequest): Promise<Entry> {
    // Convert to markdown format for API
    const markdownContent = createMarkdownWithFrontMatter(request.frontMatter!, request.content!);
    
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      method: 'PUT',
      headers: {
        'Content-Type': 'text/markdown',
        ...buildHeaders(),
      },
      body: markdownContent,
    });
    return handleResponse<Entry>(response);
  },

  async updateEntrySummary(tenantId: string, entryId: number, summary: string): Promise<Entry> {
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}/summary`), {
      method: 'PUT',
      headers: {
        'Content-Type': 'text/plain',
        ...buildHeaders(),
      },
      body: summary,
    });
    return handleResponse<Entry>(response);
  },

  async deleteEntry(tenantId: string, entryId: number): Promise<void> {
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      method: 'DELETE',
      headers: buildHeaders(),
    });
    if (!response.ok) {
      await handleResponse(response);
    }
  },

  async searchEntries(tenantId: string, query: string): Promise<PaginationResult<Entry>> {
    const response = await fetch(buildUrl(tenantId, `/entries/search?query=${encodeURIComponent(query)}`), {
      headers: buildHeaders(),
    });
    return handleResponse<PaginationResult<Entry>>(response);
  },

  // Category operations
  async getCategories(tenantId: string): Promise<Category[][]> {
    const response = await fetch(buildUrl(tenantId, '/categories'), {
      headers: buildHeaders(),
    });
    return handleResponse<Category[][]>(response);
  },

  // Tag operations
  async getTags(tenantId: string): Promise<TagAndCount[]> {
    const response = await fetch(buildUrl(tenantId, '/tags'), {
      headers: buildHeaders(),
    });
    return handleResponse<TagAndCount[]>(response);
  },

  // Summary operations
  async summarize(tenantId: string, content: string): Promise<string> {
    const response = await fetch(buildUrl(tenantId, '/summary'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...buildHeaders(),
      },
      body: JSON.stringify({ content }),
    });
    const result = await handleResponse<{ summary: string }>(response);
    return result.summary;
  },

  // Template operations
  async getTemplate(): Promise<string> {
    const response = await fetch('/entries/template.md', {
      headers: buildHeaders(),
    });
    return handleResponse<string>(response);
  },

  // S3 operations
  async getPresignedUrl(tenantId: string, fileName: string): Promise<string> {
    const response = await fetch(buildUrl(tenantId, '/s3/presign'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...buildHeaders(),
      },
      body: JSON.stringify({ fileName }),
    });
    const result = await handleResponse<{ url: string }>(response);
    return result.url;
  },
};

export { ApiError };