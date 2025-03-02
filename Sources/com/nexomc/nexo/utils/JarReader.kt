package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoPlugin
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

object JarReader {
    fun checkIsLeaked(): Boolean {
        val jarEntries = NexoPlugin.jarFile?.entries() ?: return false

        while (jarEntries.hasMoreElements()) {
            val entry = jarEntries.nextElement()
            val entryName = entry.name.takeIf { it.endsWith(".class") || "/" !in it }?.substring(0, min(entry.name.length, 10)) ?: continue

            if (StringPatternMatching.calculateStringSimilarity(entryName, "DirectLeaks") > 0.8) return true
            if (StringPatternMatching.calculateStringSimilarity(entryName, "module-info") > 0.8) return true
        }
        return false
    }

    private val manifestContent: String
        get() = NexoPlugin.jarFile?.entries()?.asSequence()?.find { it.name.equals("META-INF/MANIFEST.MF") }?.let {
            NexoPlugin.jarFile!!.getInputStream(it).readAllBytes().toString(StandardCharsets.UTF_8)
        } ?: ""

    val manifestMap: Map<String, String> by lazy {
        manifestContent.lineSequence().filter { ":" in it }.associate { line ->
            val (key, value) = line.split(":", limit = 2).map { it.trim() }
            key to value
        }
    }

    object StringPatternMatching {
        fun calculateStringSimilarity(str1: String, str2: String): Double {
            val str1Length = str1.length
            val str2Length = str2.length

            if (str1Length == 0 && str2Length == 0) return 1.0

            val matchingDistance = (max(str1Length.toDouble(), str2Length.toDouble()) / 2 - 1).toInt()
            val str1Matches = BooleanArray(str1Length)
            val str2Matches = BooleanArray(str2Length)

            var matchingCount = 0
            for (i in 0..<str1Length) {
                val start = max(0.0, (i - matchingDistance).toDouble()).toInt()
                val end = min((i + matchingDistance + 1).toDouble(), str2Length.toDouble()).toInt()

                for (j in start until end) {
                    if (!str2Matches[j] && str1[i] == str2[j]) {
                        str1Matches[i] = true
                        str2Matches[j] = true
                        matchingCount++
                        break
                    }
                }
            }

            if (matchingCount == 0) return 0.0

            var transpositionCount = 0
            var k = 0
            for (i in 0..<str1Length) {
                if (!str1Matches[i]) continue
                var j = k
                while (j < str2Length) {
                    if (str2Matches[j]) {
                        k = j + 1
                        break
                    }
                    j++
                }

                if (str1[i] != str2[j]) {
                    transpositionCount++
                }
            }

            transpositionCount /= 2

            val jaroSimilarity = matchingCount.toDouble() / str1Length
            val jaroWinklerSimilarity = jaroSimilarity + (0.1 * transpositionCount * (1 - jaroSimilarity))

            return jaroWinklerSimilarity
        }
    }
}
