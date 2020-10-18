package com.quakbo.presentation.ui

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxOptions
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL
import org.testcontainers.containers.DefaultRecordingFileFactory
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.util.concurrent.TimeUnit

class KBrowserWebDriverContainer(imageName: String) : BrowserWebDriverContainer<KBrowserWebDriverContainer>(imageName)

class SeleniumContainerTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Browser::class)
    internal fun `Wikipedia's Rick Astley page mentions rickrolling`(browser: Browser) {
        val container = browser.container
        container.start()

        val driver = container.webDriver
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS)

        driver.get("https://www.wikipedia.org")
        driver.findElement(By.name("search")).apply {
            sendKeys("Rick Astley")
            submit()
        }

        driver.findElement(By.linkText("rickrolling")).click()
        val foundExpectedText = driver.findElements(By.cssSelector("p")).map { it.text }.any { "meme" in it }

        assertThat(foundExpectedText, equalTo(true))

        container.stop()
    }
}

enum class Browser(val container: KBrowserWebDriverContainer) {
    FIREFOX(
        KBrowserWebDriverContainer("selenium/standalone-firefox-debug:3.141.59")
            .withCapabilities(FirefoxOptions())
            .waitingFor(Wait.forListeningPort())
            .withRecordingMode(RECORD_ALL, File("./target/webdriver-recordings"))
            .withRecordingFileFactory(DefaultRecordingFileFactory())
    ),

    CHROME(
        KBrowserWebDriverContainer("selenium/standalone-chrome-debug:3.141.59")
            .withCapabilities(ChromeOptions())
            .waitingFor(Wait.forListeningPort())
            .withRecordingMode(RECORD_ALL, File("./target/webdriver-recordings"))
            .withRecordingFileFactory(DefaultRecordingFileFactory())
    ),
    ;
}
