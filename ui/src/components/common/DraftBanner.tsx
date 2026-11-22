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
    <div className="flex flex-wrap items-center justify-between gap-3 border border-rule border-l-2 border-l-pencil bg-sheet px-4 py-2.5">
      <p className="meta-line text-ink">
        <span className="font-medium text-pencil">Draft restored</span>
        <span className="mx-2 text-rule">|</span>
        {new Date(restoredAt).toLocaleString()}
      </p>
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={onDiscard}
          className="font-sans text-xs font-semibold text-redline underline decoration-redline/40 underline-offset-4 hover:decoration-redline"
        >
          Discard draft
        </button>
        <button
          type="button"
          onClick={onDismiss}
          className="font-mono text-xs text-muted hover:text-ink"
        >
          Keep
        </button>
      </div>
    </div>
  );
}
