package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.TargetManager

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class KonanProject {

    static String DEFAULT_ARTIFACT_NAME = 'main'
    static String DEFAULT_INTEROP_NAME = "stdio"

    static String HOST = TargetManager.host.userName

    File projectDir
    Path projectPath
    File konanBuildDir

    String konanHome

    File         buildFile
    File         propertiesFile
    File         settingsFile

    Set<File>    srcFiles = []
    Set<File>    defFiles = []

    List<String>  interopTasks = []
    List<String>  compilationTasks = []
    String       downloadTask = ":checkKonanCompiler"

    List<String> targets

    List<String> getBuildingTasks() { return compilationTasks + interopTasks }
    List<String> getKonanTasks()    { return getBuildingTasks() + downloadTask }

    // TODO: Must be an enum.
    static String PROGRAM = "program"
    static String LIBRARY = "library"
    static String BITCODE = "bitcode"
    static String INTEROP = "interop"

    static String DEFAULT_SRC_CONTENT = """           
            fun main(args: Array<String>) {
                println(42)
            }
            """

    static String DEFAULT_DEF_CONTENT = """
            headers = stdio.h
            """.stripIndent()

    protected KonanProject(File projectDir){
        this(projectDir, [HOST])
    }

    protected KonanProject(File projectDir, List<String> targets) {
        this.projectDir = projectDir
        this.targets = targets
        projectPath = projectDir.toPath()
        konanBuildDir = projectPath.resolve('build/konan').toFile()
        def konanHome = System.getProperty("konan.home")
        if (konanHome == null) {
            throw new IllegalStateException("konan.home isn't specified")
        }
        def konanHomeDir = new File(konanHome)
        if (!konanHomeDir.exists() || !konanHomeDir.directory) {
            throw new IllegalStateException("konan.home doesn't exist or is not a directory: $konanHomeDir.canonicalPath")
        }
        // Escape windows path separator
        this.konanHome = konanHomeDir.canonicalPath.replace('\\', '\\\\')
    }

    GradleRunner createRunner(boolean withDebug = true) {
        return GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().withDebug(withDebug)
    }

    /** Creates a subdirectory specified by the given path. */
    File createSubDir(String ... path) {
        return createSubDir(Paths.get(*path))
    }

    /** Creates a subdirectory specified by the given path. */
    File createSubDir(Path path) {
        return Files.createDirectories(projectPath.resolve(path)).toFile()
    }

    /** Creates a file with the given content in project subdirectory specified by parentDirectory. */
    File createFile(Path parentDirectory = projectPath, String fileName, String content) {
        def parent = projectPath.resolve(parentDirectory)
        Files.createDirectories(parent)
        def result = parent.resolve(fileName).toFile()
        result.createNewFile()
        result.write(content)
        return result
    }

    /** Creates a file with the given content in project subdirectory specified by parentPath. */
    File createFile(List<String> parentPath, String fileName, String content) {
        return createFile(Paths.get(*parentPath), fileName, content)
    }

    /** Creates a folder for project source files (src/main/kotlin). */
    void generateFolders() {
        createSubDir("src", "main", "kotlin")
        createSubDir("src", "main", "c_interop")
    }

    /** Generates a build.gradle file in the root project directory with the given content. */
    File generateBuildFile(String content) {
        buildFile = createFile(projectPath, "build.gradle", content)
        return buildFile
    }

    /** Generates a settings.gradle file in the root project directory with the given content. */
    File generateSettingsFile(String content) {
        settingsFile = createFile(projectPath, "settings.gradle", content)
        return settingsFile
    }

    /**
     * Generates a build.gradle file in root project directory with the default content (see below)
     * and fills the compilationTasks array.
     *
     * plugins { id 'konan' }
     *
     * konanArtifacts {
     *    program('$DEFAULT_ARTIFACT_NAME')
     * }
     */
    File generateBuildFile() {
        def result = generateBuildFile("""
            plugins { id 'konan' }
            
            konanTargets = [${targets.collect { "'$it'" }.join(", ")}]
            """.stripIndent()
        )
        compilationTasks = [":compileKonan", ":build"]
        return result
    }

    /** Generates a source file with the given name and content in the given directory and adds it into srcFiles */
    File generateSrcFile(Path parentDirectory, String fileName, String content) {
        def result = createFile(parentDirectory, fileName, content)
        srcFiles.add(result)
        return result
    }

    /** Generates a source file with the given name and content in the given directory and adds it into srcFiles */
    File generateSrcFile(List<String> parentPath, String fileName, String content) {
        return generateSrcFile(Paths.get(*parentPath), fileName, content)
    }

    /** Generates a source file with the given name and content in 'src/main/kotlin' and adds it into srcFiles */
    File generateSrcFile(String fileName, String content) {
        return generateSrcFile(["src", "main", "kotlin"], fileName, content)
    }

    /**
     * Generates a source file with the given name and default content (see below) in src/main/kotlin
     * and adds it into srcFiles.
     *
     *  fun main(args: Array<String>) {
     *      println(42)
     *  }
     */
    File generateSrcFile(String fileName) {
        return generateSrcFile(fileName, DEFAULT_SRC_CONTENT)
    }

    /** Creates a def-file with the given name and content in src/main/c_interop directory and adds it to defFiles. */
    File generateDefFile(String fileName, String content) {
        def result = createFile(["src", "main", "c_interop"], fileName, content)
        defFiles.add(result)
        return result
    }

    /**
     * Creates a def-file with the given name and the default content (see below) in src/main/c_interop directory
     * and adds it to defFiles.
     *
     *  headers = stdio.h stdlib.h string.h
     */
    File generateDefFile(String fileName = "${DEFAULT_INTEROP_NAME}.def") {
        return generateDefFile(fileName, DEFAULT_DEF_CONTENT)
    }

    /** Generates gradle.properties file with the konan.home property set. */
    File generatePropertiesFile(String konanHome) {
        propertiesFile = createFile(projectPath, "gradle.properties", "konan.home=$konanHome\n")
        return propertiesFile
    }

    /**
     * Sets the given setting of the given project extension.
     * In other words adds the following string in the build file:
     *
     *  $container.$section.$parameter $value
     */
    protected void addSetting(String container, String section, String parameter, String value) {
        buildFile.append("$container.$section.$parameter $value\n")
    }

    /**
     * Sets the given setting of the given project extension using the path of the file as a value.
     * In other words adds the following string in the build file:
     *
     *  $container.$section.$parameter ${value.canonicalPath.replace(\, \\)}
     */
    protected void addSetting(String container, String section, String parameter, File value) {
        addSetting(container, section, parameter, "'${value.canonicalPath.replace('\\', '\\\\')}'")
    }

    /** Sets the given setting of the given konanArtifact */
    void addSetting(String artifactName = DEFAULT_ARTIFACT_NAME, String parameter, String value) {
        addSetting("konanArtifacts", artifactName, parameter, value)
    }

    /** Sets the given setting of the given konanArtifact using the path of the file as a value. */
    void addSetting(String artifactName = DEFAULT_ARTIFACT_NAME, String parameter, File value) {
        addSetting("konanArtifacts", artifactName, parameter, value)
    }

    void addLibraryToArtifact(String artifactName = DEFAULT_ARTIFACT_NAME, String library = DEFAULT_INTEROP_NAME) {
        addLibraryToArtifactCustom(artifactName, "artifact '$library'")
    }

    void addLibraryToArtifactCustom(String artifactName = DEFAULT_ARTIFACT_NAME, String closureContent) {
        buildFile.append("konanArtifacts.${artifactName}.libraries { $closureContent }\n")
    }

    /** Returns the path of compileKonan... task for the default artifact. */
    String defaultCompilationTask(String target = HOST) {
        return compilationTask(DEFAULT_ARTIFACT_NAME, target)
    }

    String defaultInteropTask(String target = HOST) {
        return compilationTask(DEFAULT_INTEROP_NAME, target)
    }

    /** Returns the path of compileKonan... task for the artifact specified. */
    String compilationTask(String artifactName, String target = HOST) {
        return ":compileKonan${artifactName.capitalize()}${target.capitalize()}"
    }

    String defaultCompilationConfig() {
        return artifactConfig(DEFAULT_ARTIFACT_NAME)
    }

    String defaultInteropConfig() {
        return artifactConfig(DEFAULT_INTEROP_NAME)
    }

    String artifactConfig(String artifactName) {
        return "konanArtifacts.$artifactName"
    }

    void addCompilerArtifact(String name, String content = "", String type = PROGRAM) {
        def newTasks = targets.collect { compilationTask(name, it) } + ":compileKonan${name.capitalize()}".toString()
        if (type == INTEROP) {
            defFiles += generateDefFile("${name}.def", content)
            interopTasks += newTasks
        } else {
            srcFiles += generateSrcFile(projectPath.resolve("src/$name/kotlin"), "source.kt", content)
            compilationTasks += newTasks
        }
        buildFile.append("konanArtifacts { $type('$name') }\n")
    }

    /** Creates a project with default build and source files. */
    static KonanProject create(File projectDir, List<String> targets = [HOST]) {
        return createEmpty(projectDir, targets) { KonanProject p ->
            p.addCompilerArtifact(DEFAULT_ARTIFACT_NAME, DEFAULT_SRC_CONTENT)
        }
    }

    // TODO: Add 'type parameter'

    /** Creates a project with default build and source files. */
    static KonanProject create(File projectDir, List<String> targets = [HOST], Closure config) {
        def result = create(projectDir, targets)
        config(result)
        return result
    }

    static KonanProject createWithInterop(File projectDir, List<String> targets = [HOST]) {
        return create(projectDir, targets) { KonanProject p ->
            p.addCompilerArtifact(DEFAULT_INTEROP_NAME, DEFAULT_DEF_CONTENT, INTEROP)
            p.addLibraryToArtifact()
        }
    }

    static KonanProject createWithInterop(File projectDir, List<String> targets = [HOST], Closure config) {
        def result = createWithInterop(projectDir, targets)
        config(result)
        return result
    }

    /** Creates a project with the default build file and without any source files. */
    static KonanProject createEmpty(File projectDir, List<String> targets = [HOST]) {
        def result = new KonanProject(projectDir, targets)
        result.with {
            generateFolders()
            generateBuildFile()
            generatePropertiesFile(konanHome)
            generateSettingsFile("")
        }
        return result
    }

    /** Creates a project with the default build file and without any source files. */
    static KonanProject createEmpty(File projectDir, List<String> targets = [HOST], Closure config) {
        def result = createEmpty(projectDir, targets)
        config(result)
        return result
    }

}