package com.beiying.apm.plugin.asm.track

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Matcher


class TrackClassModifier {
    private static HashSet<String> exclude = new HashSet<>();
    static {
        exclude = new HashSet<>()
        exclude.add('android.support')
        exclude.add('com.sensorsdata.analytics.android.sdk')
    }

    static File modifyJar(File jarFile, File tempDir, boolean nameHex) {
        /**
         * 读取原 jar
         */
        def file = new JarFile(jarFile, false)

        /**
         * 设置输出到的 jar
         */
        def hexName = ""
        if (nameHex) {
            hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }
        //先解压jar包
        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = null
            try {
                inputStream = file.getInputStream(jarEntry)
            } catch (Exception e) {
                return null
            }
            String entryName = jarEntry.getName()
            if (entryName.endsWith(".DSA") || entryName.endsWith(".SF")) {
                //ignore
            } else {
                String className
                JarEntry jarEntry2 = new JarEntry(entryName)
                jarOutputStream.putNextEntry(jarEntry2)

                byte[] modifiedClassBytes = null
                byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
                if (entryName.endsWith(".class")) {
                    className = entryName.replace(Matcher.quoteReplacement(File.separator), ".").replace(".class", "")
                    if (isShouldModify(className)) {
                        modifiedClassBytes = modifyClass(sourceClassBytes)
                    }
                }
                if (modifiedClassBytes == null) {
                    modifiedClassBytes = sourceClassBytes
                }
                jarOutputStream.write(modifiedClassBytes)
                jarOutputStream.closeEntry()
            }
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }

    private static byte[] modifyClass(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor classVisitor = new TrackClassVisitor(classWriter) //访问类并进行修改符合特定条件的方法
        ClassReader cr = new ClassReader(srcClass)
        cr.accept(classVisitor, ClassReader.SKIP_FRAMES)
        return classWriter.toByteArray()
    }

    /**
     * 过滤一些bending不需要修改的.class文件，可以提高编译速度
     * */
    protected static boolean isShouldModify(String className) {
        Iterator<String> iterator = exclude.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.startsWith(packageName)) {
                return false
            }
        }

        if (className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')) {
            return false
        }

        return true
    }

    /**
     * 修改class文件
     * */
    static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        try {
            //首先获取class文件对应的className， 然后获取class文件字节数组，调用modifyClass方法进行修改，再将修改后的 byte数组生成class文件
            String className = path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
            byte[] modifiedClassBytes = modifyClass(sourceClassBytes)
            if (modifiedClassBytes) {
                modified = new File(tempDir, className.replace('.', '') + '.class')
                if (modified.exists()) {
                    modified.delete()
                }
                modified.createNewFile()
                new FileOutputStream(modified).write(modifiedClassBytes)
            }
        } catch (Exception e) {
            e.printStackTrace()
            modified = classFile
        }
        return modified
    }

    static String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }
}