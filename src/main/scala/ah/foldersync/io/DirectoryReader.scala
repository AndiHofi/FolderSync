package ah.foldersync.io
import java.nio.file._
import java.nio.file.attribute._
import scala.collection.mutable.{ ArrayBuffer, Buffer }
import java.io.IOException
import java.util.logging.Logger
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.FileVisitResult.SKIP_SUBTREE

object DirectoryReader {
  type ErrorHandler = (Path, IOException) => Unit
  type Entry = (Path, BasicFileAttributes)

  def readDirectory(path: Path, filter: Entry => Boolean = defaultFilter, errorHandler: ErrorHandler = defaultErrorHandler): Buffer[(Path, BasicFileAttributes)] = {

    val filtered = for {
      entry <- traverseDirectory(path, filter, errorHandler)
    } yield entry

    filtered.toBuffer.sortBy { _._1 }
  }

  def traverseDirectory(path: Path, eFilter: Entry => Boolean = defaultFilter, errorHandler: ErrorHandler = defaultErrorHandler): Traversable[(Path, BasicFileAttributes)] = {
    new scala.collection.immutable.Traversable[(Path, BasicFileAttributes)] {
      override def foreach[U](f: Entry => U): Unit = {
        Files.walkFileTree(path, java.util.EnumSet.noneOf(classOf[FileVisitOption]), 1, new SimpleFileVisitor[Path]() {

          import FileVisitResult._
          override def preVisitDirectory(dir: Path, attr: BasicFileAttributes) = {
            val e = (dir, attr)
            if ((path ne dir) && eFilter(e)) f(e)
            CONTINUE
          }

          override def visitFile(path: Path, attr: BasicFileAttributes) = {
            val e = (path, attr)
            if (eFilter(e)) f(e)
            CONTINUE
          }

          override def visitFileFailed(path: Path, e: IOException) = {
            errorHandler(path, e)
            CONTINUE
          }
        })
      }
    }
  }

  def traverseDirectoryTree(path: Path, filter: Entry => Boolean, errorHandler: ErrorHandler = defaultErrorHandler): Traversable[Entry] = {
    val eFilter = filter
    val traversable = new scala.collection.immutable.Traversable[Entry] {
      override def foreach[U](f: Entry => U): Unit = {
        trav(path, f, eFilter, errorHandler)
      }
    }
    
    traversable

  }

  def trav[U](path: Path, f: Entry => U, filter: Entry => Boolean, errorHandler: ErrorHandler): Unit = {
    readDirectory(path, filter = filter, errorHandler) foreach {
      case e @ (p, attr) if attr.isDirectory =>
        f(e)
        trav(p, f, filter, errorHandler)
      case e =>
        f(e)
    }
  }

  def readDirectoryTree(path: Path, filter: Entry => Boolean, errorHandler: ErrorHandler = defaultErrorHandler): ArrayBuffer[Entry] = {
    val result = new ArrayBuffer[Entry]()

    trav(path, {result += _}, filter, errorHandler)

    result
  }

  def defaultFilter = (e: Entry) => !e._2.isSymbolicLink

  def defaultErrorHandler = (path: Path, e: IOException) => getLogger.warning("Unable to read file " + path + ": " + e.getMessage)

  def getLogger = Logger.getLogger(getClass.getName)
}