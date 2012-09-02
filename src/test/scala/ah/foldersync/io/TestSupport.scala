package ah.foldersync.io

import java.io.IOException
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.nio.charset.Charset
import scala.collection.JavaConversions._

object TestSupport {
  def newTestTree = TestDir(createTestRoot.toString) / (
      TestDir("emptyDir"),
      TestDir("dir2") / (
          TestFile("file1", content = "fileContent"),
          TestFile("aaa")),
      TestDir("dir3") / (
          TestFile("file1", content = "fileContent"),
          TestDir("subDir3.1") / (
              TestFile("b"),
              TestFile("a"),
              TestDir("aa")),
          TestFile("file2")),
      TestFile("zzz"),
      TestFile(".1", content = "hidden"),
      TestDir(".dir4") / (
          TestFile("inHidden")))
          
          
  def createTestRoot = {
    Paths.get(System.getProperty("java.io.tmpdir"))
      .resolve("dirReaderTest" + (math.random * 10000.0).toInt)
  }
  
  def writeEntries(testDir: TestDir) = {
    def we(parent: Path)(e: TestEntry): Unit = e match {
      case TestDir(path, mt, entries) =>
        val p = parent.resolve(path)
        Files.createDirectory(p)
        entries foreach we(p)
      case TestFile(name, mt, content) =>
        val p = parent resolve name
        Files.write(p, Seq(content), Charset.forName("UTF-8"))
    }

    testDir match {
      case TestDir(path, mt, entries) =>
        val p = Paths.get(path)
        Files.createDirectory(p)
        entries foreach we(p)
    }
  }

  def cleanupDir(e: TestDir) = {
    require(e.path startsWith System.getProperty("java.io.tmpdir"))
    val root = Paths.get(e.path)
    Files.walkFileTree(root, new SimpleFileVisitor[Path]() {
      override def visitFile(p: Path, attr: BasicFileAttributes) = {
        Files.delete(p)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(p: Path, e: IOException) = {
        if (e != null) throw e
        Files.delete(p)
        FileVisitResult.CONTINUE
      }
    });
  }
}