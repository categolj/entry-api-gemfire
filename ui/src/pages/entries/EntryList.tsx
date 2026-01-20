import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import useSWRInfinite from 'swr/infinite';
import { useTenant } from '../../hooks';
import { api } from '../../services';
import { LoadingSpinner, ErrorAlert, Button } from '../../components/common';
import { Input } from '../../components/forms';
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
    revalidateFirstPage: true
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
    <div className="px-4 py-3 sm:px-0">
      {/* Header */}
      <div className="sm:flex sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Entries</h1>
          <p className="mt-1 text-sm text-gray-700">
            Manage blog entries for tenant: {tenant}
          </p>
        </div>
        <div className="mt-3 sm:mt-0">
          <Link to={`/console/${tenant}/entries/new`}>
            <Button>Create New Entry</Button>
          </Link>
        </div>
      </div>

      {/* Search and Filters */}
      <div className="mt-4 bg-white shadow rounded-lg p-4">
        <form onSubmit={handleSearch} className="space-y-4 sm:space-y-0 sm:flex sm:items-end sm:space-x-4">
          <div className="flex-1">
            <Input
              label="Search"
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search entries..."
              helpText="Search across title, content, categories, and tags"
            />
          </div>
          <div className="flex space-x-2">
            <Button type="submit" disabled={isLoading}>
              {isLoading ? 'Searching...' : 'Search'}
            </Button>
            <Button type="button" variant="secondary" onClick={handleClearFilters} disabled={isLoading}>
              Clear
            </Button>
          </div>
        </form>
      </div>

      {/* Error Display */}
      {error && (
        <div className="mt-4">
          <ErrorAlert message={error.message || 'An error occurred'} onDismiss={() => void mutate()} />
        </div>
      )}

      {/* Results */}
      <div className="mt-4">
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <div className="px-4 py-3 border-b border-gray-200 sm:px-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900">
              {isLoading && allEntries.length === 0 ? (
                <span className="flex items-center">
                  <span className="mr-2"><LoadingSpinner size="sm" /></span>
                  Loading...
                </span>
              ) : (
                allEntries.length > 0 ? `${allEntries.length} entries${hasMore ? '+' : ''} found` : 'No entries found'
              )}
            </h3>
          </div>
          
          {isLoading && allEntries.length === 0 ? (
            <div className="px-4 py-8 text-center">
              <LoadingSpinner size="md" />
              <p className="mt-2 text-gray-500">Loading entries...</p>
            </div>
          ) : allEntries.length === 0 ? (
            <div className="px-4 py-8 text-center">
              <p className="text-gray-500">No entries found.</p>
              <Link to={`/console/${tenant}/entries/new`} className="mt-2 inline-block">
                <Button>Create your first entry</Button>
              </Link>
            </div>
          ) : (
            <ul className="divide-y divide-gray-200">
              {allEntries.map((entry: Entry) => (
                  <li key={entry.entryId}>
                    <Link
                      to={`/console/${tenant}/entries/${entry.entryId}`}
                      className="block hover:bg-gray-50 px-4 py-3 sm:px-6"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <div className="flex items-center justify-between">
                            <p className="text-lg font-medium text-blue-600 truncate">
                              {entry.frontMatter.title}
                            </p>
                            <div className="ml-2 flex-shrink-0 flex">
                              <p className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                                ID: {entry.entryId}
                              </p>
                            </div>
                          </div>
                          {entry.frontMatter.summary && (
                            <p className="mt-2 text-sm text-gray-600">
                              {entry.frontMatter.summary}
                            </p>
                          )}
                          <div className="mt-2 sm:flex sm:justify-between">
                            <div className="sm:flex">
                              {entry.frontMatter.categories.length > 0 && (
                                <p className="flex items-center text-sm text-gray-500">
                                  <span className="font-medium">Categories:</span>
                                  <span className="ml-1">{formatCategories(entry.frontMatter.categories)}</span>
                                </p>
                              )}
                              {entry.frontMatter.tags.length > 0 && (
                                <p className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0 sm:ml-6">
                                  <span className="font-medium">Tags:</span>
                                  <span className="ml-1">{formatTags(entry.frontMatter.tags)}</span>
                                </p>
                              )}
                            </div>
                            <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                              <p>
                                Updated {formatDate(entry.updated.date)} by {entry.updated.name}
                              </p>
                            </div>
                          </div>
                        </div>
                      </div>
                    </Link>
                  </li>
                ))}
            </ul>
          )}

          {/* Load More */}
          {hasMore && allEntries.length > 0 && (
            <div className="px-6 py-6 border-t border-gray-200 text-center">
              <button
                onClick={handleLoadMore}
                disabled={isValidating}
                className="w-full max-w-xs mx-auto px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-blue-400 disabled:cursor-not-allowed transition-colors"
              >
                {isValidating ? (
                  <span className="flex items-center justify-center">
                    <LoadingSpinner size="sm" />
                    <span className="ml-2">Loading More...</span>
                  </span>
                ) : (
                  'Load More'
                )}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}