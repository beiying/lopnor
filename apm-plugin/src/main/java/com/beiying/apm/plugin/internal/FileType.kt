package com.beiying.apm.plugin.internal

enum class FileType {
    DEFAULT,
    /**
     * class file like 'xxx.class'
     */
    CLASS,
    /**
     * java source file like 'xxx.java'
     */
    JAVA,
    /**
     * groovy source file like 'xxx.groovy'
     */
    GROOVY,
    /**
     * kotlin source file like 'kotlin'
     */
    KOTLIN,
    /**
     * jar file like 'xxx.jar'
     */
    JAR
}