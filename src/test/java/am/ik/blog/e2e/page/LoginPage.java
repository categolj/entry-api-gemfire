package am.ik.blog.e2e.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page object for the login/tenant selection page.
 */
public class LoginPage extends BasePage {

	public LoginPage(Page page, String baseUrl) {
		super(page, baseUrl);
	}

	/**
	 * Navigate to the login page.
	 */
	public LoginPage navigate() {
		page.navigate(baseUrl + "/console", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
		return this;
	}

	@Override
	public void waitForLoad() {
		page.locator("h2:has-text('Select Tenant')").waitFor();
	}

	/**
	 * Fill in the username field.
	 */
	public LoginPage fillUsername(String username) {
		page.fill("#username", username);
		return this;
	}

	/**
	 * Fill in the password field.
	 */
	public LoginPage fillPassword(String password) {
		page.fill("#password", password);
		return this;
	}

	/**
	 * Click the Default Tenant button and navigate to entry list.
	 */
	public EntryListPage loginWithDefaultTenant() {
		page.click("button:has-text('Default Tenant')");
		page.waitForURL("**/console/_");
		return new EntryListPage(page, baseUrl);
	}

	/**
	 * Perform full login with given credentials.
	 */
	public EntryListPage login(String username, String password) {
		fillUsername(username);
		fillPassword(password);
		return loginWithDefaultTenant();
	}

	/**
	 * Verify the login page is displayed.
	 */
	public LoginPage verifyPageDisplayed() {
		assertThat(page.locator("h2")).containsText("Select Tenant");
		return this;
	}

}
