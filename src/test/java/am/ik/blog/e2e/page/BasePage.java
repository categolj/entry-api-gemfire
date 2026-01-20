package am.ik.blog.e2e.page;

import com.microsoft.playwright.Page;

/**
 * Base class for all page objects. Provides common functionality and page reference.
 */
public abstract class BasePage {

	protected final Page page;

	protected final String baseUrl;

	protected BasePage(Page page, String baseUrl) {
		this.page = page;
		this.baseUrl = baseUrl;
	}

	/**
	 * Wait for the page to be fully loaded by checking for a characteristic element.
	 */
	public abstract void waitForLoad();

	/**
	 * Get the current URL.
	 */
	public String currentUrl() {
		return page.url();
	}

}
