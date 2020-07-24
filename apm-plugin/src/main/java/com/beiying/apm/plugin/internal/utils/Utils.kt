package com.beiying.apm.plugin.internal.utils

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.Status
import com.android.build.api.transform.TransformInvocation
import com.beiying.apm.plugin.internal.AspectJClassVisitor
import com.beiying.apm.plugin.internal.FileType
import com.beiying.apm.plugin.internal.JarMerger
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.Exception
import java.util.jar.JarFile

fun File.eachFileRecurse(action: (File) -> Unit) {
    if (!isDirectory) {
        action(this)
    } else {
        listFiles()?.forEach { file ->
            if (file.isDirectory) {
                file.eachFileRecurse(action)
            } else {
                action(file)
            }
        }
    }
}

/**
 * 通过isAspectClass(FileUtils.readFileToByteArray(file))读入文件内容并转换为byte array，判断这是不是一个aspect class文件
 * */
fun isAspectClassFile(file: File): Boolean {
    if (isClassFile(file)) {
        return try {
            isAspectClass(FileUtils.readFileToByteArray(file))
        } catch (e: Exception) {
            logCore("isAspectClassFile Exception:[ ${e.message} ]")
            false
        }
    }
    return false
}

/**
 * ASM是一个Java字节码操控框架。它能被用来动态生成类或者增强既有类的功能。ClassReader,ClassWriter类都来自该框架。
 * ClassReader类可以直接由字节数组或由class文件间接的获得字节码数据，它能正确的分析字节码，构建出抽象的树在内存中表示字节码。它会调用accept方法，
 * 这个方法接受一个实现了ClassVisitor接口的对象实例作为参数，然后依次调用 ClassVisitor接口的各个方法。字节码空间上的偏移被转换成 visit 事件时间上调用的先后，
 * 所谓 visit 事件是指对各种不同 visit 函数的调用，ClassReader知道如何调用各种visit函数。在这个过程中用户无法对操作进行干涉，所以遍历的算法是确定的，
 * 用户可以做的是提供不同的Visitor来对字节码树进行不同的修改。ClassVisitor会产生一些子过程，比如visitMethod会返回一个实现MethordVisitor接口的实例，
 * visitField会返回一个实现 FieldVisitor接口的实例，完成子过程后控制返回到父过程，继续访问下一节点。因此对于ClassReader来说，其内部顺序访问是有一定要求的。
 * 实际上用户还可以不通过ClassReader类，自行手工控制这个流程，只要按照一定的顺序，各个 visit 事件被先后正确的调用，最后就能生成可以被正确加载的字节码。
 * 当然获得更大灵活性的同时也加大了调整字节码的复杂度。ClassWriter实现了ClassVisitor接口，而且含有一个toByteArray()函数，返回生成的字节码的字节流，
 * 将字节流写回文件即可生产调整后的 class 文件。一般它都作为职责链的终点，把所有visit事件的先后调用（时间上的先后），最终转换成字节码的位置的调整（空间上的前后）。
 * AspectJClassVisitor扩展自ClassVisitor类，并且重写了visitAnnotation方法.visitAnnotation方法声明：public AnnotationVisitor visitAnnotation(String desc, boolean visible)。
 * 参数desc表示被注解修饰的class的描述符，参数visible表示注解在运行时是否可见.重写的visitAnnotation会判断类前有没有“org/aspectj/lang/annotation/Aspect”注解。
 * 这也是判断是否是Aspect class的依据。如果类前包含以上注解，那么它就是一个Aspect class，那么回到FiltFilter的isAspectClassFile方法。如果这是一个Aspect class，
 * 那么我们先要把inputSourceFileStatus.isAspectChanged设置成true，标记Aspect class 发生了改变，并且设置path和subPath，subPathdirInput的路径除去所有前导目录，
 * cacheFile的路径就是aspectPath再加上subPath。接下来，根据变化的status，采取进一步操作。如果是Status.REMOVED，就删除cacheFile；如果是Status.CHANGED，首先删除cacheFile，
 * 然后调用cache(file, cacheFile)方法，生成新的cache文件；如果状态是Status.ADDED，就直接调用cache(file, cacheFile)生成新的cache文件;其它状态不处理。
 *
 * */
fun isAspectClass(bytes: ByteArray): Boolean {
    if (bytes.isEmpty()) {
        return false
    }

    try {
        val classReader = ClassReader(bytes)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
        val aspectJClassVisitor = AspectJClassVisitor(classWriter)
        classReader.accept(aspectJClassVisitor, ClassReader.EXPAND_FRAMES)
        return aspectJClassVisitor.isAspectClass
    } catch (e: Exception) {

    }

    return false
}

private fun fileType(file: File): FileType {
    val filePath = file.absolutePath
    return when {
        filePath?.toLowerCase()!!.endsWith(".java") -> FileType.JAVA
        filePath.toLowerCase().endsWith(".class") -> FileType.CLASS
        filePath.toLowerCase().endsWith(".jar") -> FileType.JAR
        filePath.toLowerCase().endsWith(".kt") -> FileType.KOTLIN
        filePath.toLowerCase().endsWith(".groovy") -> FileType.GROOVY
        else -> FileType.DEFAULT
    }
}

fun isClassFile(file: File): Boolean {
    return fileType(file) == FileType.CLASS
}

fun isClassFile(filePath: String): Boolean {
    return filePath.toLowerCase().endsWith(".class")
}

fun cache(sourceFile: File, cacheFile: File) {
    val bytes = FileUtils.readFileToByteArray(sourceFile)
    cache(bytes, cacheFile)
}

