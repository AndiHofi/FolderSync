package ah.foldersync

import java.util.logging.Logger
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.io.IOException
import ah.foldersync.io._
import java.io.DataOutputStream
import java.io.DataInputStream
import java.io.BufferedInputStream
import java.io.DataInput

object FileData {
  final val MAGIC_NUMBER = 436217849
  final val VERSION = 1

  private val logger = Logger.getLogger(classOf[FileData].getName)

  def parseDirectory(root: Path, alias: String) = {
    val infoView = for {
      (path, attr) <- defaultTraverse(root).view
    } yield FileInfo(
      name = root.relativize(path).toString,
      isDirectory = attr.isDirectory,
      size = attr.size(),
      lastModified = attr.lastModifiedTime().toMillis())

    val entries = infoView.toBuffer
    new FileData(root, alias, System.currentTimeMillis(), entries)
  }

  def loadMetaData(targetPath: Path): FileData = {
    val inStream = Files.newInputStream(targetPath)
    using(new DataInputStream(new BufferedInputStream(inStream))) { in =>
      checkHeader(in)
      val (rootDirName, alias, createMillis) = readGlobalInfo(in)
      val infos = readFileInfos(in)
      new FileData(Paths.get(rootDirName), alias, createMillis, infos)
    }
  }

  private def checkHeader(in: DataInput) {
    val magicNumber = in.readInt()
    val version = in.readInt()
    if (magicNumber != MAGIC_NUMBER) throw new IllegalArgumentException("Unrecognized file")
    if (version != VERSION) throw new IllegalArgumentException("Unknown metadata file version: " + version)
  }

  private def readGlobalInfo(in: DataInput) = {
    (in.readUTF(), in.readUTF(), in.readLong())
  }

  private def readFileInfos(in: DataInput) = {
    val count = in.readInt()
    val result = new collection.mutable.ArrayBuffer[FileInfo](count)
    for (_ <- 0 until count) {
      val name = in.readUTF()
      val isDirectory = in.readBoolean()
      val size = in.readLong()
      val lastModifiedMillis = in.readLong()
      result += FileInfo(name, isDirectory, size, lastModifiedMillis)
    }
    result
  }

  private def defaultTraverse(root: Path) =
    DirectoryReader.traverseDirectoryTree(root, filter = { _ => true })

}
class FileData(val rootDirectory: Path, val alias: String, val timestamp: Long, val files: Seq[FileInfo]) {

  private val defaultTraverse = DirectoryReader.traverseDirectoryTree(rootDirectory, filter = { _ => true })

  def storeMetaData(targetPath: Path) = {
    val outStream = Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    using(new DataOutputStream(outStream)) { out =>
      writeHeader(out)
      writeGlobalInfo(out)
      writeFileInfos(files, out)
    }
  }

  private def writeHeader(out: DataOutputStream) = {
    out.writeInt(FileData.MAGIC_NUMBER)
    out.writeInt(FileData.VERSION)
  }

  private def writeGlobalInfo(out: DataOutputStream) = {
    out.writeUTF(rootDirectory.toString())
    out.writeUTF(alias)
    out.writeLong(timestamp)
  }

  private def writeFileInfos(files: Seq[FileInfo], out: DataOutputStream) = {
    out.writeInt(files.length)
    for (FileInfo(name, isDirectory, size, lastModified) <- files) {
      out.writeUTF(name)
      out.writeBoolean(isDirectory)
      out.writeLong(size)
      out.writeLong(lastModified)
    }
  }
  
  override def equals(o: Any): Boolean= o match {
    case that: FileData =>
      that.rootDirectory == this.rootDirectory &&
      that.alias == this.alias &&
      that.timestamp == this.timestamp &&
      that.files == this.files
    case _ => false
  }
}