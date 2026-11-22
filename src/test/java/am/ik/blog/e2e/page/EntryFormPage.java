package am.ik.blog.e2e.page;

import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

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
		titleField().waitFor();
	}

	private Locator titleField() {
		return page.getByLabel("Title", new Page.GetByLabelOptions().setExact(true));
	}

	private Locator summaryField() {
		return page.getByPlaceholder(Pattern.compile("One or two sentences"));
	}

	/**
	 * Fill in the title field.
	 */
	public EntryFormPage fillTitle(String title) {
		titleField().fill(title);
		return this;
	}

	/**
	 * Verify the title field contains the expected text.
	 */
	public EntryFormPage verifyTitle(String expectedTitle) {
		assertThat(titleField()).hasValue(expectedTitle);
		return this;
	}

	/**
	 * Add a category.
	 */
	public EntryFormPage addCategory(String category) {
		page.getByLabel("Categories").fill(category);
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click();
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
		summaryField().fill(summary);
		return this;
	}

	/**
	 * Get the current summary text.
	 */
	public String getSummary() {
		return summaryField().inputValue();
	}

	/**
	 * Verify the summary field contains the expected text.
	 */
	public EntryFormPage verifySummary(String expectedSummary) {
		assertThat(summaryField()).hasValue(expectedSummary);
		return this;
	}

	/**
	 * Fill in the Entry ID field (for loading existing entries).
	 */
	public EntryFormPage fillEntryId(String entryId) {
		page.getByLabel("Entry ID (optional)").fill(entryId);
		return this;
	}

	/**
	 * Click the Load entry button to load an existing entry.
	 */
	public EntryFormPage clickLoad() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load entry")).click();
		// Wait for loading to complete (button becomes enabled again)
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load entry"))
			.and(page.locator(":not([disabled])"))
			.waitFor();
		return this;
	}

	/**
	 * Click the Write with AI button to generate summary.
	 */
	public EntryFormPage clickAutoGenerateSummary() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Write with AI")).click();
		// Wait for summarization to complete (button becomes enabled again)
		page.locator("button:has-text('Write with AI'):not([disabled])").waitFor();
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
	 * Click the Load template button to load the template.
	 */
	public EntryFormPage clickLoadTemplate() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load template")).click();
		// Wait for loading to complete (button becomes enabled again)
		page.locator("button:has-text('Load template'):not([disabled])").waitFor();
		return this;
	}

	/**
	 * Click the Preview and create button.
	 */
	public EntryPreviewPage clickPreviewAndCreate() {
		page.locator("button:has-text('Preview and create'):not([disabled])").waitFor();
		page.click("button:has-text('Preview and create')");
		return new EntryPreviewPage(page, baseUrl);
	}

	/**
	 * Click the Preview and update button.
	 */
	public EntryPreviewPage clickPreviewAndUpdate() {
		page.locator("button:has-text('Preview and update'):not([disabled])").waitFor();
		page.click("button:has-text('Preview and update')");
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
		assertThat(page.locator("h1")).containsText("New entry");
		return this;
	}

	/**
	 * Verify this is the edit entry page.
	 */
	public EntryFormPage verifyEditPageDisplayed() {
		assertThat(page.locator("h1")).containsText("Edit entry");
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

	/**
	 * Simulate dropping an image file onto the MDEditor.
	 * @param imageData the image data as byte array
	 * @param fileName the file name
	 * @param mimeType the MIME type (e.g., "image/png")
	 * @return this page object for chaining
	 */
	public EntryFormPage dropImageFile(byte[] imageData, String fileName, String mimeType) {
		String base64Content = Base64.getEncoder().encodeToString(imageData);
		page.evaluate("""
				([base64, name, type]) => {
				    const byteString = atob(base64);
				    const ab = new ArrayBuffer(byteString.length);
				    const ia = new Uint8Array(ab);
				    for (let i = 0; i < byteString.length; i++) {
				        ia[i] = byteString.charCodeAt(i);
				    }
				    const file = new File([ab], name, { type: type });
				    const dataTransfer = new DataTransfer();
				    dataTransfer.items.add(file);
				    const dropEvent = new DragEvent('drop', {
				        bubbles: true,
				        cancelable: true,
				        dataTransfer: dataTransfer
				    });
				    document.querySelector('.w-md-editor').dispatchEvent(dropEvent);
				}
				""", Arrays.asList(base64Content, fileName, mimeType));
		return this;
	}

	/**
	 * Wait for the image upload to complete.
	 * @return this page object for chaining
	 */
	public EntryFormPage waitForUploadComplete() {
		// Wait for uploading indicator to disappear
		page.locator("text=Uploading image")
			.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
		return this;
	}

	/**
	 * Verify that the content contains an image markdown syntax.
	 * @return this page object for chaining
	 */
	public EntryFormPage verifyContentContainsImageMarkdown() {
		assertThat(page.locator(".w-md-editor-text-input")).hasValue(Pattern.compile("!\\[.*?\\]\\(.*?\\)"));
		return this;
	}

	/**
	 * Verify that the upload error message is displayed.
	 * @param expectedText the expected error text
	 * @return this page object for chaining
	 */
	public EntryFormPage verifyUploadError(String expectedText) {
		assertThat(page.locator("[role='alert']")).containsText(expectedText);
		return this;
	}

	/**
	 * Simulate pasting an image from clipboard onto the MDEditor.
	 * @param imageData the image data as byte array
	 * @param mimeType the MIME type (e.g., "image/png")
	 * @return this page object for chaining
	 */
	public EntryFormPage pasteImageFromClipboard(byte[] imageData, String mimeType) {
		String base64Content = Base64.getEncoder().encodeToString(imageData);
		page.evaluate("""
				([base64, type]) => {
				    const byteString = atob(base64);
				    const ab = new ArrayBuffer(byteString.length);
				    const ia = new Uint8Array(ab);
				    for (let i = 0; i < byteString.length; i++) {
				        ia[i] = byteString.charCodeAt(i);
				    }
				    const blob = new Blob([ab], { type: type });
				    const dataTransfer = new DataTransfer();
				    dataTransfer.items.add(new File([blob], '', { type: type }));
				    const pasteEvent = new ClipboardEvent('paste', {
				        bubbles: true,
				        cancelable: true,
				        clipboardData: dataTransfer
				    });
				    document.querySelector('.w-md-editor').dispatchEvent(pasteEvent);
				}
				""", Arrays.asList(base64Content, mimeType));
		return this;
	}

	/**
	 * Click the AI revise button.
	 * @return this page object for chaining
	 */
	public EntryFormPage clickAIEditingButton() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("AI revise")).click();
		return this;
	}

	/**
	 * Select the revision kind in the AI dialog.
	 * @param mode the mode to select (e.g., "PROOFREADING", "COMPLETION", "EXPANSION")
	 * @return this page object for chaining
	 */
	public EntryFormPage selectEditMode(String mode) {
		page.locator("#ai-mode-" + mode).check();
		return this;
	}

	/**
	 * Click the Run revision button in the AI dialog.
	 * @return this page object for chaining
	 */
	public EntryFormPage clickRunButton() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Run revision")).click();
		// Wait for loading to complete (Apply changes button becomes enabled)
		page.locator("button:has-text('Apply changes'):not([disabled])").waitFor();
		return this;
	}

	/**
	 * Verify that the diff dialog is displayed.
	 * @return this page object for chaining
	 */
	public EntryFormPage verifyDiffDialogDisplayed() {
		assertThat(page.getByText("Proposed changes", new Page.GetByTextOptions().setExact(true))).isVisible();
		return this;
	}

	/**
	 * Click the Apply changes button in the AI dialog.
	 * @return this page object for chaining
	 */
	public EntryFormPage clickApplyChangesButton() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply changes")).click();
		// Wait for dialog to close
		page.locator("[role='dialog']").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
		return this;
	}

	/**
	 * Click the Cancel button in the AI dialog.
	 * @return this page object for chaining
	 */
	public EntryFormPage clickEditingCancelButton() {
		page.locator("[role='dialog'] button:has-text('Cancel')").click();
		// Wait for dialog to close
		page.locator("[role='dialog']").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
		return this;
	}

	/**
	 * Verify that the content contains the expected text.
	 * @param expectedText the text to check for
	 * @return this page object for chaining
	 */
	public EntryFormPage verifyContentContains(String expectedText) {
		assertThat(page.locator(".w-md-editor-text-input")).containsText(expectedText);
		return this;
	}

	/**
	 * Get the current content from the editor.
	 * @return the content text
	 */
	public String getContent() {
		return page.locator(".w-md-editor-text-input").inputValue();
	}

}
