package am.ik.blog.e2e.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page object for the entry create/edit form page.
 */
public class EntryFormPage extends BasePage {

	public EntryFormPage(Page page, String baseUrl) {
		super(page, baseUrl);
	}

	@Override
	public void waitForLoad() {
		page.getByLabel("Title *").waitFor();
	}

	/**
	 * Fill in the title field.
	 */
	public EntryFormPage fillTitle(String title) {
		page.getByLabel("Title *").fill(title);
		return this;
	}

	/**
	 * Verify the title field contains the expected text.
	 */
	public EntryFormPage verifyTitle(String expectedTitle) {
		assertThat(page.getByLabel("Title *")).hasValue(expectedTitle);
		return this;
	}

	/**
	 * Add a category.
	 */
	public EntryFormPage addCategory(String category) {
		page.getByLabel("Categories").fill(category);
		page.locator("button:has-text('Add')").first().click();
		return this;
	}

	/**
	 * Add multiple categories.
	 */
	public EntryFormPage addCategories(String... categories) {
		for (String category : categories) {
			addCategory(category);
		}
		return this;
	}

	/**
	 * Add a tag.
	 */
	public EntryFormPage addTag(String tag) {
		page.getByLabel("Tags").fill(tag);
		page.keyboard().press("Enter");
		return this;
	}

	/**
	 * Add multiple tags.
	 */
	public EntryFormPage addTags(String... tags) {
		for (String tag : tags) {
			addTag(tag);
		}
		return this;
	}

	/**
	 * Fill in the content using MDEditor.
	 */
	public EntryFormPage fillContent(String content) {
		Locator contentEditor = page.locator(".w-md-editor-text-input");
		contentEditor.click();
		contentEditor.fill(content);
		return this;
	}

	/**
	 * Fill in the summary field.
	 */
	public EntryFormPage fillSummary(String summary) {
		page.getByPlaceholder("Brief description of the entry").fill(summary);
		return this;
	}

	/**
	 * Get the current summary text.
	 */
	public String getSummary() {
		return page.getByPlaceholder("Brief description of the entry").inputValue();
	}

	/**
	 * Verify the summary field contains the expected text.
	 */
	public EntryFormPage verifySummary(String expectedSummary) {
		assertThat(page.getByPlaceholder("Brief description of the entry")).hasValue(expectedSummary);
		return this;
	}

	/**
	 * Fill in the Entry ID field (for loading existing entries).
	 */
	public EntryFormPage fillEntryId(String entryId) {
		page.getByLabel("Entry ID (Optional)").fill(entryId);
		return this;
	}

	/**
	 * Click the Load button to load an existing entry.
	 */
	public EntryFormPage clickLoad() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load").setExact(true)).click();
		// Wait for loading to complete (button becomes enabled again)
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load").setExact(true))
			.and(page.locator(":not([disabled])"))
			.waitFor();
		return this;
	}

	/**
	 * Click the Auto Generate button to generate summary.
	 */
	public EntryFormPage clickAutoGenerateSummary() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Auto Generate")).click();
		// Wait for summarization to complete (button becomes enabled again)
		page.locator("button:has-text('Auto Generate'):not([disabled])").waitFor();
		return this;
	}

	/**
	 * Load an existing entry by ID.
	 */
	public EntryFormPage loadExistingEntry(String entryId) {
		fillEntryId(entryId);
		return clickLoad();
	}

	/**
	 * Click the Load Template button to load the template.
	 */
	public EntryFormPage clickLoadTemplate() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load Template")).click();
		// Wait for loading to complete (button becomes enabled again)
		page.locator("button:has-text('Load Template'):not([disabled])").waitFor();
		return this;
	}

	/**
	 * Click the Preview & Create button.
	 */
	public EntryPreviewPage clickPreviewAndCreate() {
		page.locator("button:has-text('Preview & Create'):not([disabled])").waitFor();
		page.click("button:has-text('Preview & Create')");
		return new EntryPreviewPage(page, baseUrl);
	}

	/**
	 * Click the Preview & Update button.
	 */
	public EntryPreviewPage clickPreviewAndUpdate() {
		page.locator("button:has-text('Preview & Update'):not([disabled])").waitFor();
		page.click("button:has-text('Preview & Update')");
		return new EntryPreviewPage(page, baseUrl);
	}

	/**
	 * Click the Cancel button.
	 */
	public EntryListPage clickCancel() {
		page.click("button:has-text('Cancel')");
		page.waitForURL("**/console/_");
		return new EntryListPage(page, baseUrl);
	}

	/**
	 * Verify this is the create entry page.
	 */
	public EntryFormPage verifyCreatePageDisplayed() {
		assertThat(page.locator("h1")).containsText("Create New Entry");
		return this;
	}

	/**
	 * Verify this is the edit entry page.
	 */
	public EntryFormPage verifyEditPageDisplayed() {
		assertThat(page.locator("h1")).containsText("Edit Entry");
		return this;
	}

	/**
	 * Fill in all fields and submit for creation.
	 */
	public EntryDetailPage createEntry(String title, String content) {
		fillTitle(title);
		fillContent(content);
		return clickPreviewAndCreate().confirmAndSave();
	}

	/**
	 * Fill in all fields with categories/tags and submit for creation.
	 */
	public EntryDetailPage createEntry(String title, String content, String[] categories, String[] tags) {
		fillTitle(title);
		if (categories != null && categories.length > 0) {
			addCategories(categories);
		}
		if (tags != null && tags.length > 0) {
			addTags(tags);
		}
		fillContent(content);
		return clickPreviewAndCreate().confirmAndSave();
	}

	/**
	 * Update the entry with new title and content.
	 */
	public EntryDetailPage updateEntry(String title, String content) {
		fillTitle(title);
		fillContent(content);
		return clickPreviewAndUpdate().confirmAndSave();
	}

	/**
	 * Verify that an error alert is displayed.
	 */
	public EntryFormPage verifyErrorDisplayed() {
		assertThat(page.locator("[role='alert']")).isVisible();
		return this;
	}

	/**
	 * Verify that the error alert contains the expected text.
	 */
	public EntryFormPage verifyErrorContains(String expectedText) {
		assertThat(page.locator("[role='alert']")).containsText(expectedText);
		return this;
	}

	/**
	 * Verify that the error alert contains the HTTP status code.
	 */
	public EntryFormPage verifyErrorContainsStatusCode(int statusCode) {
		assertThat(page.locator("[role='alert']")).containsText("HTTP " + statusCode);
		return this;
	}

}
