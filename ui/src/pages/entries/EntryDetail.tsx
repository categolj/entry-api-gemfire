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

  const {
    data: entry,
    loading,
    error,
    refetch,
  } = useApi(
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
        navigate(`/console/${tenant}`);
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

  const formatCategories = (categories: { name: string }[]) => {
    return categories.map(c => c.name).join(' > ');
  };

  const formatTags = (tags: { name: string }[]) => {
    return tags.map(t => t.name).join(', ');
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="px-4 py-3 sm:px-0">
        <ErrorAlert message={error} onDismiss={() => void refetch()} />
        <div className="mt-3">
          <Link to={`/console/${tenant}`}>
            <Button variant="secondary">Back to Entries</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (!entry) {
    return (
      <div>
        <div className="text-center py-12">
          <h1 className="text-xl font-bold text-black">Entry not found</h1>
          <Link to={`/console/${tenant}/entries`} className="mt-4 inline-block">
            <Button>Back to Entries</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="lg:flex lg:items-center lg:justify-between mb-8">
        <div className="flex-1 min-w-0">
          <nav className="flex" aria-label="Breadcrumb">
            <ol className="flex items-center space-x-2 text-sm">
              <li>
                <Link to={`/console/${tenant}`} className="text-gray-500 hover:text-black">
                  Entries
                </Link>
              </li>
              <li>
                <span className="text-gray-300">/</span>
              </li>
              <li>
                <span className="text-gray-500">Entry {entry.entryId}</span>
              </li>
            </ol>
          </nav>
          <h1 className="mt-2 text-xl font-bold text-black">
            {entry.frontMatter.title}
          </h1>
        </div>
        <div className="mt-4 flex lg:mt-0 lg:ml-4 space-x-3">
          <Link to={`/console/${tenant}/entries/${entry.entryId}/edit`}>
            <Button variant="secondary">Edit</Button>
          </Link>
          <Button
            variant="danger"
            onClick={() => setShowDeleteConfirm(true)}
          >
            Delete
          </Button>
        </div>
      </div>

      {/* Entry Metadata */}
      <div className="border border-gray-200 mb-6">
        <div className="px-4 py-3 border-b border-gray-200">
          <h3 className="text-base font-medium text-black">Entry Information</h3>
        </div>
        <dl className="divide-y divide-gray-100">
          <div className="py-3 px-4 sm:grid sm:grid-cols-3 sm:gap-4">
            <dt className="text-sm text-gray-500">Entry ID</dt>
            <dd className="mt-1 text-sm text-black sm:mt-0 sm:col-span-2">{entry.entryId}</dd>
          </div>
          <div className="py-3 px-4 sm:grid sm:grid-cols-3 sm:gap-4">
            <dt className="text-sm text-gray-500">Title</dt>
            <dd className="mt-1 text-sm text-black sm:mt-0 sm:col-span-2">{entry.frontMatter.title}</dd>
          </div>
          {entry.frontMatter.summary && (
            <div className="py-3 px-4 sm:grid sm:grid-cols-3 sm:gap-4">
              <dt className="text-sm text-gray-500">Summary</dt>
              <dd className="mt-1 text-sm text-black sm:mt-0 sm:col-span-2">{entry.frontMatter.summary}</dd>
            </div>
          )}
          {entry.frontMatter.categories.length > 0 && (
            <div className="py-3 px-4 sm:grid sm:grid-cols-3 sm:gap-4">
              <dt className="text-sm text-gray-500">Categories</dt>
              <dd className="mt-1 text-sm text-black sm:mt-0 sm:col-span-2">
                {formatCategories(entry.frontMatter.categories)}
              </dd>
            </div>
          )}
          {entry.frontMatter.tags.length > 0 && (
            <div className="py-3 px-4 sm:grid sm:grid-cols-3 sm:gap-4">
              <dt className="text-sm text-gray-500">Tags</dt>
              <dd className="mt-1 text-sm text-black sm:mt-0 sm:col-span-2">
                {formatTags(entry.frontMatter.tags)}
              </dd>
            </div>
          )}
          <div className="py-3 px-4 sm:grid sm:grid-cols-3 sm:gap-4">
            <dt className="text-sm text-gray-500">Created</dt>
            <dd className="mt-1 text-sm text-black sm:mt-0 sm:col-span-2">
              {formatDate(entry.created.date)} by {entry.created.name}
            </dd>
          </div>
          <div className="py-3 px-4 sm:grid sm:grid-cols-3 sm:gap-4">
            <dt className="text-sm text-gray-500">Last Updated</dt>
            <dd className="mt-1 text-sm text-black sm:mt-0 sm:col-span-2">
              {formatDate(entry.updated.date)} by {entry.updated.name}
            </dd>
          </div>
        </dl>
      </div>

      {/* Entry Content */}
      <div className="border border-gray-200">
        <div className="px-4 py-3 border-b border-gray-200">
          <h3 className="text-base font-medium text-black">Content</h3>
        </div>
        <div className="px-4 py-4">
          <pre className="whitespace-pre-wrap text-sm text-black font-mono bg-gray-50 p-4 border border-gray-200">
            {entry.content}
          </pre>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-30 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-6 border border-gray-200 w-96 bg-white">
            <div className="text-center">
              <h3 className="text-lg font-medium text-black">Delete Entry</h3>
              <div className="mt-4 mb-6">
                <p className="text-sm text-gray-500">
                  Are you sure you want to delete this entry? This action cannot be undone.
                </p>
              </div>
              <div className="flex justify-center space-x-4">
                <Button
                  variant="secondary"
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={deleteLoading}
                >
                  Cancel
                </Button>
                <Button
                  variant="danger"
                  onClick={() => handleDelete()}
                  loading={deleteLoading}
                >
                  Delete
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}