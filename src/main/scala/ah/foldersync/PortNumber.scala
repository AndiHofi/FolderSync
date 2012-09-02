package ah.foldersync

object PortNumber {
  def apply(v: String) = new PortNumber(v.toInt)
}
case class PortNumber(value: Int) {
  require(value >= 0 && value <= 65535)
}