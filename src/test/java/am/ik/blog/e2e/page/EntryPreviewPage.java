package am.ik.blog.e2e.page;

import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page object for the entry preview page (diff view before saving).
 */
public class EntryPreviewPage extends BasePage {

	public EntryPreviewPage(Page page, String baseUrl) {
		super(page, baseUrl);
	}

	@Override
	public void waitForLoad() {
		page.locator("h1:has-text('Review')").waitFor();
	}

	/**
	 * Click the Confirm & Save button.
	 */
	public EntryDetailPage confirmAndSave() {
		page.click("button:has-text('Confirm & Save')");
		page.waitForURL(url -> url.matches(".*/console/_/entries/\\d+$"));
		return new EntryDetailPage(page, baseUrl);
	}

	/**
	 * Click the Back to Edit button.
	 */
	public EntryFormPage backToEdit() {
		page.click("button:has-text('Back to Edit')");
		return new EntryFormPage(page, baseUrl);
	}

	/**
	 * Verify this is the "Review New Entry" page.
	 */
	public EntryPreviewPage verifyNewEntryPreview() {
		assertThat(page.locator("h1")).containsText("Review New Entry");
		return this;
	}

	/**
	 * Verify this is the "Review Changes" page.
	 */
	public EntryPreviewPage verifyChangesPreview() {
		assertThat(page.locator("h1")).containsText("Review Changes");
		return this;
	}

}
