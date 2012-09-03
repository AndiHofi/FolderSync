package ah.foldersync

import org.testng.Assert._
import org.junit.Test
import java.nio.file.Paths

class DiffFinderTest {
  @Test def compareEmpty {
    checkCompare(Seq(), Seq())()
  }

  @Test def compareEqual {
    val files = createTestData(10)
    checkCompare(files, files)()
  }

  @Test def compareLeftEmpty {
    val files = createTestData(10)

    checkCompare(empty, files)(added = files)
  }

  @Test def compareRightEmpty {
    val files = createTestData(10)
    checkCompare(files, Seq())(removed = files)
  }

  @Test def compareFirstFileRemoved {
    val left = createTestData(10)
    val right = left drop 1
    checkCompare(left, right)(removed = left take 1)
  }

  @Test def compareFirstFileAdded {
    val left = createTestData(10, startAt = 2)
    val right = createTestData(1) ++ left

    checkCompare(left, right)(added = right take 1)
  }

  @Test def compareLastFileRemoved {
    val left = createTestData(3)
    val right = left take 2

    checkCompare(left, right)(removed = left drop 2)
  }

  @Test def compareLastFileAdded {
    val left = createTestData(2)
    val right = left ++ createTestData(1, startAt = 3)

    checkCompare(left, right)(added = right drop 2)
  }

  @Test def addedFileInMiddle {
    val left = createTestData(1) ++ createTestData(1, startAt = 3)
    val right = createTestData(3)

    checkCompare(left, right)(added = right drop 1 take 1)
  }

  @Test def removedFileInMiddle {
    val left = createTestData(3)
    val right = Seq(left(0), left(2))

    checkCompare(left, right)(removed = Seq(left(1)))
  }

  @Test def oneAddedOneRemovedInMiddle {
    val files = createTestData(10)
    val left = Seq(1, 2, 4, 5) map { files(_) }
    val right = Seq(1, 2, 3, 5) map { files(_) }
    
    checkCompare(left, right)(
      added = Seq(files(3)),
      removed = Seq(files(4))
    )
  }
  
  @Test def interleavingFiles {
    val files = createTestData(10)
    
    val left = Seq(1,3,5,7) map files.apply
    val right = Seq(2,4,6) map files.apply
    
    checkCompare(left, right)(added = right, removed = left)
  }
  
  @Test def lastFileModified {
    val left = createTestData(5)
    val right = left.updated(4, left(4).copy(lastModified = 1231315))
    
    checkCompare(left, right)(modified = Seq(left.last -> right.last))
  }
  
  @Test def allModified {
    val left = createTestData(100)
    val right = left map { i =>
      i.copy(lastModified = i.lastModified + 1)
    }
    
    checkCompare(left, right)(modified = left zip right)
  }

  private def checkCompare(left: Seq[FileInfo], right: Seq[FileInfo])(
    added: Seq[FileInfo] = empty,
    removed: Seq[FileInfo] = empty,
    modified: Seq[(FileInfo, FileInfo)] = empty) = {

    val diff = DiffFinder.compare(left, right)
    
    assertEquals(diff.added, added)
    assertEquals(diff.removed, removed)
    assertEquals(diff.modified, modified)
  }

  private def empty[A] = Seq[A]()

  private def createTestData(length: Int, startAt: Int = 1) = {
    for (i <- (startAt until (length + startAt)).toSeq) yield {
      val asString = i.toString
      val name = "0" * (10 - asString.length()) + asString
      FileInfo(name, false, i * 100, i)
    }
  }
}