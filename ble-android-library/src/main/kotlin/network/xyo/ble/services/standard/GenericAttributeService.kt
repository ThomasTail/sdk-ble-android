package network.xyo.ble.services.standard

import network.xyo.ble.devices.XYBluetoothDevice
import network.xyo.ble.services.Service
import java.util.*

class GenericAttributeService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return uuid
        }

    val serviceChanged = IntegerCharacteristic(this, Characteristics.ServiceChanged.uuid)

    companion object {
        val uuid: UUID = UUID.fromString("00001801-0000-1000-8000-00805F9B34FB")

        enum class Characteristics(val uuid: UUID) {
            ServiceChanged(  UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb"))
        }
    }
}