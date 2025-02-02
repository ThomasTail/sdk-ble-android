package network.xyo.ble.sample.activities

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_server.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import network.xyo.ble.bluetooth.BluetoothIntentReceiver
import network.xyo.ble.gatt.server.*
import network.xyo.ble.gatt.server.responders.XYBluetoothReadResponder
import network.xyo.ble.gatt.server.responders.XYBluetoothWriteResponder
import network.xyo.ble.sample.R
import network.xyo.ble.sample.fragments.AdvertiserFragment
import network.xyo.ble.sample.fragments.RootServicesFragment
import network.xyo.ui.XYBaseFragment
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

class XYOServerActivity : XYOAppBaseActivity() {
    var bleServer : XYBluetoothGattServer? = null
    var bleAdvertiser : XYBluetoothAdvertiser? = null
    val bluetoothIntentReceiver = BluetoothIntentReceiver()
    private lateinit var pagerAdapter: SectionsPagerAdapter

    private val simpleService = XYBluetoothService(
            UUID.fromString("3079ca44-ae64-4797-b4e5-a31e3304c481"),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
    )

    private val characteristicRead = XYBluetoothCharacteristic(
            UUID.fromString("01ef8f90-e99f-48ae-87bb-f683b93c692f"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
    )

    private val characteristicWrite = XYBluetoothCharacteristic(
            UUID.fromString("02ef8f90-e99f-48ae-87bb-f683b93c692f"),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    val services = arrayOf<BluetoothGattService>(simpleService)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)


        val tabAdapter = SectionsPagerAdapter(supportFragmentManager)
        pagerAdapter = tabAdapter
        val serverPagerContainer = findViewById<ViewPager>(R.id.server_pager_container)
        serverPagerContainer.adapter = pagerAdapter
        serverPagerContainer.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(server_tabs))
        serverPagerContainer.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(server_tabs) as ViewPager.OnPageChangeListener)
        server_tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(server_pager_container))
        bleAdvertiser = XYBluetoothAdvertiser(applicationContext)

        registerReceiver(bluetoothIntentReceiver, BluetoothIntentReceiver.bluetoothDeviceIntentFilter)


        GlobalScope.launch {
            spinUpServer().await()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothIntentReceiver)
        bleServer?.stopServer()
    }

    private fun createTestServer() = GlobalScope.async {
        val server = XYBluetoothGattServer(applicationContext)
        server.startServer()
        bleServer = server
        return@async server.addService(simpleService).await()
    }

    private fun spinUpServer () = GlobalScope.async {
        simpleService.addCharacteristic(characteristicRead)
        simpleService.addCharacteristic(characteristicWrite)
        characteristicRead.addReadResponder("countResponder", countResponder)
        characteristicWrite.addWriteResponder("log Responder",logResponder)
        return@async createTestServer().await()
    }

    /**
     * A simple write characteristic that logs whenever it is written to.
     */
    private val logResponder = object : XYBluetoothWriteResponder {
        override fun onWriteRequest(writeRequestValue: ByteArray?, device: BluetoothDevice?): Boolean? {
            Log.v("BluetoothGattServer", writeRequestValue?.toString(Charset.defaultCharset()))
            return true
        }
    }

    /**
     * A simple read characteristic that increases one time every time it is read.
     */
    private val countResponder = object : XYBluetoothReadResponder {
        var count = 0

        override fun onReadRequest(device: BluetoothDevice?, offset: Int): XYBluetoothGattServer.XYReadRequest? {
            count++
            return XYBluetoothGattServer.XYReadRequest( ByteBuffer.allocate(4).putInt(count).array(), 0)
        }
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val size = 3
        private val fragments: SparseArray<XYBaseFragment> = SparseArray(size)

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> return AdvertiserFragment.newInstance(bleAdvertiser)
                1 -> return RootServicesFragment.newInstance(services)
            }

            throw Exception("Position out of index!")
        }

        override fun getCount(): Int {
            return size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as XYBaseFragment
            fragments.append(position, fragment)
            return fragment
        }

        fun getFragmentByPosition(position: Int): XYBaseFragment? {
            return fragments.get(position)
        }
    }
}