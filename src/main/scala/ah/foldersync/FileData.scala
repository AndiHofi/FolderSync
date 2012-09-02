package ah.foldersync

import java.util.logging.Logger
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.io.IOException

class FileData(val rootDirectory: Path, alias: String) {
  val logger = Logger.getLogger(classOf[FileData].getName)
	def iterateCurrentState() = {
	  val rootDirStream = Files.newDirectoryStream(rootDirectory)
	  rootDirStream.iterator
	  rootDirStream.close
	  
	  Files.walkFileTree(rootDirectory, new SimpleFileVisitor[Path] {
	    override def preVisitDirectory(path: Path, attr: BasicFileAttributes) = {
	      FileVisitResult.SKIP_SUBTREE
	    }
	    
	    override def visitFile(path: Path, attr: BasicFileAttributes) = {
	      FileVisitResult.CONTINUE
	    }
	    
	    override def visitFileFailed(path: Path, e: IOException) = {
	      logger.warning("Unable to read file " + path + ": " + e.getMessage)
	      FileVisitResult.CONTINUE;
	    }
	  })
	  
	  new scala.collection.Iterator[Path] {
	    override def hasNext(): Boolean = false
	    
	    override def next():Path =throw new NoSuchElementException
	  }
	}
}