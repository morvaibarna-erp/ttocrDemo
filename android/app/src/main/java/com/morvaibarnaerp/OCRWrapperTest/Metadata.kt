package com.morvaibarnaerp.OCRWrapperTest

import org.tensorflow.lite.support.metadata.MetadataExtractor
import java.nio.MappedByteBuffer

object Metadata {

    fun extractNamesFromMetadata(model: MappedByteBuffer): List<String> {
        try {
            val metadataExtractor = MetadataExtractor(model)
            val inputStream = metadataExtractor.getAssociatedFile("temp_meta.txt")
            val metadata = inputStream?.bufferedReader()?.use { it.readText() } ?: return emptyList()

            val regex = Regex("'names': \\{(.*?)\\}", RegexOption.DOT_MATCHES_ALL)

            val match = regex.find(metadata)
            val namesContent = match?.groups?.get(1)?.value ?: return emptyList()

            val regex2 = Regex("\"([^\"]*)\"|'([^']*)'")
            val match2 = regex2.findAll(namesContent)
            val list = match2.map { it.groupValues[1].ifEmpty { it.groupValues[2] }}.toList()

            return list
        } catch (_: Exception) {
            return emptyList()
        }
    }
}