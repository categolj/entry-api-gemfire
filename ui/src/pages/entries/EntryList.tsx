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

  const { data, error, isLoading, isValidating, size, setSize, mutate } = useSWRInfinite<
    PaginationResult<Entry>,
    Error
  >(getKey, fetcher, {
    revalidateOnFocus: true,
    revalidateFirstPage: true,
    revalidateOnMount: true,
    dedupingInterval: 0,
  });

  // Flatten all entries from all pages
  const allEntries = data ? data.flatMap((page) => page.content) : [];
  const hasMore = data ? data[data.length - 1]?.hasNext ?? true : true;
  const isSearching = Boolean(currentSearchCriteria.query);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentSearchCriteria({ query: searchQuery.trim() || undefined });
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

  return (
    <div>
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-ink pb-5">
        <div>
          <h1 className="font-sans text-4xl font-bold tracking-tightest sm:text-5xl">Entries</h1>
          <p className="mt-2 text-sm text-ink-soft">
            {isSearching ? (
              <>
                Results for <span className="font-mono text-ink">“{currentSearchCriteria.query}”</span>
              </>
            ) : (
              'The manuscript ledger. Open an entry to edit it or start a new draft.'
            )}
          </p>
        </div>
        <Link to={`/console/${tenant}/entries/new`}>
          <Button>New entry</Button>
        </Link>
      </header>

      <form onSubmit={handleSearch} className="mt-6 flex flex-wrap items-end gap-3 border-b border-rule pb-4">
        <div className="w-full max-w-2xl flex-1">
          <label htmlFor="entry-search" className="label">
            Search entries
          </label>
          <input
            id="entry-search"
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Title, body text, tag…"
            className="field"
          />
        </div>
        <Button type="submit" variant="secondary">
          Search
        </Button>
        {(searchQuery || isSearching) && (
          <Button type="button" variant="ghost" onClick={handleClearFilters}>
            Clear
          </Button>
        )}
      </form>

      {error && (
        <div className="mt-6">
          <ErrorAlert message={error.message || 'An error occurred'} onDismiss={() => void mutate()} />
        </div>
      )}

      {isLoading && allEntries.length === 0 ? (
        <div className="py-16">
          <LoadingSpinner size="md" label="Loading the ledger" />
        </div>
      ) : allEntries.length === 0 ? (
        <div className="py-20 text-center">
          {isSearching ? (
            <>
              <p className="font-sans text-lg font-semibold">Nothing matched that search.</p>
              <p className="mt-1 text-sm text-ink-soft">Try a different word or clear the search.</p>
              <div className="mt-5">
                <Button variant="secondary" onClick={handleClearFilters}>
                  Clear search
                </Button>
              </div>
            </>
          ) : (
            <>
              <p className="font-sans text-lg font-semibold">The ledger is empty.</p>
              <p className="mt-1 text-sm text-ink-soft">Write the first entry for this tenant.</p>
              <div className="mt-5">
                <Link to={`/console/${tenant}/entries/new`}>
                  <Button>Write an entry</Button>
                </Link>
              </div>
            </>
          )}
        </div>
      ) : (
        <div>
          <div className="flex items-baseline justify-between border-b border-rule py-2 font-mono text-[11px] text-muted">
            <span className="w-12 shrink-0">No.</span>
            <span className="flex-1">Entry</span>
            <span className="hidden w-32 shrink-0 text-right sm:inline">Updated</span>
          </div>

          <ol>
            {allEntries.map((entry: Entry) => (
              <li key={entry.entryId}>
                <Link
                  to={`/console/${tenant}/entries/${entry.entryId}`}
                  className="ledger-row group grid grid-cols-[3rem_minmax(0,1fr)] gap-4 py-5 pl-3 sm:grid-cols-[3rem_minmax(0,1fr)_8rem] sm:pl-4"
                >
                  <span className="folio-marker" aria-hidden="true" />
                  <span className="pt-1 font-mono text-xs text-muted">
                    {String(entry.entryId).padStart(3, '0')}
                  </span>

                  <div className="min-w-0">
                    <h2 className="font-sans text-lg font-semibold leading-snug tracking-[-0.02em] decoration-pencil decoration-2 underline-offset-4 group-hover:underline">
                      {entry.frontMatter.title || 'Untitled entry'}
                    </h2>

                    {entry.frontMatter.summary && (
                      <p className="mt-1 max-w-2xl text-sm leading-relaxed text-ink-soft">
                        {entry.frontMatter.summary}
                      </p>
                    )}

                    <div className="mt-2 flex flex-wrap items-center gap-x-5 gap-y-1 meta-line">
                      {entry.frontMatter.categories.length > 0 && (
                        <span>
                          <span className="text-muted">in </span>
                          {entry.frontMatter.categories.map((category, index) => (
                            <React.Fragment key={index}>
                              {index > 0 && <span className="mx-0.5 text-muted">›</span>}
                              <span className="text-ink-soft">{category.name}</span>
                            </React.Fragment>
                          ))}
                        </span>
                      )}
                      {entry.frontMatter.tags.length > 0 && (
                        <span className="flex flex-wrap gap-x-2">
                          {entry.frontMatter.tags.map((tag, index) => (
                            <span key={index}>
                              <span className="text-pencil">#</span>
                              {tag.name}
                            </span>
                          ))}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="col-start-2 mt-2 text-left meta-line sm:col-start-3 sm:mt-0 sm:pt-1 sm:text-right">
                    <time dateTime={entry.updated.date}>{formatDate(entry.updated.date)}</time>
                    <span className="block truncate text-muted">{entry.updated.name}</span>
                  </div>
                </Link>
              </li>
            ))}
          </ol>

          {hasMore && (
            <div className="flex justify-center py-8">
              <Button variant="secondary" onClick={handleLoadMore} loading={isValidating} disabled={isValidating}>
                {isValidating ? 'Loading entries' : 'Load more entries'}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
