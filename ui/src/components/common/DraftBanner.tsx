/**
 * Props for DraftBanner component
 */
interface DraftBannerProps {
  /** Timestamp when draft was restored */
  restoredAt: number;
  /** Callback when user clicks Discard button */
  onDiscard: () => void;
  /** Callback when user clicks Dismiss button */
  onDismiss: () => void;
}

/**
 * Banner component to notify user about restored draft
 */
export function DraftBanner({ restoredAt, onDiscard, onDismiss }: DraftBannerProps) {
  return (
    <div className="mb-4 border border-yellow-300 bg-yellow-50 p-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <svg
            className="w-5 h-5 text-yellow-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <span className="text-sm text-yellow-800">
            Draft restored from {new Date(restoredAt).toLocaleString()}
          </span>
          <button
            type="button"
            onClick={onDiscard}
            className="text-sm text-yellow-600 hover:text-yellow-800 underline"
          >
            Discard
          </button>
        </div>
        <button
          type="button"
          onClick={onDismiss}
          className="text-yellow-600 hover:text-yellow-800"
          aria-label="Dismiss"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
      </div>
    </div>
  );
}
