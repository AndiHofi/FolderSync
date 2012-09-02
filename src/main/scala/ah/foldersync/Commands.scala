package ah.foldersync

object Commands {
	abstract class Command(val code: Byte) {
	  def unapply(b: Byte) = b == code
	}
	
	object Ping extends Command(0)
	object Pong extends Command(1)
	object IncommingFileList extends Command(3)
	object IncommingFiles extends Command(4)
	object Response extends Command(5)
}