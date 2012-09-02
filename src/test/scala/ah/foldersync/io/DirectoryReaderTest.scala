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

import TestSupport._

class DirectoryReaderTest {


  val testTree = newTestTree

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

  @Test def readDirectoryTree {
    val dirs = DirectoryReader.readDirectory(Paths.get(testTree.path), notHidden)

    println(dirs.length)
  }


  val notHidden = (p: (Path, BasicFileAttributes)) => !(p._1.getFileName.toString startsWith ".")
}

