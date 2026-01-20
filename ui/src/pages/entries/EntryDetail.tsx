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
      <div className="px-4 py-3 sm:px-0">
        <div className="text-center">
          <h1 className="text-xl font-bold text-gray-900">Entry not found</h1>
          <Link to={`/console/${tenant}/entries`} className="mt-3 inline-block">
            <Button>Back to Entries</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="px-4 py-3 sm:px-0">
      {/* Header */}
      <div className="lg:flex lg:items-center lg:justify-between">
        <div className="flex-1 min-w-0">
          <nav className="flex" aria-label="Breadcrumb">
            <ol className="flex items-center space-x-3">
              <li>
                <Link to={`/console/${tenant}`} className="text-gray-400 hover:text-gray-500 text-sm">
                  Entries
                </Link>
              </li>
              <li>
                <span className="text-gray-400">/</span>
              </li>
              <li>
                <span className="text-gray-500 text-sm">Entry {entry.entryId}</span>
              </li>
            </ol>
          </nav>
          <h1 className="mt-1 text-xl font-bold leading-6 text-gray-900 sm:text-2xl sm:truncate">
            {entry.frontMatter.title}
          </h1>
        </div>
        <div className="mt-3 flex lg:mt-0 lg:ml-4">
          <span className="hidden sm:block">
            <Link to={`/console/${tenant}/entries/${entry.entryId}/edit`}>
              <Button variant="secondary">Edit</Button>
            </Link>
          </span>
          <span className="ml-3">
            <Button
              variant="danger"
              onClick={() => setShowDeleteConfirm(true)}
            >
              Delete
            </Button>
          </span>
        </div>
      </div>

      {/* Entry Metadata */}
      <div className="mt-4 bg-white shadow overflow-hidden sm:rounded-lg">
        <div className="px-4 py-3 sm:px-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">Entry Information</h3>
        </div>
        <div className="border-t border-gray-200">
          <dl className="divide-y divide-gray-200">
            <div className="py-2 px-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Entry ID</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{entry.entryId}</dd>
            </div>
            <div className="py-2 px-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Title</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{entry.frontMatter.title}</dd>
            </div>
            {entry.frontMatter.summary && (
              <div className="py-2 px-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">Summary</dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{entry.frontMatter.summary}</dd>
              </div>
            )}
            {entry.frontMatter.categories.length > 0 && (
              <div className="py-2 px-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">Categories</dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  {formatCategories(entry.frontMatter.categories)}
                </dd>
              </div>
            )}
            {entry.frontMatter.tags.length > 0 && (
              <div className="py-2 px-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">Tags</dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  {formatTags(entry.frontMatter.tags)}
                </dd>
              </div>
            )}
            <div className="py-2 px-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Created</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                {formatDate(entry.created.date)} by {entry.created.name}
              </dd>
            </div>
            <div className="py-2 px-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Last Updated</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                {formatDate(entry.updated.date)} by {entry.updated.name}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      {/* Entry Content */}
      <div className="mt-4 bg-white shadow overflow-hidden sm:rounded-lg">
        <div className="px-4 py-3 sm:px-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">Content</h3>
        </div>
        <div className="border-t border-gray-200 px-4 py-3 sm:p-4">
          <pre className="whitespace-pre-wrap text-sm text-gray-900 font-mono bg-gray-50 p-3 rounded-lg">
            {entry.content}
          </pre>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
            <div className="mt-3 text-center">
              <h3 className="text-lg font-medium text-gray-900">Delete Entry</h3>
              <div className="mt-2 px-7 py-3">
                <p className="text-sm text-gray-500">
                  Are you sure you want to delete this entry? This action cannot be undone.
                </p>
              </div>
              <div className="flex justify-center space-x-4 mt-4">
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