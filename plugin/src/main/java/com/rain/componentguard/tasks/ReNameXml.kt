package com.rain.componentguard.tasks

import com.rain.componentguard.utils.LogProguard
import com.rain.componentguard.entensions.GuardExtension
import com.rain.componentguard.utils.StringUtil
import com.rain.componentguard.utils.allDependencyAndroidProjects
import com.rain.componentguard.utils.javaDirs
import com.rain.componentguard.utils.manifestFile
import com.rain.componentguard.utils.resDirs
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

/**
 * User: rain
 * Date: 2023/7/26
 * Time: 22:00
 */
open class ReNameXml @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "componentproguard"
    }

    @TaskAction
    fun execute() {
        LogProguard.log("=============================")
        LogProguard.log("ReNameXml")
        LogProguard.log("=============================")


        val androidProjects = allDependencyAndroidProjects()
        var pngAndXmlFiles: MutableList<File> = mutableListOf()
        var layoutAndjavaCodes: MutableList<File> = mutableListOf()

        xmlFiles(androidProjects, pngAndXmlFiles)

        val map = HashMap<String, String>()
        xmlFilesHandler(map, pngAndXmlFiles)

        addLayoutAndjavaCode(androidProjects, layoutAndjavaCodes)
        layoutAndjavaCodesHandler(map, layoutAndjavaCodes)
    }

    private fun layoutAndjavaCodesHandler(
        map: HashMap<String, String>,
        layoutAndjavaCodes: MutableList<File>
    ) {
        layoutAndjavaCodes?.forEach {
            var content = it.readText()
            map.forEach { mapit ->
                val name = mapit.key
                val nameWithoutExtension = if (name.lastIndexOf('.') > 0) name.substring(
                    0,
                    name.lastIndexOf('.')
                ) else name

                content = content.replace(
                    Regex("@xml/${nameWithoutExtension}"),
                    "@xml/${mapit.value}"
                )

                content = content.replace(
                    Regex("R.xml.${nameWithoutExtension}"),
                    "R.xml.${mapit.value}"
                )
                it.writeText(content)
            }
        }
    }

    private fun xmlFilesHandler(
        map: HashMap<String, String>,
        pngAndXmlFiles: MutableList<File>
    ) {
        pngAndXmlFiles.forEach {
            var name = it.name

            val fileExtension =
                if (it.name.lastIndexOf('.') > 0) it.name.substring(it.name.lastIndexOf('.') + 1) else "" // 获取文件后缀名

            name = name?.let {
                name.substring(0, name.lastIndexOf('.'))
            }
            var newName = map.get(name)
            if (newName == null) {
                newName =
                    StringUtil.generateWords(2) + StringUtil.generateNonce(6 + Random.nextInt(10))
                map.put(name, newName)
            }

            //修改文件名
            it.renameTo(File(it.getParent(), newName + "." + fileExtension))
        }
    }

    private fun xmlFiles(androidProjects: List<Project>, pngAndXmlFiles: MutableList<File>) {
        androidProjects.forEach { project ->
            val resDirs = project.resDirs(variantName)
            resDirs.forEach { res ->
                if (res != null && res.path.length > 1) {
                    val xmls =
                        File(res.path + File.separator + "xml")
                    xmls.walk().filter {
                        it.isFile
                    }.forEach {
                        pngAndXmlFiles.add(it)
                    }
                }
            }
        }
    }

    private fun addLayoutAndjavaCode(
        androidProjects: List<Project>,
        layoutAndjavaCodes: MutableList<File>
    ) {
        androidProjects.forEach {
            //src目录java、kt代码
            layoutAndjavaCode(it.project, layoutAndjavaCodes)
            //Androidmainfest.xml
            layoutAndjavaCodes.add(it.project.manifestFile())


            //res目录
            val res = it.project.resDirs(variantName)
            res.forEach {
                val fileTree: FileTreeWalk = it.walk()
                fileTree
                    .filter {
                        it.isFile
                    }
                    .filter {
                        it.extension in listOf("xml")
                    }
                    .forEach {
                        layoutAndjavaCodes.add(it)
                    }
            }
        }
    }

    private fun layoutAndjavaCode(project: Project, layoutAndjavaCode: MutableList<File>) {
        var javaDirs = project.javaDirs(variantName)
        LogProguard.log("res=$javaDirs")
        javaDirs.forEach {
            if (it.exists() && it.isDirectory) {
                LogProguard.log("it:" + it.path)

                val fileTree: FileTreeWalk = it.walk()
                fileTree
                    .filter {
                        it.isFile
                    }
                    .filter {
//                        LogProguard.log("extension:${it.extension}")
                        it.extension in listOf("java", "kt", "c++", "xml")
                    }
                    .forEach {
                        layoutAndjavaCode?.add(it)
                    }
            }
        }
    }


}