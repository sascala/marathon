package mesosphere.marathon
package state

import mesosphere.marathon.api.JsonTestHelper
import mesosphere.marathon.state.DiscoveryInfo.Port
import mesosphere.marathon.stream._
import mesosphere.marathon.test.MarathonSpec
import org.apache.mesos.{ Protos => MesosProtos }
import org.scalatest.Matchers
import play.api.libs.json.{ JsError, JsPath, Json }

class DiscoveryInfoTest extends MarathonSpec with Matchers {
  import mesosphere.marathon.api.v2.json.Formats._

  class Fixture {
    lazy val emptyDiscoveryInfo = DiscoveryInfo()

    lazy val discoveryInfoWithPort = DiscoveryInfo(
      ports = Seq(Port(name = "http", number = 80, protocol = "tcp", labels = Map("VIP_0" -> "192.168.0.1:80")))
    )
    lazy val discoveryInfoWithTwoPorts = DiscoveryInfo(
      ports = Seq(
        Port(name = "dns", number = 53, protocol = "udp"),
        Port(name = "http", number = 80, protocol = "tcp")
      )
    )
    lazy val discoveryInfoWithTwoPorts2 = DiscoveryInfo(
      ports = Seq(
        Port(name = "dnsudp", number = 53, protocol = "udp"),
        Port(name = "dnstcp", number = 53, protocol = "tcp")
      )
    )
  }

  def fixture(): Fixture = new Fixture

  test("ToProto default DiscoveryInfo") {
    val f = fixture()
    val proto = f.emptyDiscoveryInfo.toProto

    proto should be(Protos.DiscoveryInfo.getDefaultInstance)
  }

  test("ToProto with one port") {
    val f = fixture()
    val proto = f.discoveryInfoWithPort.toProto

    val portProto =
      MesosProtos.Port.newBuilder()
        .setName("http")
        .setNumber(80)
        .setProtocol("tcp")
        .setLabels(
          MesosProtos.Labels.newBuilder.addLabels(
            MesosProtos.Label.newBuilder
              .setKey("VIP_0")
              .setValue("192.168.0.1:80")))
        .build()

    proto.getPortsList.head should equal(portProto)
  }

  test("ConstructFromProto with default proto") {
    val f = fixture()

    val defaultProto = Protos.DiscoveryInfo.newBuilder.build
    val result = DiscoveryInfo.fromProto(defaultProto)
    result should equal(f.emptyDiscoveryInfo)
  }

  test("ConstructFromProto with port") {
    val f = fixture()

    val portProto =
      MesosProtos.Port.newBuilder()
        .setName("http")
        .setNumber(80)
        .setProtocol("tcp")
        .setLabels(
          MesosProtos.Labels.newBuilder.addLabels(
            MesosProtos.Label.newBuilder
              .setKey("VIP_0")
              .setValue("192.168.0.1:80")))
        .build()

    val protoWithPort = Protos.DiscoveryInfo.newBuilder
      .addAllPorts(Seq(portProto))
      .build

    val result = DiscoveryInfo.fromProto(protoWithPort)
    result should equal(f.discoveryInfoWithPort)
  }

  test("JSON Serialization round-trip emptyDiscoveryInfo") {
    val f = fixture()
    JsonTestHelper.assertSerializationRoundtripWorks(f.emptyDiscoveryInfo)
  }

  test("JSON Serialization round-trip discoveryInfoWithPort") {
    val f = fixture()
    JsonTestHelper.assertSerializationRoundtripWorks(f.discoveryInfoWithPort)
  }

  private[this] def fromJson(json: String): DiscoveryInfo = {
    Json.fromJson[DiscoveryInfo](Json.parse(json)).get
  }

  test("Read empty discovery info") {
    val json =
      """
      {
        "ports": []
      }
      """

    val readResult = fromJson(json)

    val f = fixture()
    assert(readResult == f.emptyDiscoveryInfo)
  }

  test("Read discovery info with one port") {
    val json =
      """
      {
        "ports": [
          { "name": "http", "number": 80, "protocol": "tcp", "labels": { "VIP_0": "192.168.0.1:80" } }
        ]
      }
      """

    val readResult = fromJson(json)

    val f = fixture()
    assert(readResult == f.discoveryInfoWithPort)
  }

  test("Read discovery info with two ports") {
    val json =
      """
      {
        "ports": [
          { "name": "dns", "number": 53, "protocol": "udp" },
          { "name": "http", "number": 80, "protocol": "tcp" }
        ]
      }
      """

    val readResult = fromJson(json)

    val f = fixture()
    assert(readResult == f.discoveryInfoWithTwoPorts)
  }

  test("Read discovery info with two ports with the same port number") {
    val json =
      """
      {
        "ports": [
          { "name": "dnsudp", "number": 53, "protocol": "udp" },
          { "name": "dnstcp", "number": 53, "protocol": "tcp" }
        ]
      }
      """

    val readResult = fromJson(json)

    val f = fixture()
    assert(readResult == f.discoveryInfoWithTwoPorts2)
  }

  test("Read discovery info with two ports with duplicate port/number") {
    val json =
      """
      {
        "ports": [
          { "name": "dns1", "number": 53, "protocol": "udp" },
          { "name": "dns2", "number": 53, "protocol": "udp" }
        ]
      }
      """

    val readResult = Json.fromJson[DiscoveryInfo](Json.parse(json))
    readResult should be(JsError(
      JsPath() \ "ports",
      "There may be only one port with a particular port number/protocol combination.")
    )
  }

  test("Read discovery info with two ports with duplicate name") {
    val json =
      """
      {
        "ports": [
          { "name": "dns1", "number": 53, "protocol": "udp" },
          { "name": "dns1", "number": 53, "protocol": "tcp" }
        ]
      }
      """

    val readResult = Json.fromJson[DiscoveryInfo](Json.parse(json))
    readResult should be(JsError(
      JsPath() \ "ports",
      "Port names are not unique.")
    )
  }

  test("Read discovery info with a port with an invalid protocol") {
    val json =
      """
      {
        "ports": [
          { "name": "http", "number": 80, "protocol": "foo" }
        ]
      }
      """

    val readResult = Json.fromJson[DiscoveryInfo](Json.parse(json))
    readResult should be(JsError(
      (JsPath() \ "ports")(0) \ "protocol",
      "Invalid protocol. Only 'udp' or 'tcp' are allowed.")
    )
  }

  test("Read discovery info with a port with an invalid name") {
    val json =
      """
      {
        "ports": [
          { "name": "???", "number": 80, "protocol": "tcp" }
        ]
      }
      """

    val readResult = Json.fromJson[DiscoveryInfo](Json.parse(json))
    readResult should be(JsError(
      (JsPath() \ "ports")(0) \ "name",
      s"Port name must fully match regular expression ${PortAssignment.PortNamePattern}")
    )
  }
}
