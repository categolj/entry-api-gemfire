import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Form data structure stored in draft
 */
export interface DraftFormData {
  title: string;
  summary: string;
  categories: string[];
  tags: string[];
  content: string;
}

/**
 * Complete draft data including metadata
 */
export interface DraftData {
  formData: DraftFormData;
  entryIdInput: string;
  markdownMode: boolean;
  updateTimestamp: boolean;
  savedAt: number;
}

/**
 * Options for useDraft hook
 */
interface UseDraftOptions {
  /** Unique key for sessionStorage */
  key: string;
  /** Whether draft functionality is enabled */
  enabled?: boolean;
  /** Debounce delay in milliseconds for auto-save */
  debounceMs?: number;
}

/**
 * Return type of useDraft hook
 */
export interface UseDraftReturn {
  /** Current draft status */
  status: 'idle' | 'saved' | 'restored';
  /** Timestamp when draft was restored */
  restoredAt: number | null;
  /** Restore draft from storage */
  restore: () => DraftData | null;
  /** Save draft to storage */
  save: (data: Omit<DraftData, 'savedAt'>) => void;
  /** Clear draft from storage */
  clear: () => void;
  /** Dismiss the restored banner */
  dismiss: () => void;
}

/**
 * Custom hook for managing draft state in sessionStorage
 *
 * @param options - Configuration options for the draft
 * @returns Draft management functions and state
 */
export function useDraft(options: UseDraftOptions): UseDraftReturn {
  const { key, enabled = true, debounceMs = 1500 } = options;

  const [status, setStatus] = useState<'idle' | 'saved' | 'restored'>('idle');
  const [restoredAt, setRestoredAt] = useState<number | null>(null);

  // Refs for cleanup and debouncing
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout>>();
  const idleTimeoutRef = useRef<ReturnType<typeof setTimeout>>();
  // Flag to suppress saves immediately after clear
  const suppressSaveUntilRef = useRef<number>(0);
  // Flag to track if restore has been attempted (to prevent multiple restores)
  const hasAttemptedRestoreRef = useRef<boolean>(false);

  // Cleanup timeouts on unmount
  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
      if (idleTimeoutRef.current) {
        clearTimeout(idleTimeoutRef.current);
      }
    };
  }, []);

  // Reset restore flag when key changes
  useEffect(() => {
    hasAttemptedRestoreRef.current = false;
  }, [key]);

  /**
   * Restore draft from sessionStorage (only runs once per hook instance)
   */
  const restore = useCallback((): DraftData | null => {
    // Only attempt restore once to prevent repeated restoration
    if (hasAttemptedRestoreRef.current) return null;
    hasAttemptedRestoreRef.current = true;

    if (!enabled) return null;

    const savedDraft = sessionStorage.getItem(key);
    if (!savedDraft) return null;

    try {
      const draft = JSON.parse(savedDraft) as DraftData;
      setStatus('restored');
      setRestoredAt(draft.savedAt);
      return draft;
    } catch {
      // Invalid draft data, remove it
      sessionStorage.removeItem(key);
      return null;
    }
  }, [key, enabled]);

  /**
   * Save draft to sessionStorage with debouncing
   */
  const save = useCallback(
    (data: Omit<DraftData, 'savedAt'>) => {
      if (!enabled) return;

      // Skip save if within suppression period (after clear)
      if (Date.now() < suppressSaveUntilRef.current) return;

      // Clear previous timeouts
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
      if (idleTimeoutRef.current) {
        clearTimeout(idleTimeoutRef.current);
      }

      saveTimeoutRef.current = setTimeout(() => {
        // Check again in case clear was called during debounce
        if (Date.now() < suppressSaveUntilRef.current) return;

        const draftData: DraftData = {
          ...data,
          savedAt: Date.now(),
        };
        sessionStorage.setItem(key, JSON.stringify(draftData));

        // Show "saved" status only when not showing restored banner
        setStatus((prev) => {
          if (prev === 'restored') return prev;
          idleTimeoutRef.current = setTimeout(() => setStatus('idle'), 5000);
          return 'saved';
        });
      }, debounceMs);
    },
    [key, enabled, debounceMs]
  );

  /**
   * Clear draft from sessionStorage
   */
  const clear = useCallback(() => {
    sessionStorage.removeItem(key);
    setStatus('idle');
    setRestoredAt(null);

    // Clear pending save timeout
    if (saveTimeoutRef.current) {
      clearTimeout(saveTimeoutRef.current);
    }

    // Suppress saves for a period longer than debounce to prevent immediate re-save
    suppressSaveUntilRef.current = Date.now() + debounceMs + 500;
  }, [key, debounceMs]);

  /**
   * Dismiss the restored banner without clearing draft
   */
  const dismiss = useCallback(() => {
    setStatus('idle');
    setRestoredAt(null);
  }, []);

  return {
    status,
    restoredAt,
    restore,
    save,
    clear,
    dismiss,
  };
}
