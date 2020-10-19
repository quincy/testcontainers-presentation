package com.quakbo.presentation.ui

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxOptions
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL
import org.testcontainers.containers.RecordingFileFactory
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class KBrowserWebDriverContainer(imageName: String) : BrowserWebDriverContainer<KBrowserWebDriverContainer>(imageName)

private const val recordingDir = "/home/quincy/dev/presentations/testcontainers-presentation/target/webdriver-recordings"

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class FirefoxContainerTest {

    @Container
    val container = KBrowserWebDriverContainer("selenium/standalone-firefox-debug:3.141.59")
        .withCapabilities(FirefoxOptions())
        .waitingFor(Wait.forListeningPort())
        .withRecordingMode(RECORD_ALL, File(recordingDir))
        .withRecordingFileFactory(customRecordingFileFactory("Wikipedia-Rick-Astley-page-mentions-rickrolling", "firefox"))

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            with(File(recordingDir)) {
                if (!exists()) {
                    mkdir()
                }
            }
        }
    }

    @Test
    internal fun `Wikipedia Rick Astley page mentions rickrolling`() {
        runTest(container)
    }
}

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class ChromeContainerTest {

    @Container
    val container = KBrowserWebDriverContainer("selenium/standalone-chrome-debug:3.141.59")
        .withCapabilities(ChromeOptions())
        .waitingFor(Wait.forListeningPort())
        .withRecordingMode(RECORD_ALL, File(recordingDir))
        .withRecordingFileFactory(customRecordingFileFactory("Wikipedia-Rick-Astley-page-mentions-rickrolling", "chrome"))

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            with(File(recordingDir)) {
                if (!exists()) {
                    mkdir()
                }
            }
        }
    }

    @Test
    internal fun `Wikipedia Rick Astley page mentions rickrolling`() {
        runTest(container)
    }
}

private fun runTest(container: KBrowserWebDriverContainer) {
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
}

fun customRecordingFileFactory(description: String, browser: String): RecordingFileFactory {
    return RecordingFileFactory { vncRecordingDirectory, _, succeeded ->
        Paths.get(vncRecordingDirectory.absolutePath, "${if (succeeded) "PASS" else "FAIL"}-$browser-$description.flv").toFile()
    }
}