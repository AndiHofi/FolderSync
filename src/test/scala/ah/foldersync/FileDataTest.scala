package ah.foldersync

import ah.foldersync.io.TestSupport._
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.nio.file.Paths
import java.nio.file.Files
import org.testng.Assert._

class FileDataTest {
  val testTree = newTestTree

  @Before def createTestTree {
    writeEntries(testTree)
  }

  @After def deleteTestTree {
    cleanupDir(testTree)
  }

  @Test def parseWriteReadCompare {
    val testFile = Files.createTempFile("FileDataTest", ".metadata")

    val original = FileData.parseDirectory(Paths.get(testTree.path), "FileDataTest.parseWriteReadCompare")
    val rootPath = Paths.get(testTree.path)
    assertEquals(original.rootDirectory, rootPath)
    assertEquals(original.alias, "FileDataTest.parseWriteReadCompare")
    assertEquals(original.files.length, 15)
    
    assertEquals(original.files(0), 
        FileInfo(".1", 
            false, 
            7, 
            Files.getLastModifiedTime(rootPath.resolve(".1")).toMillis()))
    
    
    original.storeMetaData(testFile)
    val fromFile = FileData.loadMetaData(testFile)

    assertEquals(original.rootDirectory, fromFile.rootDirectory)
    assertEquals(original.alias, fromFile.alias)
    assertEquals(original.timestamp, fromFile.timestamp)
    assertEquals(original.files, fromFile.files)
  }
}