fun cache(classBytes: ByteArray, cacheFile: File) {
    FileUtils.writeByteArrayToFile(cacheFile, classBytes)
}

fun outputFiles(transformInvocation: TransformInvocation) {
    if (transformInvocation.isIncremental) {
        outputChangeFiles(transformInvocation)
    } else {
        outputAllFiles(transformInvocation)
    }
}

fun outputAllFiles(transformInvocation: TransformInvocation) {
    transformInvocation.outputProvider.deleteAll()

    transformInvocation.inputs.forEach { input ->
        input.directoryInputs.forEach { dirInput ->
            val outputJar = transformInvocation.outputProvider.getContentLocation("output", dirInput.contentTypes, dirInput.scopes, Format.JAR)

            mergeJar(dirInput.file, outputJar)
        }

        input.jarInputs.forEach { jarInput ->
            val dest = transformInvocation.outputProvider.getContentLocation(jarInput.name
                , jarInput.contentTypes
                , jarInput.scopes
                , Format.JAR)
            FileUtils.copyFile(jarInput.file, dest)
        }
    }
}

fun outputChangeFiles(transformInvocation: TransformInvocation) {
    transformInvocation.inputs.forEach { input ->
        input.directoryInputs.forEach { dirInput ->
            if (dirInput.changedFiles.isNotEmpty()) {
                val excludeJar = transformInvocation.outputProvider.getContentLocation("exclude", dirInput.contentTypes, dirInput.scopes, Format.JAR)
                mergeJar(dirInput.file, excludeJar)
            }
        }

        input.jarInputs.forEach { jarInput ->
            val target = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
            when {
                jarInput.status == Status.REMOVED -> {
                    FileUtils.forceDelete(target)
                }

                jarInput.status == Status.CHANGED -> {
                    FileUtils.forceDelete(target)
                    FileUtils.copyFile(jarInput.file, target)
                }

                jarInput.status == Status.ADDED -> {
                    FileUtils.copyFile(jarInput.file, target)
                }

            }
        }
    }
}

fun mergeJar(sourceDir: File, targetJar: File) {
    if (!targetJar.parentFile.exists()) {
        FileUtils.forceMkdir(targetJar.parentFile)
    }

    FileUtils.deleteQuietly(targetJar)
    val jarMerger = JarMerger(targetJar)
    try {
        jarMerger.setFilter(object : JarMerger.IZipEntryFilter {
            override fun checkEntry(archivePath: String): Boolean {
                return archivePath.endsWith(SdkConstants.DOT_CLASS)
            }
        })
        jarMerger.addFolder(sourceDir)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        jarMerger.close()
    }
}

fun filterJar(jarInput: JarInput, includes: List<String>, excludes: List<String>, excludeJars: List<String>): Boolean {
    if (excludeJars.isNotEmpty()) {
        val jarPath = jarInput.file.absolutePath
        return !isExcludeFilterMatched(jarPath, excludeJars)
    }
    if (includes.isEmpty() && excludes.isEmpty()) {
        return true
    } else if (includes.isEmpty()) {
        var isExclude = false
        val jarFile = JarFile(jarInput.file)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val jarEntry = entries.nextElement()
            val entryName = jarEntry.name
            val tranEntryName = entryName.replace(File.separator, ".")
            if (isExcludeFilterMatched(tranEntryName, excludes)) {
                isExclude = true
                break
            }
        }

        jarFile.close()
        return !isExclude
    } else if (excludes.isEmpty()) {
        var isInclude = false
        val jarFile = JarFile(jarInput.file)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val jarEntry = entries.nextElement()
            val entryName = jarEntry.name
            val tranEntryName = entryName.replace(File.separator, ".")
            if (isIncludeFilterMatched(tranEntryName, includes)) {
                isInclude = true
                break
            }
        }

        jarFile.close()
        return isInclude
    } else {
        var isIncludeMatched = false
        var isExcludeMatched = false
        val jarFile = JarFile(jarInput.file)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val jarEntry = entries.nextElement()
            val entryName = jarEntry.name
            val tranEntryName = entryName.replace(File.separator, ".")
            if (isIncludeFilterMatched(tranEntryName, includes)) {
                isIncludeMatched = true
            }

            if (isExcludeFilterMatched(tranEntryName, excludes)) {
                isExcludeMatched = true
            }
        }

        jarFile.close()
        return isIncludeMatched && !isExcludeMatched
    }
}

fun isExcludeFilterMatched(str: String, filters: List<String>): Boolean {
    return isFilterMatched(str, filters, FilterPolicy.EXCLUDE)
}

fun isIncludeFilterMatched(str: String, filters: List<String>): Boolean {
    return isFilterMatched(str, filters, FilterPolicy.INCLUDE)
}

private fun isFilterMatched(str: String, filters: List<String>, filterPolicy: FilterPolicy): Boolean {

    if (filters.isEmpty()) {
        return filterPolicy == FilterPolicy.INCLUDE
    }

    filters.forEach {
        if (isContained(str, it)) {
            return true
        }
    }
    return false
}

private fun isContained(str: String, filter: String): Boolean {

    if (str.contains(filter)) {
        return true
    } else {
        if (filter.contains("/")) {
            return str.contains(filter.replace("/", File.separator))
        } else if (filter.contains("\\")) {
            return str.contains(filter.replace("\\", File.separator))
        }
    }

    return false
}

enum class FilterPolicy {
    INCLUDE,
    EXCLUDE
}

fun countOfFiles(file: File): Int {
    return if (file.isFile) {
        1
    } else {
        val files = file.listFiles()
        var total = 0
        files?.forEach {
            total += countOfFiles(it)
        }

        total
    }
}