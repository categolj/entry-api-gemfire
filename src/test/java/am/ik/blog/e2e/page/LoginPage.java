package am.ik.blog.e2e.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
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
		page.navigate(baseUrl + "/", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
		return this;
	}

	@Override
	public void waitForLoad() {
		page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Open the desk")).waitFor();
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
	 * Click the default tenant button and navigate to entry list.
	 */
	public EntryListPage loginWithDefaultTenant() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open default tenant")).click();
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
		assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Open the desk"))).isVisible();
		return this;
	}

}
