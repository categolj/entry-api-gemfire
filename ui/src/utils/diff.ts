/**
 * Type representing a single line in a diff result.
 */
export interface DiffLine {
  type: 'unchanged' | 'added' | 'deleted';
  content: string;
  oldLine: number | null;
  newLine: number | null;
}

/**
 * Calculate diff between two text contents using LCS (Longest Common Subsequence) algorithm.
 *
 * @param originalContent - The original text content
 * @param newContent - The new text content to compare against
 * @returns Array of DiffLine objects representing the diff
 */
export function calculateDiff(originalContent: string, newContent: string): DiffLine[] {
  const originalLines = originalContent.split('\n');
  const newLines = newContent.split('\n');

  return calculateLCS(originalLines, newLines);
}

/**
 * Internal function to calculate LCS and generate diff lines.
 */
function calculateLCS(arr1: string[], arr2: string[]): DiffLine[] {
  const m = arr1.length;
  const n = arr2.length;
  const dp: number[][] = Array(m + 1)
    .fill(0)
    .map(() => Array(n + 1).fill(0) as number[]);

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (arr1[i - 1] === arr2[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1;
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
      }
    }
  }

  // Backtrack to find the diff
  const result: DiffLine[] = [];
  let i = m;
  let j = n;

  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && arr1[i - 1] === arr2[j - 1]) {
      result.unshift({ type: 'unchanged', content: arr1[i - 1], oldLine: i, newLine: j });
      i--;
      j--;
    } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
      result.unshift({ type: 'added', content: arr2[j - 1], oldLine: null, newLine: j });
      j--;
    } else if (i > 0) {
      result.unshift({ type: 'deleted', content: arr1[i - 1], oldLine: i, newLine: null });
      i--;
    }
  }

  return result;
}
