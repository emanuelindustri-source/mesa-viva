package com.mesaviva.app

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Relay de salas para LAN, sin internet.
 *
 * Implementa el mismo protocolo que server/room-server.mjs:
 * host/join/send/guestLeft/closed. El teléfono anfitrión mantiene el relay y
 * los invitados se conectan a ws://IP-LAN:puerto desde el QR.
 */
class LanRelayServer {

    private var server: WebSocketServer? = null
    private var started = false
    private val rooms = ConcurrentHashMap<String, Room>()
    private val clients = ConcurrentHashMap<WebSocket, Client>()

    @Volatile
    var url: String? = null
        private set

    private data class Room(
        val host: WebSocket,
        val hostId: String,
        val guests: ConcurrentHashMap<String, WebSocket> = ConcurrentHashMap(),
    )

    private data class Client(
        var roomCode: String? = null,
        var role: String? = null,
        var clientId: String? = null,
    )

    @Synchronized
    fun start(): String? {
        if (started) return url
        val ip = localIp() ?: return null
        val port = findFreePort() ?: return null
        val latch = CountDownLatch(1)
        val onStartCalled = AtomicBoolean(false)
        val ws = object : WebSocketServer(InetSocketAddress("0.0.0.0", port)) {
            override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
                clients[conn] = Client()
            }

            override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
                drop(conn)
            }

            override fun onMessage(conn: WebSocket, message: String) {
                receive(conn, message)
            }

            override fun onError(conn: WebSocket?, ex: Exception) {
                if (!started) latch.countDown()
                if (conn != null && ex is java.io.IOException) drop(conn)
            }

            override fun onStart() {
                onStartCalled.set(true)
                latch.countDown()
            }
        }
        ws.isReuseAddr = true
        try {
            ws.start()
        } catch (_: Exception) {
            return null
        }
        if (!latch.await(4, TimeUnit.SECONDS) || !onStartCalled.get()) {
            try {
                ws.stop()
            } catch (_: Exception) {
                // Ignorar.
            }
            return null
        }
        // El servidor pudo arrancar o fallar sin onStart; comprobamos con un
        // conexión de sonda mínima no es necesaria: si el puerto falló, onError
        // cerró el latch y el start de Java-WebSocket lanza.
        started = true
        server = ws
        url = "ws://$ip:$port"
        return url
    }

    @Synchronized
    fun stop() {
        try {
            for (ws in clients.keys) {
                try {
                    ws.close()
                } catch (_: Exception) {
                    // Ignorar.
                }
            }
            server?.stop()
        } catch (_: Exception) {
            // Ignorar.
        }
        clients.clear()
        rooms.clear()
        server = null
        url = null
        started = false
    }

    private fun receive(ws: WebSocket, raw: String) {
        if (raw.length > 64 * 1024) return
        val msg = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return
        }
        val client = clients[ws] ?: Client().also { clients[ws] = it }
        when (msg.optString("t")) {
            "host" -> {
                val code = normalize(msg.optString("code"))
                if (code.length < 4) {
                    send(ws, JSONObject().put("t", "error").put("key", "sala.invalidCode"))
                    return
                }
                if (rooms.containsKey(code)) {
                    send(ws, JSONObject().put("t", "error").put("key", "sala.codeTaken"))
                    return
                }
                if (rooms.size >= 200) {
                    send(ws, JSONObject().put("t", "error").put("key", "sala.serverBusy"))
                    return
                }
                val hostId = "host-${shortId()}"
                client.roomCode = code
                client.role = "host"
                client.clientId = hostId
                rooms[code] = Room(ws, hostId)
                send(ws, JSONObject().put("t", "hosted").put("code", code).put("hostId", hostId))
            }

            "join" -> {
                val code = normalize(msg.optString("code"))
                val room = rooms[code]
                if (room == null) {
                    send(ws, JSONObject().put("t", "error").put("key", "sala.invalid"))
                    return
                }
                if (room.guests.size >= 8) {
                    send(ws, JSONObject().put("t", "error").put("key", "sala.full"))
                    return
                }
                val guestId = "guest-${shortId()}"
                client.roomCode = code
                client.role = "guest"
                client.clientId = guestId
                room.guests[guestId] = ws
                send(ws, JSONObject().put("t", "joined").put("code", code).put("guestId", guestId))
                send(room.host, JSONObject()
                    .put("t", "guest")
                    .put("guestId", guestId)
                    .put("name", msg.optString("name", "Invitado").take(24))
                    .put("avatar", msg.optString("avatar", "tucan").take(24)))
            }

            "send" -> {
                val room = client.roomCode?.let { rooms[it] } ?: return
                val payload = msg.optJSONObject("payload") ?: return
                if (client.role == "host") {
                    val to = msg.optString("to")
                    val guest = room.guests[to] ?: return
                    send(guest, JSONObject()
                        .put("t", "message")
                        .put("from", client.clientId)
                        .put("payload", payload))
                } else {
                    send(room.host, JSONObject()
                        .put("t", "message")
                        .put("from", client.clientId)
                        .put("payload", payload))
                }
            }

            "ping" -> send(ws, JSONObject().put("t", "pong"))
        }
    }

    private fun drop(ws: WebSocket) {
        val client = clients.remove(ws) ?: return
        val code = client.roomCode ?: return
        val room = rooms[code] ?: return
        if (client.role == "host") {
            for (guest in room.guests.values) {
                send(guest, JSONObject().put("t", "closed"))
                try {
                    guest.close()
                } catch (_: Exception) {
                    // Ignorar.
                }
            }
            rooms.remove(code)
        } else {
            room.guests.remove(client.clientId)
            send(room.host, JSONObject().put("t", "guestLeft").put("guestId", client.clientId))
        }
    }

    private fun send(ws: WebSocket, json: JSONObject) {
        try {
            if (ws.isOpen) ws.send(json.toString())
        } catch (_: Exception) {
            // La conexión puede cerrarse durante el envío.
        }
    }

    private fun normalize(raw: String): String =
        raw.uppercase().replace(Regex("[^A-Z0-9]"), "").take(6)

    private fun shortId(): String = UUID.randomUUID().toString().replace("-", "").take(8)

    private fun findFreePort(): Int? {
        for (port in 9001..9010) {
            try {
                ServerSocket(port).use { return port }
            } catch (_: Exception) {
                // Puerto ocupado; probar el siguiente.
            }
        }
        return null
    }

    private fun localIp(): String? {
        return try {
            for (iface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!iface.isUp || iface.isLoopback) continue
                for (address in Collections.list(iface.inetAddresses)) {
                    if (address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
