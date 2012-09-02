package ah.foldersync

import java.io.File
import java.net._
import java.nio.file._
import io.PathUtil._

object Main extends App {
  final val DefaultPort = PortNumber(23048)
  
  val configOptions = parseConfiguration(args, Configuration())
  
  val config = validateConfiguration(configOptions)

  start(config)
  
  
  
  def parseConfiguration(args: Seq[String], conf: Configuration): Configuration = args match {
    case Seq("-ownPort", portNumber, rest@_*) =>
      parseConfiguration(rest, conf.copy(ownPort = PortNumber(portNumber)))
    case Seq("-ownHost", hostName, rest@_*) =>
      parseConfiguration(rest, conf.copy(ownHost = Some(hostName)))
    case Seq("-destPort", portNumber, rest@_*) =>
      parseConfiguration(rest, conf.copy(dstPort = PortNumber(portNumber)))
    case Seq("-destHost", hostName, rest@_*) =>
      parseConfiguration(rest, conf.copy(dstHost = Some(hostName)))
    case Seq(watchedDirs@_*) =>
      conf.copy(watchedDirectories = watchedDirs map toPath)
  }
  
  def validateConfiguration(config: Configuration) = config match {
    case Configuration(ownHost, ownPort, Some(dstHost), dstPort, paths @ Seq(_ , _@_*)) =>
      val host = ownHost.getOrElse{ InetAddress.getLocalHost.getHostName }
      if (!(paths.forall { Files.isDirectory(_) })) {
        System.err.println("All paths must be directories.")
        System.exit(-1)
      }
      FinalConfig (
    		  ownAddress = new InetSocketAddress(host, ownPort.value),
    		  dstAddress = new InetSocketAddress(dstHost, dstPort.value),
    		  watchedDirs = paths    		  
      )
      	
    case _ =>
      import System.err._
      println("Invalid arguments.")
      println("Usage: ")
      println("filesync -destHost <desthost> [-destPort port] [-ownHost host] [-ownPort port] paths*")
      System.exit(-1)
      throw new Error()
  }
  
  def start(config: FinalConfig) = {
    
  }
  
  def address(host: String, port: String) = 
    new InetSocketAddress(host, port.toInt)
}

case class MySocketAddress(hostName: String, port: PortNumber)

case class FinalConfig(ownAddress: InetSocketAddress, dstAddress: InetSocketAddress, watchedDirs: Seq[Path])

case class Configuration(
	ownHost: Option[String] = None,
	ownPort: PortNumber = Main.DefaultPort,
	dstHost: Option[String] = None,
	dstPort: PortNumber = Main.DefaultPort,
	watchedDirectories: Seq[Path] = Seq()
)