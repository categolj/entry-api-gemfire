import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import useSWRInfinite from 'swr/infinite';
import { useTenant } from '../../hooks';
import { api } from '../../services';
import { LoadingSpinner, ErrorAlert, Button } from '../../components/common';
import { Entry, SearchCriteria, PaginationResult } from '../../types';


export function EntryList() {
  const { tenant } = useTenant();
  const [searchQuery, setSearchQuery] = useState('');
  const [currentSearchCriteria, setCurrentSearchCriteria] = useState<SearchCriteria>({});

  // SWR Infinite key generator
  const getKey = (pageIndex: number, previousPageData: PaginationResult<Entry> | null) => {
    // If there's no more data, return null
    if (previousPageData && !previousPageData.hasNext) return null;

    // First page, no cursor
    if (pageIndex === 0) {
      return [`/entries/${tenant}`, currentSearchCriteria];
    }

    // Next pages, use cursor from last item of previous page
    if (previousPageData && previousPageData.content.length > 0) {
      const lastEntry = previousPageData.content[previousPageData.content.length - 1];
      const cursor = lastEntry.updated.date;
      return [`/entries/${tenant}`, { ...currentSearchCriteria, cursor }];
    }

    return null;
  };

  // SWR fetcher
  const fetcher = async ([, criteria]: [string, SearchCriteria]) => {
    return api.getEntries(tenant, criteria);
  };

  const {
    data,
    error,
    isLoading,
    isValidating,
    size,
    setSize,
    mutate
  } = useSWRInfinite<PaginationResult<Entry>, Error>(getKey, fetcher, {
    revalidateOnFocus: true,
    revalidateFirstPage: true,
    revalidateOnMount: true,
    dedupingInterval: 0
  });

  // Flatten all entries from all pages
  const allEntries = data ? data.flatMap(page => page.content) : [];
  const hasMore = data ? data[data.length - 1]?.hasNext ?? true : true;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const newCriteria = { query: searchQuery.trim() || undefined };
    console.log('Search submitted with criteria:', newCriteria);
    setCurrentSearchCriteria(newCriteria);
    void mutate(); // Reset and refetch data
  };

  const handleClearFilters = () => {
    setSearchQuery('');
    setCurrentSearchCriteria({});
    void mutate(); // Reset and refetch data
  };

  const handleLoadMore = () => {
    void setSize(size + 1);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatCategories = (categories: { name: string }[]) => {
    return categories.map(c => c.name).join(' > ');
  };

  const formatTags = (tags: { name: string }[]) => {
    return tags.map(t => t.name).join(', ');
  };



  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-xl font-bold text-black">## Entries</h1>
        <div className="flex items-center space-x-4">
          <form onSubmit={handleSearch} className="flex items-center">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search..."
              className="px-3 py-1.5 text-sm border-b border-gray-300 bg-transparent focus:outline-none focus:border-black transition-colors w-40"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={handleClearFilters}
                className="ml-2 text-gray-400 hover:text-black"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </form>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="mb-4">
          <ErrorAlert message={error.message || 'An error occurred'} onDismiss={() => void mutate()} />
        </div>
      )}

      {/* Results */}
      {isLoading && allEntries.length === 0 ? (
        <div className="py-12 text-center">
          <LoadingSpinner size="md" />
          <p className="mt-3 text-gray-500 text-sm">Loading entries...</p>
        </div>
      ) : allEntries.length === 0 ? (
        <div className="py-12 text-center">
          <p className="text-gray-500 mb-4">No entries found.</p>
          <Link to={`/console/${tenant}/entries/new`}>
            <Button>Create your first entry</Button>
          </Link>
        </div>
      ) : (
        <div>
          {allEntries.map((entry: Entry) => (
            <Link
              key={entry.entryId}
              to={`/console/${tenant}/entries/${entry.entryId}`}
              className="entry-card block pl-4 py-4 border-b border-b-gray-100"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  {/* Title */}
                  <h2 className="text-base font-medium text-black">
                    {entry.frontMatter.title}
                  </h2>

                  {/* Summary */}
                  {entry.frontMatter.summary && (
                    <p className="mt-1 text-sm text-gray-500">
                      {entry.frontMatter.summary}
                    </p>
                  )}

                  {/* Categories & Tags */}
                  <div className="mt-2 flex flex-wrap items-center gap-3">
                    {entry.frontMatter.categories.length > 0 && (
                      <span className="inline-flex items-center text-sm text-gray-500">
                        <svg className="w-3.5 h-3.5 mr-1 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                        </svg>
                        {formatCategories(entry.frontMatter.categories)}
                      </span>
                    )}
                    {entry.frontMatter.tags.length > 0 && (
                      <span className="inline-flex items-center text-sm text-gray-500">
                        <svg className="w-3.5 h-3.5 mr-1 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                        </svg>
                        {formatTags(entry.frontMatter.tags)}
                      </span>
                    )}
                  </div>

                  {/* Metadata */}
                  <div className="mt-2 flex items-center gap-4 text-sm text-gray-400">
                    <span className="inline-flex items-center">
                      <svg className="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      {formatDate(entry.updated.date)}
                    </span>
                    <span className="inline-flex items-center">
                      <svg className="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                      {entry.updated.name}
                    </span>
                  </div>
                </div>

                {/* Entry ID */}
                <span className="flex-shrink-0 font-mono text-xs text-gray-400">
                  #{entry.entryId}
                </span>
              </div>
            </Link>
          ))}

          {/* Load More */}
          {hasMore && (
            <div className="pt-8 pb-4 flex justify-center">
              <button
                onClick={handleLoadMore}
                disabled={isValidating}
                className="group inline-flex items-center gap-2 px-6 py-2.5 text-sm font-medium text-gray-600 border border-gray-300 hover:border-black hover:text-black transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isValidating ? (
                  <>
                    <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    Loading...
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4 transition-transform group-hover:translate-y-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                    </svg>
                    Load more
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}