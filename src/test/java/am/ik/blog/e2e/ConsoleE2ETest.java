package am.ik.blog.e2e;

import am.ik.blog.MockConfig;
import am.ik.blog.TestcontainersConfiguration;
import am.ik.blog.e2e.page.EntryDetailPage;
import am.ik.blog.e2e.page.EntryFormPage;
import am.ik.blog.e2e.page.EntryListPage;
import am.ik.blog.e2e.page.LoginPage;
import am.ik.blog.entry.gemfire.GemfireEntryRepository;
import am.ik.blog.mockserver.MockServer;
import am.ik.blog.mockserver.MockServer.Response;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Import({ TestcontainersConfiguration.class, MockConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsoleE2ETest {

	@LocalServerPort
	int port;

	@Autowired
	GemfireEntryRepository entryRepository;

	@Autowired
	MockServer mockServer;

	static Playwright playwright;

	static Browser browser;

	Page page;

	@BeforeAll
	static void launchBrowser() {
		playwright = Playwright.create();
		browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
	}

	@AfterAll
	static void closeBrowser() {
		browser.close();
		playwright.close();
	}

	@BeforeEach
	void setUp() {
		page = browser.newPage();
		mockServer.reset();
		entryRepository.deleteAll();
	}

	@AfterEach
	void closePage() {
		page.close();
	}

	String baseUrl() {
		return "http://localhost:" + port;
	}

	LoginPage loginPage() {
		return new LoginPage(page, baseUrl());
	}

	EntryListPage login() {
		return loginPage().navigate().login("admin", "changeme");
	}

	@Test
	void loginAndViewEntryList() {
		// Navigate to login page and verify
		loginPage().navigate().verifyPageDisplayed();

		// Login and verify entry list
		loginPage().login("admin", "changeme")
			.verifyPageDisplayed()
			.verifyNoEntriesMessage()
			.verifyCreateFirstEntryButtonVisible();
	}

	@Test
	void createEntry() {
		// Login and navigate to entry list
		EntryListPage entryListPage = login();
		entryListPage.waitForLoad();

		// Create new entry with categories and tags
		EntryFormPage formPage = entryListPage.clickCreateFirstEntry();
		formPage.waitForLoad();
		formPage.verifyCreatePageDisplayed();

		EntryDetailPage detailPage = formPage.createEntry("Test Entry Title",
				"This is the **content** of my test entry.", new String[] { "Tech", "Java" },
				new String[] { "spring", "boot" });

		// Verify entry was created
		detailPage.verifyTitle("Test Entry Title")
			.verifyCategories("Tech", "Java")
			.verifyTags("spring", "boot")
			.verifyContent("This is the **content** of my test entry.");
	}

	@Test
	void editEntry() {
		// Login and create an entry first
		EntryListPage entryListPage = login();
		entryListPage.waitForLoad();

		EntryFormPage createFormPage = entryListPage.clickNewEntry();
		createFormPage.waitForLoad();

		EntryDetailPage detailPage = createFormPage.createEntry("Original Title", "Original content");
		detailPage.verifyTitle("Original Title");

		// Edit the entry
		EntryFormPage editFormPage = detailPage.clickEdit();
		editFormPage.waitForLoad();
		editFormPage.verifyEditPageDisplayed();

		EntryDetailPage updatedDetailPage = editFormPage.updateEntry("Updated Title", "Updated content with changes");

		// Verify updated data
		updatedDetailPage.verifyTitle("Updated Title").verifyContent("Updated content with changes");
	}

	@Test
	void deleteEntry() {
		// Login and create an entry first
		EntryListPage entryListPage = login();
		entryListPage.waitForLoad();

		EntryFormPage formPage = entryListPage.clickNewEntry();
		formPage.waitForLoad();

		EntryDetailPage detailPage = formPage.createEntry("Entry to Delete", "This entry will be deleted");
		detailPage.verifyTitle("Entry to Delete");

		// Delete the entry
		detailPage.clickDelete().verifyDeleteModalDisplayed();
		EntryListPage listPage = detailPage.confirmDelete();

		// Verify entry is no longer in list
		listPage.verifyPageDisplayed().verifyNoEntriesMessage();
	}

	@Test
	void loadExistingEntryInCreateMode() {
		// Login and create an entry first
		EntryListPage entryListPage = login();
		entryListPage.waitForLoad();

		EntryFormPage createFormPage = entryListPage.clickNewEntry();
		createFormPage.waitForLoad();

		// Create an entry with ID 1
		EntryDetailPage detailPage = createFormPage.createEntry("Existing Entry Title", "Existing entry content",
				new String[] { "Category1" }, new String[] { "tag1" });
		detailPage.verifyTitle("Existing Entry Title");

		// Navigate to create new entry form
		EntryFormPage newFormPage = entryListPage.clickNewEntry();
		newFormPage.waitForLoad();
		newFormPage.verifyCreatePageDisplayed();

		// Load the existing entry by ID
		newFormPage.loadExistingEntry("1");

		// Verify the form is populated with the existing entry's data
		newFormPage.verifyTitle("Existing Entry Title");
	}

	@Test
	void autoGenerateSummary() {
		// Setup OpenAI mock for summarization
		String summaryText = "This article explains Spring Boot fundamentals.";
		setupOpenAiMock(summaryText);

		// Login and navigate to create entry form
		EntryListPage entryListPage = login();
		entryListPage.waitForLoad();

		EntryFormPage formPage = entryListPage.clickNewEntry();
		formPage.waitForLoad();
		formPage.verifyCreatePageDisplayed();

		// Fill in the title and content
		formPage.fillTitle("Spring Boot Tutorial").fillContent("This is a comprehensive guide to Spring Boot.");

		// Click Auto Generate button
		formPage.clickAutoGenerateSummary();

		// Verify the summary is populated
		formPage.verifySummary(summaryText);
	}

	@Test
	void searchEntries() {
		// Login and create multiple entries
		EntryListPage entryListPage = login();
		entryListPage.waitForLoad();

		// Create first entry about Java
		EntryFormPage formPage1 = entryListPage.clickNewEntry();
		formPage1.waitForLoad();
		formPage1.createEntry("Java Programming Guide", "Learn Java basics");

		// Navigate back to entry list and create second entry about Python
		EntryFormPage formPage2 = entryListPage.clickNewEntry();
		formPage2.waitForLoad();
		formPage2.createEntry("Python Tutorial", "Learn Python basics");

		// Navigate back to entry list and create third entry about Spring
		EntryFormPage formPage3 = entryListPage.clickNewEntry();
		formPage3.waitForLoad();
		formPage3.createEntry("Spring Boot Introduction", "Getting started with Spring Boot");

		// Navigate back to entry list
		page.click("a:has-text('Entries')");
		entryListPage.waitForLoad();

		// Verify all 3 entries are displayed
		entryListPage.verifyEntryCount(3);

		// Search for "Java"
		entryListPage.search("Java");

		// Verify only Java entry is displayed
		entryListPage.verifyEntryCount(1);
		entryListPage.verifyEntryExists("Java Programming Guide");
		entryListPage.verifyEntryNotExists("Python Tutorial");
		entryListPage.verifyEntryNotExists("Spring Boot Introduction");

		// Clear search
		entryListPage.clearSearch();

		// Verify all entries are displayed again
		entryListPage.verifyEntryCount(3);
	}

	void setupOpenAiMock(String summaryText) {
		String sseResponse = """
				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"%s"},"finish_reason":null}]}

				data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

				data: [DONE]

				"""
			.formatted(summaryText);

		mockServer.POST("/v1/chat/completions",
				request -> Response.builder().status(200).contentType("text/event-stream").body(sseResponse).build());
	}

}
