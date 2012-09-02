package ah.foldersync

import java.net.{ServerSocket, Socket, InetSocketAddress}
import java.io.{IOException, InterruptedIOException}

class ConnectionListener(ownAddress: InetSocketAddress) {
  @volatile private var stopped = false
  val serverSocket = new ServerSocket()
  serverSocket.bind(ownAddress)
  
  
  
  val waitThread = new Thread {
    override def run {
      while (!stopped && !Thread.interrupted()) {
        try {
        	val newSocket = serverSocket.accept()
        	val commandHandler = new CommandHandlerThread(newSocket)
        	commandHandler.start()
        } catch {
          case e: InterruptedIOException => return
          case e: IOException => 
            System.err.println("IOException occured while waiting.")
            e.printStackTrace
          case e: SecurityException =>
            e.printStackTrace
            System.exit(-2)
        }
        
      }
    }
  }
  
  waitThread.start()

}