package am.ik.blog.e2e.page;

import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page object for the entry detail page.
 */
public class EntryDetailPage extends BasePage {

	public EntryDetailPage(Page page, String baseUrl) {
		super(page, baseUrl);
	}

	@Override
	public void waitForLoad() {
		page.locator("button:has-text('Edit')").waitFor();
	}

	/**
	 * Click the Edit button.
	 */
	public EntryFormPage clickEdit() {
		page.click("button:has-text('Edit')");
		page.waitForURL("**/console/_/entries/*/edit");
		return new EntryFormPage(page, baseUrl);
	}

	/**
	 * Click the Delete button (opens confirmation modal).
	 */
	public EntryDetailPage clickDelete() {
		page.locator("button:has-text('Delete')").first().click();
		page.locator("h3:has-text('Delete Entry')").waitFor();
		return this;
	}

	/**
	 * Confirm deletion in the modal.
	 */
	public EntryListPage confirmDelete() {
		page.locator(".fixed button:has-text('Delete')").click();
		page.waitForURL("**/console/_");
		return new EntryListPage(page, baseUrl);
	}

	/**
	 * Cancel deletion in the modal.
	 */
	public EntryDetailPage cancelDelete() {
		page.locator(".fixed button:has-text('Cancel')").click();
		return this;
	}

	/**
	 * Delete the entry (click Delete and confirm).
	 */
	public EntryListPage deleteEntry() {
		clickDelete();
		return confirmDelete();
	}

	/**
	 * Verify the entry title is displayed.
	 */
	public EntryDetailPage verifyTitle(String title) {
		assertThat(page.locator("h1")).containsText(title);
		return this;
	}

	/**
	 * Verify the entry content is displayed.
	 */
	public EntryDetailPage verifyContent(String content) {
		assertThat(page.locator("pre")).containsText(content);
		return this;
	}

	/**
	 * Verify the categories are displayed.
	 */
	public EntryDetailPage verifyCategories(String... categories) {
		String expected = String.join(" > ", categories);
		assertThat(page.locator("text=" + expected)).isVisible();
		return this;
	}

	/**
	 * Verify the tags are displayed.
	 */
	public EntryDetailPage verifyTags(String... tags) {
		String expected = String.join(", ", tags);
		assertThat(page.locator("text=" + expected)).isVisible();
		return this;
	}

	/**
	 * Verify the delete confirmation modal is displayed.
	 */
	public EntryDetailPage verifyDeleteModalDisplayed() {
		assertThat(page.locator("h3:has-text('Delete Entry')")).isVisible();
		assertThat(page.locator("text=Are you sure you want to delete this entry")).isVisible();
		return this;
	}

}
