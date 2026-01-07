package net.lifenet.core.mesh.portal

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class MeshBridgeServer(port: Int, private val onMessageReceived: (sender: String, content: String) -> Unit) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (method == Method.POST && uri == "/send") {
            return handleSendMessage(session)
        }

        // Serve the main portal page
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>LIFENET Mesh Bridge</title>
                <style>
                    body { background: #0D0D0D; color: #FFFFFF; font-family: sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                    .card { background: #1A1A1A; border: 1px solid #333; padding: 24dp; border-radius: 16px; width: 90%; max-width: 400px; text-align: center; }
                    h1 { color: #D4AF37; letter-spacing: 2px; font-size: 24px; }
                    p { color: #888; font-size: 14px; }
                    input, button { width: 100%; padding: 12px; margin-top: 12px; border-radius: 8px; border: none; outline: none; box-sizing: border-box; }
                    input { background: #262626; color: #FFF; }
                    button { background: #D4AF37; color: #000; font-weight: bold; cursor: pointer; }
                    #status { margin-top: 16px; font-size: 12px; color: #00E676; display: none; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>LIFENET</h1>
                    <p>MESH BRIDGE ACTIVE</p>
                    <input type="text" id="name" placeholder="Your Name">
                    <input type="text" id="msg" placeholder="Type a message...">
                    <button onclick="send()">SEND TO MESH</button>
                    <div id="status">MESSAGE SENT TO MESH!</div>
                </div>
                <script>
                    function send() {
                        const name = document.getElementById('name').value;
                        const msg = document.getElementById('msg').value;
                        if(!msg) return;
                        
                        fetch('/send', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ sender: name || 'Guest', content: msg })
                        }).then(() => {
                            document.getElementById('status').style.display = 'block';
                            document.getElementById('msg').value = '';
                            setTimeout(() => { document.getElementById('status').style.display = 'none'; }, 3000);
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(html)
    }

    private fun handleSendMessage(session: IHTTPSession): Response {
        return try {
            val map = HashMap<String, String>()
            session.parseBody(map)
            val postData = map["postData"] ?: "{}"
            val json = JSONObject(postData)
            val sender = json.getString("sender")
            val content = json.getString("content")
            
            Log.d("MeshBridgeServer", "Msg from Web: $sender -> $content")
            onMessageReceived(sender, content)
            
            newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}")
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: ${e.message}")
        }
    }
}
