package utils

import java.io.File


object ParseFunctions {
    fun parseRunfile(fileLoc: String, cutoff: Int = 0) =
        parseRunfile(File(fileLoc), cutoff)

    fun parseRunfile(file: File, cutoff: Int = 0) =
            file
                .bufferedReader()
                .readLines()
                .pmap {  line ->
                    val elements = line.split(" ")
                    val query = elements[0]
                    val id = elements[2]
                    query to id }
                .groupBy { it.first }
                .map { (query, results) ->
                    val r = results.map { it.second }
                        .run { if (cutoff > 0) take(cutoff) else this }
                    query to r
                }
                .toMap()
}