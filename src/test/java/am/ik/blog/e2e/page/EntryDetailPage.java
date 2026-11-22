package am.ik.blog.e2e.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

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
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit entry")).waitFor();
	}

	/**
	 * Click the Edit entry button.
	 */
	public EntryFormPage clickEdit() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit entry")).click();
		page.waitForURL("**/console/_/entries/*/edit");
		return new EntryFormPage(page, baseUrl);
	}

	/**
	 * Click the Delete button (opens confirmation modal).
	 */
	public EntryDetailPage clickDelete() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete").setExact(true)).click();
		page.locator("h2:has-text('Delete this entry?')").waitFor();
		return this;
	}

	/**
	 * Confirm deletion in the modal.
	 */
	public EntryListPage confirmDelete() {
		page.locator("[role='alertdialog'] button:has-text('Delete entry')").click();
		page.waitForURL("**/console/_");
		return new EntryListPage(page, baseUrl);
	}

	/**
	 * Cancel deletion in the modal.
	 */
	public EntryDetailPage cancelDelete() {
		page.locator("[role='alertdialog'] button:has-text('Keep entry')").click();
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
		String expected = String.join("›", categories);
		assertThat(page.locator("text=" + expected)).isVisible();
		return this;
	}

	/**
	 * Verify the tags are displayed.
	 */
	public EntryDetailPage verifyTags(String... tags) {
		for (String tag : tags) {
			assertThat(page.getByText("#" + tag, new Page.GetByTextOptions().setExact(true))).isVisible();
		}
		return this;
	}

	/**
	 * Verify the delete confirmation modal is displayed.
	 */
	public EntryDetailPage verifyDeleteModalDisplayed() {
		assertThat(page.locator("h2:has-text('Delete this entry?')")).isVisible();
		assertThat(page.locator("text=The entry is removed from the API.")).isVisible();
		return this;
	}

}
