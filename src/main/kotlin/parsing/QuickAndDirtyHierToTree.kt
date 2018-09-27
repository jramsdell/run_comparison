package parsing

import java.io.File

data class QrelEnt(val path: List<String>, val name: String)

class QuickAndDirtyHierToTree(val qrelLoc: String, val outName: String = "test_tree_entity.qrels") {
    val paths = HashMap<List<String>, ArrayList<String>>()

    fun run() {
        val entities = File(qrelLoc).bufferedReader()
            .readLines()
            .asSequence()
            .map { line -> line.split(" ") }
            .map { elements -> QrelEnt(
                    path = elements[0].split("/"),
                    name = elements[2] ) }
            .toList()

        entities.forEach { entity ->
            (0 .. entity.path.size).forEach { subIndex ->
                val subKey = entity.path.subList(0, subIndex)
                if (subKey !in paths) {
                    paths[subKey] = ArrayList()
                }

                paths[subKey]!!.add(entity.name)
            }
        }

        val out = File(outName).bufferedWriter()

        paths.entries
            .filter { it.key.size > 1 }
            .sortedBy { it.key.first() }
            .forEach { (pathList, entities) ->
                println(pathList)
                val path = pathList.joinToString("/")
                entities.distinct().forEach { entity ->
                    out.write("$path 0 $entity 1\n")
                }
            }


        out.close()


    }
}
