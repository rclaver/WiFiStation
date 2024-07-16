package cat.tron.wifistation

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
   private lateinit var title_srvIP: TextView
   private lateinit var title_srvPort: TextView
   private lateinit var title_estat: TextView
   private lateinit var srvIP: EditText
   private lateinit var srvPort: EditText
   private lateinit var estat: TextView
   private lateinit var btnConnect: Button
   private lateinit var wifi_on: ImageView
   private lateinit var wifi_off: ImageView
   private lateinit var temperatura: TextView
   private lateinit var receiveMessages: TextView
   private lateinit var sendMessages: TextView

   private lateinit var wifiManager: WifiManager
   private var SERVER_IP: String? = null
   private var SERVER_PORT = 3000
   private var count: Int = 0
   private var serverActive = 9

   private lateinit var output: PrintWriter
   private lateinit var InputStream: InputStream

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)

      iniciParametres()
      val btnDecrementar: ImageButton = findViewById(R.id.btnDecrementar)
      val btnIncrementar: ImageButton = findViewById(R.id.btnIncrementar)
      val btnClose: ImageButton = findViewById(R.id.btnClose)

      if (checkWiFi()) {
         Thread(MainThread()).start()
         if (isSocketActive(3000)) {
            wifi_off.visibility = View.INVISIBLE
            wifi_on.visibility = View.VISIBLE
         }
      }

      btnConnect.setOnClickListener {
         estat.text = getString(R.string.connecting)
         sendMessages.text = ""

         if (checkWiFi()) {
            if (!Thread(MainThread()).isAlive) {Thread(MainThread()).start() }
            if (isSocketActive(3000)) {
               wifi_off.visibility = View.INVISIBLE
               wifi_on.visibility = View.VISIBLE
            }
         }
      }

      btnDecrementar.setOnClickListener {
         if (checkWiFi()) {
            if (isSocketActive(1000)) {
               Thread(SendDataThread(btnDecrementar.tag.toString(), ++count)).start()
            }
         }
      }

      btnIncrementar.setOnClickListener {
         if (checkWiFi()) {
            if (isSocketActive(1000)) {
               Thread(SendDataThread(btnIncrementar.tag.toString(), ++count)).start()
            }
         }
      }

      btnClose.setOnClickListener {
         finishAndRemoveTask()
         exitProcess(0)
      }

   }

   internal inner class MainThread : Runnable {
      override fun run() {
         val socket: Socket
         try {
            serverActive = 0
            socket = Socket()
            socket.connect(InetSocketAddress(SERVER_IP, SERVER_PORT), 3000)
            if (socket.isConnected) {
               serverActive = 1
               output = PrintWriter(socket.getOutputStream())
               InputStream = socket.getInputStream()
               Thread(ListenerThread()).start()
            }
         }catch (e: IOException) {
            estat.append(String.format(getString(R.string.err_MainThread), e.message))
            e.printStackTrace()
         }
      }
   }

   internal inner class ListenerThread : Runnable {
      override fun run() {
         val buffer = ByteArray(32)
         var bytesRead: Int
         while (true) {
            try {
               bytesRead = InputStream.read(buffer)
               if (bytesRead != -1) {
                  val message = String(buffer, 0, bytesRead)
                  try {
                     val json = JSONObject(message)
                     val ajson = json.names()
                     when (ajson[0]) {
                        "temperatura" -> runOnUiThread {temperatura.text = String.format(getString(R.string.temperatura), json.getString("temperatura"))}
                        "p" -> runOnUiThread {receiveMessages.append("p:" + json.getString("p") + " § ")}
                        "b" -> runOnUiThread {receiveMessages.append("b:" + json.getString("b") + " § ")}
                        else -> {runOnUiThread {receiveMessages.append("incomprensible:" + ajson[0] + " § ")}}
                     }
                  }catch (e: JSONException) {
                     runOnUiThread { estat.append(String.format(getString(R.string.err_Json), e.message)) }
                     InputStream.close()
                     Thread(MainThread()).start()
                     return
                  }
               }else {
                  InputStream.close()
                  Thread(MainThread()).start()
                  return
               }
            }catch (e: IOException) {
               estat.append(String.format(getString(R.string.err_ListenerThread), e.message))
               e.printStackTrace()
            }
         }
      }
   }

   internal inner class SendDataThread(private val message: String, private val n: Int) : Runnable {
      override fun run() {
         if (message.isNotEmpty()) {
            output.write(message)
            output.flush()
            runOnUiThread(Runnable {sendMessages.text = String.format(getString(R.string.send_message), n, message)})
         }
      }
   }

   private fun checkWiFi(): Boolean {
      val isWiFi = wifiManager.isWifiEnabled
      if (!isWiFi) {
         title_estat.visibility = View.VISIBLE
         estat.text = getString(R.string.no_wifi)
         serverVisible(View.VISIBLE)
      }else if (estat.text.toString() == getString(R.string.no_wifi)) {
         noHiHaError()
      }
      return isWiFi
   }

   private fun isSocketActive(time: Long): Boolean {
      Thread.sleep(time)
      var active = (serverActive == 1)
      if (!active) {
         title_estat.visibility = View.VISIBLE
         estat.text = getString(R.string.no_connect)
         serverVisible(View.VISIBLE)
      }else if (output.checkError()) {
         serverVisible(View.VISIBLE)
         active = false
      }else if (estat.text.toString() == getString(R.string.no_connect)) {
         serverVisible(View.INVISIBLE)
         noHiHaError()
      }
      return active
   }

   private fun noHiHaError() {
      title_estat.visibility = View.INVISIBLE
      estat.text = getString(R.string.blanc)
   }
   private fun serverVisible(visible: Int) {
      title_srvIP.visibility = visible
      srvIP.visibility = visible
      title_srvPort.visibility = visible
      srvPort.visibility = visible
      btnConnect.visibility = visible
   }

   private fun iniciParametres() {
      title_srvIP = findViewById(R.id.title_srvIP)
      title_srvPort = findViewById(R.id.title_srvPort)
      title_estat = findViewById(R.id.title_estat)
      srvIP = findViewById(R.id.srvIP)
      srvPort = findViewById(R.id.srvPort)
      estat = findViewById(R.id.estat)
      btnConnect = findViewById(R.id.btnConnect)
      wifi_on = findViewById(R.id.wifiON)
      wifi_off = findViewById(R.id.wifiOFF)
      receiveMessages = findViewById(R.id.receiveMessages)
      sendMessages = findViewById(R.id.sendMessages)
      temperatura = findViewById(R.id.temperatura)
      temperatura.text = "?ºC"

      SERVER_IP = srvIP.text.toString().trim()
      SERVER_PORT = srvPort.text.toString().trim().toInt()

      wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
   }

}
