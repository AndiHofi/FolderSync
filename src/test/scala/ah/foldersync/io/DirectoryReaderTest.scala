package ah.foldersync.io

import org.junit.Test
import org.testng.Assert._
import java.nio.file.Paths
import scala.collection.JavaConversions._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Path
import java.nio.file.Files
import java.nio.charset.Charset
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.io.IOException
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Before
import org.junit.After
import java.util.concurrent.atomic.AtomicInteger

class DirectoryReaderTest {
  val testTree = TestDir(createTestRoot.toString) / (
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
      TestFile(".1"),
      TestDir(".dir4") / (
          TestFile("inHidden")))

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

  @Before def createTestDirectoryTree {
    writeEntries(testTree)
  }

  @After def deleteTestDirectoryTree {
    try cleanupDir(testTree)
    catch {
      case e: Exception => e.printStackTrace
    }
  }

  @Test def iterateSingleDirectory {
    val dirs = DirectoryReader.readDirectory(Paths.get(testTree.path), filter = notHidden)
    assertFalse(dirs.isEmpty)
    
    assertEquals(dirs.length, 4)
  }

  @Test def iterateDirectoryTree {
    val readCount = new AtomicInteger(0)
    val dirs = DirectoryReader.traverseDirectoryTree(
      Paths.get(testTree.path),
      filter = {(e: (Path, BasicFileAttributes)) => 
        readCount.incrementAndGet()
        notHidden(e)})
        
    val root = Paths.get(testTree.path)
    val simplified = dirs.view map simplify(root)

    assertEquals(simplified.force, Seq(
      "dir2" -> true,
      "dir2/aaa" -> false,
      "dir2/file1" -> false,
      "dir3" -> true,
      "dir3/file1" -> false,
      "dir3/file2" -> false,
      "dir3/subDir3.1" -> true,
      "dir3/subDir3.1/a" -> false,
      "dir3/subDir3.1/aa" -> true,
      "dir3/subDir3.1/b" -> false,
      "emptyDir" -> true,
      "zzz" -> false))
      
    assertEquals(readCount.get, 14)
    
    val onlyFiles = dirs.view filter {!_._2.isDirectory} map simplify(root)
      
    assertEquals(readCount.get, 14)
    assertEquals(onlyFiles.force, Seq(
      "dir2/aaa" -> false,
      "dir2/file1" -> false,
      "dir3/file1" -> false,
      "dir3/file2" -> false,
      "dir3/subDir3.1/a" -> false,
      "dir3/subDir3.1/b" -> false,
      "zzz" -> false  
    ))
    assertEquals(readCount.get, 28)
  }
  
  val simplify = (root: Path) => (e: (Path, BasicFileAttributes)) => e match {
      case (path, attr) =>
        val rel = root.relativize(path)
        val isDir = attr.isDirectory
        (rel.toString, isDir)
    }
  /*
   *       TestDir("emptyDir"),
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
      TestFile(".1"),
      TestDir(".dir4") / (
          TestFile("inHidden")))
   * 
   */

  @Test def readDirectoryTree {
    val dirs = DirectoryReader.readDirectory(Paths.get(testTree.path), notHidden)

    println(dirs.length)
  }

  def createTestRoot = {
    Paths.get(System.getProperty("java.io.tmpdir"))
      .resolve("dirReaderTest" + (math.random * 10000.0).toInt)
  }

  val notHidden = (p: (Path, BasicFileAttributes)) => !(p._1.getFileName.toString startsWith ".")
}

