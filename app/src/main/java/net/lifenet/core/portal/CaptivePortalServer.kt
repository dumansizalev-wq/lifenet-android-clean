package net.lifenet.core.portal

import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * CaptivePortalServer: Uygulama yüklemeyen kullanıcılara web üzerinden erişim sağlar.
 */
class CaptivePortalServer(private val port: Int = 8080) {

    private val threadPool = Executors.newFixedThreadPool(4)
    private var isRunning = false
    private var serverSocket: ServerSocket? = null

    // Mesajları mesh katmanına ileten callback
    var onMessageReceived: ((String, ByteArray?) -> Unit)? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        
        threadPool.execute {
            try {
                serverSocket = ServerSocket(port)
                Log.i("LIFENET", "PortalServer: Listening on port $port")
                
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                Log.e("LIFENET", "PortalServer Error: ${e.message}")
            } finally {
                stop()
            }
        }
    }

    private fun handleClient(client: Socket) {
        threadPool.execute {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val out = PrintWriter(client.getOutputStream())
                
                val firstLine = reader.readLine() ?: return@execute
                val parts = firstLine.split(" ")
                if (parts.size < 2) return@execute
                
                val method = parts[0]
                val path = parts[1]

                if (method == "GET") {
                    serveTemplate(path, out, client.getOutputStream())
                } else if (method == "POST") {
                    handlePost(reader, out)
                }
                
                client.close()
            } catch (e: Exception) {
                Log.e("LIFENET", "PortalHandler Error: ${e.message}")
            }
        }
    }

    private fun serveTemplate(path: String, out: PrintWriter, rawOut: OutputStream) {
        // Basit routing ( index.html varsayıyoruz )
        val html = """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>LIFENET EMERGENCY PORTAL</title>
                <style>
                    body { background: #0a0a0a; color: #00ff7f; font-family: 'Segoe UI', sans-serif; text-align: center; padding: 20px; }
                    .card { background: #1a1a1a; padding: 20px; border-radius: 12px; border: 1px solid #00ff7f; margin: 10px; }
                    input, textarea { width: 90%; background: #222; border: 1px solid #333; color: white; padding: 10px; margin: 10px 0; border-radius: 5px; }
                    button { background: #00ff7f; color: black; border: none; padding: 15px 30px; font-weight: bold; border-radius: 5px; cursor: pointer; }
                    .status { font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <h1>LIFENET ACİL DURUM PORTALI</h1>
                <div class="card">
                    <p>Panik Yapmayın. Bağlantınız Aktif.</p>
                    <textarea id="msg" placeholder="Mesajınızı yazın..."></textarea>
                    <button onclick="send()">GÖNDER (SOS)</button>
                    <p class="status">Bağlantı Türü: Peer-to-Peer Mesh</p>
                </div>
                <script>
                    function send() {
                        const msg = document.getElementById('msg').value;
                        fetch('/message', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ message: msg })
                        }).then(() => alert('Mesaj Mesh Ağına İletildi.'));
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        out.println("HTTP/1.1 200 OK")
        out.println("Content-Type: text/html")
        out.println("Content-Length: ${html.length}")
        out.println()
        out.println(html)
        out.flush()
    }

    private fun handlePost(reader: BufferedReader, out: PrintWriter) {
        // POST body okuma ve Mesaj İletme (Sadeleştirilmiş)
        var line: String?
        var contentLength = 0
        while (reader.readLine().also { line = it } != "") {
            if (line?.startsWith("Content-Length:") == true) {
                contentLength = line!!.substring(15).trim().toInt()
            }
        }
        
        val body = CharArray(contentLength)
        reader.read(body, 0, contentLength)
        val json = String(body)
        
        Log.i("LIFENET", "PortalServer: Received Message -> $json")
        onMessageReceived?.invoke(json, null)

        out.println("HTTP/1.1 200 OK")
        out.println("Content-Length: 0")
        out.println()
        out.flush()
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }
}
