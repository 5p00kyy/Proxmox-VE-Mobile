package com.proxmoxmobile.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationConsistencyTest {
    @Test
    fun localizedStringFilesHaveMatchingKeysAndFormatSpecifiers() {
        val defaultStrings = readStrings("src/main/res/values/strings.xml")
        val localizedFiles = listOf(
            "src/main/res/values-de/strings.xml",
            "src/main/res/values-es/strings.xml"
        )

        localizedFiles.forEach { path ->
            val localizedStrings = readStrings(path)

            assertEquals(
                "String keys differ for $path",
                defaultStrings.keys.sorted(),
                localizedStrings.keys.sorted()
            )

            defaultStrings.forEach { (key, defaultValue) ->
                assertEquals(
                    "Format specifiers differ for '$key' in $path",
                    defaultValue.formatSpecifiers(),
                    localizedStrings.getValue(key).formatSpecifiers()
                )
            }
        }
    }

    private fun readStrings(path: String): Map<String, String> {
        val file = File(path)
        assertTrue("Missing string resource file: $path", file.exists())

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val nodes = document.getElementsByTagName("string")

        return buildMap {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val name = node.attributes.getNamedItem("name").nodeValue
                put(name, node.textContent.orEmpty())
            }
        }
    }

    private fun String.formatSpecifiers(): List<String> {
        val pattern = Regex("%(?:\\d+\\$)?[\\d.]*[a-zA-Z%]")
        return pattern.findAll(this)
            .map { it.value }
            .filterNot { it == "%%" }
            .toList()
    }
}
