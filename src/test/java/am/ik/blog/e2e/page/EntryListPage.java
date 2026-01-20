package am.ik.blog.e2e.page;

import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page object for the entry list page.
 */
public class EntryListPage extends BasePage {

	public EntryListPage(Page page, String baseUrl) {
		super(page, baseUrl);
	}

	@Override
	public void waitForLoad() {
		page.locator("h1:has-text('Entries')").waitFor();
	}

	/**
	 * Click the "Create your first entry" button when no entries exist.
	 */
	public EntryFormPage clickCreateFirstEntry() {
		page.click("a:has-text('Create your first entry')");
		page.waitForURL("**/console/_/entries/new");
		return new EntryFormPage(page, baseUrl);
	}

	/**
	 * Click the "New" link in the navigation to create a new entry.
	 */
	public EntryFormPage clickNewEntry() {
		page.click("a:has-text('New')");
		page.waitForURL("**/console/_/entries/new");
		return new EntryFormPage(page, baseUrl);
	}

	/**
	 * Click on an entry in the list by its title.
	 */
	public EntryDetailPage clickEntry(String title) {
		page.click("a:has-text('" + title + "')");
		page.waitForURL(url -> url.matches(".*/console/_/entries/\\d+$"));
		return new EntryDetailPage(page, baseUrl);
	}

	/**
	 * Verify the entry list page is displayed.
	 */
	public EntryListPage verifyPageDisplayed() {
		assertThat(page.locator("h1")).containsText("Entries");
		return this;
	}

	/**
	 * Verify the "No entries found" message is displayed.
	 */
	public EntryListPage verifyNoEntriesMessage() {
		assertThat(page.locator("text=No entries found")).isVisible();
		return this;
	}

	/**
	 * Verify the "Create your first entry" button is visible.
	 */
	public EntryListPage verifyCreateFirstEntryButtonVisible() {
		assertThat(page.locator("button:has-text('Create your first entry')")).isVisible();
		return this;
	}

	/**
	 * Verify an entry with the given title exists in the list.
	 */
	public EntryListPage verifyEntryExists(String title) {
		assertThat(page.locator("text=" + title)).isVisible();
		return this;
	}

	/**
	 * Verify an entry with the given title does not exist in the list.
	 */
	public EntryListPage verifyEntryNotExists(String title) {
		assertThat(page.locator(".entry-card:has-text('" + title + "')")).hasCount(0);
		return this;
	}

	/**
	 * Search for entries by query.
	 */
	public EntryListPage search(String query) {
		page.getByPlaceholder("Search...").fill(query);
		page.getByPlaceholder("Search...").press("Enter");
		// Wait for the search results to load
		page.waitForTimeout(500);
		return this;
	}

	/**
	 * Clear the search query.
	 */
	public EntryListPage clearSearch() {
		// Click the clear button (X icon)
		page.locator("button:near(input[placeholder='Search...'])").first().click();
		page.waitForTimeout(500);
		return this;
	}

	/**
	 * Get the number of entries displayed in the list.
	 */
	public int getEntryCount() {
		return page.locator(".entry-card").count();
	}

	/**
	 * Verify the number of entries displayed in the list.
	 */
	public EntryListPage verifyEntryCount(int expectedCount) {
		assertThat(page.locator(".entry-card")).hasCount(expectedCount);
		return this;
	}

}
