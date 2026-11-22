import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useSWRConfig } from 'swr';
import { useTenant, useApi } from '../../hooks';
import { api } from '../../services';
import { LoadingSpinner, ErrorAlert, Button } from '../../components/common';

export function EntryDetail() {
  const { tenant } = useTenant();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { mutate } = useSWRConfig();
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const entryId = id ? parseInt(id, 10) : 0;

  const { data: entry, loading, error, refetch } = useApi(
    () => api.getEntry(tenant, entryId),
    [tenant, entryId]
  );

  const handleDelete = () => {
    if (!entry) return;

    const deleteEntry = async () => {
      setDeleteLoading(true);
      try {
        await api.deleteEntry(tenant, entry.entryId);
        // Invalidate all SWR cache to ensure entry list is refreshed
        await mutate(() => true);
        await navigate(`/console/${tenant}`);
      } catch (error) {
        console.error('Failed to delete entry:', error);
        // Error will be handled by the API layer
      } finally {
        setDeleteLoading(false);
        setShowDeleteConfirm(false);
      }
    };

    void deleteEntry();
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <LoadingSpinner size="lg" label="Loading entry" />
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <ErrorAlert message={error} onDismiss={() => void refetch()} />
        <div className="mt-4">
          <Link to={`/console/${tenant}`}>
            <Button variant="secondary">Back to entries</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (!entry) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-sans text-2xl font-bold tracking-tightest">Entry not found</h1>
        <div className="mt-5">
          <Link to={`/console/${tenant}/entries`}>
            <Button>Back to entries</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      <nav aria-label="Breadcrumb" className="meta-line">
        <Link to={`/console/${tenant}`} className="hover:text-pencil hover:underline">
          Entries
        </Link>
        <span className="mx-2 text-rule">/</span>
        <span>Entry {entry.entryId}</span>
      </nav>

      <header className="mt-3 flex flex-wrap items-start justify-between gap-6 border-b border-ink pb-6">
        <div className="min-w-0 flex-1">
          <p className="font-mono text-[11px] uppercase tracking-[0.14em] text-muted">
            Entry № {String(entry.entryId).padStart(3, '0')}
          </p>
          <h1 className="mt-2 font-sans text-3xl font-bold leading-tight tracking-tightest sm:text-4xl">
            {entry.frontMatter.title || 'Untitled entry'}
          </h1>
          {entry.frontMatter.summary && (
            <p className="mt-3 max-w-2xl text-sm leading-relaxed text-ink-soft">
              {entry.frontMatter.summary}
            </p>
          )}
        </div>
        <div className="flex gap-3">
          <Link to={`/console/${tenant}/entries/${entry.entryId}/edit`}>
            <Button>Edit entry</Button>
          </Link>
          <Button variant="danger" onClick={() => setShowDeleteConfirm(true)}>
            Delete
          </Button>
        </div>
      </header>

      <div className="mt-8 grid gap-8 lg:grid-cols-[16rem_minmax(0,1fr)]">
        <aside>
          <h2 className="label">Front matter</h2>
          <dl className="divide-y divide-rule border-y border-rule">
            {entry.frontMatter.categories.length > 0 && (
              <div className="py-3">
                <dt className="meta-line text-muted">Categories</dt>
                <dd className="mt-1 font-sans text-sm text-ink">
                  {entry.frontMatter.categories.map((category, index) => (
                    <React.Fragment key={index}>
                      {index > 0 && <span className="mx-1 text-muted">›</span>}
                      {category.name}
                    </React.Fragment>
                  ))}
                </dd>
              </div>
            )}
            {entry.frontMatter.tags.length > 0 && (
              <div className="py-3">
                <dt className="meta-line text-muted">Tags</dt>
                <dd className="mt-1 flex flex-wrap gap-x-2 font-mono text-xs text-ink">
                  {entry.frontMatter.tags.map((tag, index) => (
                    <span key={index}>
                      <span className="text-pencil">#</span>
                      {tag.name}
                    </span>
                  ))}
                </dd>
              </div>
            )}
            <div className="py-3">
              <dt className="meta-line text-muted">Created</dt>
              <dd className="mt-1 font-mono text-xs leading-relaxed text-ink">
                {formatDate(entry.created.date)}
                <span className="block text-muted">by {entry.created.name}</span>
              </dd>
            </div>
            <div className="py-3">
              <dt className="meta-line text-muted">Last updated</dt>
              <dd className="mt-1 font-mono text-xs leading-relaxed text-ink">
                {formatDate(entry.updated.date)}
                <span className="block text-muted">by {entry.updated.name}</span>
              </dd>
            </div>
          </dl>
        </aside>

        <section>
          <div className="flex items-baseline justify-between border-b border-rule pb-2">
            <h2 className="label mb-0">Markdown source</h2>
            <span className="meta-line">
              {entry.content.split('\n').length} lines
            </span>
          </div>
          <pre className="mt-4 overflow-x-auto whitespace-pre-wrap break-words border border-rule bg-sheet p-5 font-mono text-[12.5px] leading-[1.7] text-ink">
            {entry.content}
          </pre>
        </section>
      </div>

      {showDeleteConfirm && (
        <div
          className="modal-scrim fixed inset-0 z-50 flex items-start justify-center px-4 py-24"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !deleteLoading) {
              setShowDeleteConfirm(false);
            }
          }}
        >
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-entry-title"
            className="w-full max-w-md border border-rule bg-paper p-6 shadow-slip"
          >
            <h2 id="delete-entry-title" className="font-sans text-xl font-bold tracking-tightest">
              Delete this entry?
            </h2>
            <p className="mt-2 break-words font-mono text-xs leading-relaxed text-ink-soft">
              {entry.frontMatter.title || 'Untitled entry'}
            </p>
            <p className="mt-3 text-sm leading-relaxed text-ink-soft">
              The entry is removed from the API. This cannot be undone.
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={deleteLoading}
              >
                Keep entry
              </Button>
              <Button variant="danger" onClick={() => handleDelete()} loading={deleteLoading}>
                Delete entry
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
