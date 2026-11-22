package am.ik.blog.e2e.page;

import java.util.regex.Pattern;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

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
	 * Click the Confirm and save button.
	 */
	public EntryDetailPage confirmAndSave() {
		page.getByRole(AriaRole.BUTTON,
				new Page.GetByRoleOptions().setName(Pattern.compile("Confirm and (create|update)")))
			.click();
		page.waitForURL(url -> url.matches(".*/console/_/entries/\\d+$"));
		return new EntryDetailPage(page, baseUrl);
	}

	/**
	 * Click the Back to edit button.
	 */
	public EntryFormPage backToEdit() {
		page.click("button:has-text('Back to edit')");
		return new EntryFormPage(page, baseUrl);
	}

	/**
	 * Verify this is the new entry review page.
	 */
	public EntryPreviewPage verifyNewEntryPreview() {
		assertThat(page.locator("h1")).containsText("Review the new entry");
		return this;
	}

	/**
	 * Verify this is the review changes page.
	 */
	public EntryPreviewPage verifyChangesPreview() {
		assertThat(page.locator("h1")).containsText("Review changes");
		return this;
	}

}
