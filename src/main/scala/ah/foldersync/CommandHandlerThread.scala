package ah.foldersync


import java.net.Socket
import java.util.logging.Logger
import java.nio.file._
import java.nio.file.attribute._
import java.io.{ DataInputStream, DataOutputStream, BufferedInputStream }
import java.util.concurrent.TimeUnit
import Commands._
import java.util.concurrent.ConcurrentHashMap

class CommandHandlerThread(socket: Socket) extends Thread {
  @volatile private var stopped: Boolean = false
  private val logger = Logger.getLogger(classOf[CommandHandlerThread].getName)
  private val lock = new Lock

  private val in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))
  private val out = new DataOutputStream(socket.getOutputStream())
  private val pendingResponses = new ConcurrentHashMap[Int, () => Unit]

  override def run() {
    while (!stopped && !Thread.interrupted()) {
      in.readByte() match {
        case Ping() => lock.synchronized {
          out.writeByte(1)
          out.flush()
        }
        case Pong() => //ignore
        case IncommingFileList() =>
          lock.synchronized {
            handleFileLists()
          }
        case IncommingFiles() => 
          lock.synchronized {
          lock.synchronized {
            handleFileTransfers()
          }
        }
        case Response() =>
          lock.synchronized {
            val requestNum = in.readInt()
            Option(pendingResponses.remove(requestNum)) match {
              case Some(f) =>
                f()
              case None =>
                logger.severe("Got unexpected response")
                out.writeByte(6)
                out.flush()
            }
          }
        case invalid =>
          throw new RuntimeException("invalid input " + invalid)
      }
    }
  }

  def handleFileLists() {
    lock.synchronized(onCountTimes(handleFileList))
  }

  def handleFileList() {
    lock.synchronized {
      val dirName = in.readUTF()
      val fileInfos = onCountTimeRead(readFileInfo)
    }
  }

  def readFileInfo(index: Int): FileInfo =
    FileInfo(in.readUTF(), in.readBoolean, in.readLong(), in.readLong())

  def handleFileTransfers() {
    val baseDir = Paths.get(in.readUTF)

    lock.synchronized {
      onCountTimes(() => handleFileTransfer(baseDir))
    }
  }

  def handleFileTransfer(baseDir: Path) {
    lock.synchronized {
      val name = in.readUTF()
      val size = in.readLong()
      val lastModified = in.readLong()
      val targetPath = baseDir.resolve(name)
      Files.createDirectories(targetPath.getParent)
      using(Files.newOutputStream(targetPath)) { fOut =>
        val buffer = new Array[Byte]((4096L min size).toInt)
        var remaining = size
        while (remaining > 0) {
          val read = in.read(buffer, 0, (remaining min 4096L).toInt)
          fOut.write(buffer, 0, read)
          remaining -= read
        }
      }
      Files.setLastModifiedTime(
          targetPath, 
          FileTime.from(lastModified, TimeUnit.MILLISECONDS))
    }
  }

  def using[A <: java.io.Closeable, B](resource: => A)(f: A => B): B = {
    val r = resource
    try f(r)
    finally r.close()
  }

  def onCountTimes(f: () => Unit) {
    val count = in.readInt()
    for (_ <- 1 to count) f
  }

  def onCountTimeRead[A](f: Int => A) = {
    val count = in.readInt()
    for (index <- 1 to count) yield f
  }
}
