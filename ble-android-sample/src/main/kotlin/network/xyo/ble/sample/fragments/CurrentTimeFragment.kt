package network.xyo.ble.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_current_time.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import network.xyo.ble.devices.XY2BluetoothDevice
import network.xyo.ble.devices.XY3BluetoothDevice
import network.xyo.ble.devices.XY4BluetoothDevice
import network.xyo.ble.devices.XYBluetoothDevice
import network.xyo.ble.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.sample.R
import network.xyo.ble.sample.XYDeviceData
import network.xyo.ui.ui

class CurrentTimeFragment : XYDeviceFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_current_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_time_refresh.setOnClickListener {
            setTimeValues()
        }
    }

    override fun onResume() {
        super.onResume()

        if (deviceData?.currentTime.isNullOrEmpty()) {
            setTimeValues()
        } else {
            updateUI()
        }

    }

    private fun updateUI() {
        ui {
            throbber?.hide()

            text_currentTime?.text = deviceData?.currentTime
            text_localTimeInformation?.text = deviceData?.localTimeInformation
            text_referenceTimeInformation?.text = deviceData?.referenceTimeInformation
        }
    }

    private fun setTimeValues() {
        throbber?.show()

        when (device) {
            is XY4BluetoothDevice -> {
                val x4 = (device as? XY4BluetoothDevice)
                x4?.let { getXY4Values(it) }
            }
            is XY3BluetoothDevice -> {
                val x3 = (device as? XY3BluetoothDevice)
                x3?.let { getXY3Values(it) }
            }
            is XY2BluetoothDevice -> {
                text_currentTime.text = getString(R.string.not_supported_x2)
            }
            else -> {
                text_currentTime.text = getString(R.string.unknown_device)
            }
        }

        throbber?.hide()
    }

    private fun getXY4Values(device: XY4BluetoothDevice) {
        GlobalScope.launch {
            var hasConnectionError = true

            val conn = device.connection {
                hasConnectionError = false

                deviceData?.let {
                    it.currentTime = device.currentTimeService.currentTime.get().await().format()
                    it.localTimeInformation = device.currentTimeService.localTimeInformation.get().await().format()
                    it.referenceTimeInformation = device.currentTimeService.referenceTimeInformation.get().await().format()
                }

                return@connection XYBluetoothResult(true)

            }
            conn.await()

            updateUI()
            checkConnectionError(hasConnectionError)
        }
    }


    private fun getXY3Values(device: XY3BluetoothDevice) {
        GlobalScope.launch {
            var hasConnectionError = true

            val conn = device.connection {
                hasConnectionError = false

                deviceData?.let {
                    it.currentTime = device.currentTimeService.currentTime.get().await().format()
                    it.localTimeInformation = device.currentTimeService.localTimeInformation.get().await().format()
                    it.referenceTimeInformation = device.currentTimeService.referenceTimeInformation.get().await().format()
                }

                return@connection XYBluetoothResult(true)

            }
            conn.await()

            updateUI()
            checkConnectionError(hasConnectionError)
        }
    }

    companion object {

        fun newInstance() =
                CurrentTimeFragment()

        fun newInstance (device: XYBluetoothDevice?, deviceData : XYDeviceData?) : CurrentTimeFragment {
            val frag = CurrentTimeFragment()
            frag.device = device
            frag.deviceData = deviceData
            return frag
        }
    }

}
