package am.ik.blog.e2e.page;

import java.util.regex.Pattern;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

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
	 * Click the "Write an entry" button when no entries exist.
	 */
	public EntryFormPage clickCreateFirstEntry() {
		page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Write an entry")).click();
		page.waitForURL("**/console/_/entries/new");
		return new EntryFormPage(page, baseUrl);
	}

	/**
	 * Click the "New entry" link in the navigation to create a new entry.
	 */
	public EntryFormPage clickNewEntry() {
		page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("New entry")).first().click();
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
	 * Verify the empty ledger message is displayed.
	 */
	public EntryListPage verifyNoEntriesMessage() {
		assertThat(page.locator("text=The ledger is empty.")).isVisible();
		return this;
	}

	/**
	 * Verify the "Write an entry" button is visible.
	 */
	public EntryListPage verifyCreateFirstEntryButtonVisible() {
		assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Write an entry"))).isVisible();
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
		assertThat(page.locator(".ledger-row:has-text('" + title + "')")).hasCount(0);
		return this;
	}

	/**
	 * Search for entries by query.
	 */
	public EntryListPage search(String query) {
		page.getByPlaceholder(Pattern.compile("Title, body text, tag")).fill(query);
		page.getByPlaceholder(Pattern.compile("Title, body text, tag")).press("Enter");
		// Wait for the search results to load
		page.waitForTimeout(500);
		return this;
	}

	/**
	 * Clear the search query.
	 */
	public EntryListPage clearSearch() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Clear").setExact(true)).click();
		page.waitForTimeout(500);
		return this;
	}

	/**
	 * Get the number of entries displayed in the list.
	 */
	public int getEntryCount() {
		return page.locator(".ledger-row").count();
	}

	/**
	 * Verify the number of entries displayed in the list.
	 */
	public EntryListPage verifyEntryCount(int expectedCount) {
		assertThat(page.locator(".ledger-row")).hasCount(expectedCount);
		return this;
	}

}
