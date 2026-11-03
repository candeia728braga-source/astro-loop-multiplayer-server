package com.astroloop.server

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

private const val PORT = 7777

private val clients = ConcurrentHashMap<Socket, String>()

fun main() {
    println("================================")
    println("     ASTRO LOOP MULTIPLAYER")
    println("================================")
    println("Iniciando servidor na porta $PORT...")

    ServerSocket(PORT).use { server ->
        println("Servidor iniciado!")
        println("Aguardando jogadores...")

        while (true) {
            val socket = server.accept()
            val playerId = "Player-${clients.size + 1}"

            clients[socket] = playerId
            println("$playerId conectado: ${socket.inetAddress.hostAddress}")

            thread {
                handleClient(socket, playerId)
            }
        }
    }
}

private fun handleClient(socket: Socket, playerId: String) {
    try {
        val input = socket.getInputStream().bufferedReader()
        val output = socket.getOutputStream().bufferedWriter()

        output.write("CONNECTED:$playerId\n")
        output.flush()

        while (true) {
            val message = input.readLine() ?: break

            println("[$playerId] $message")
            broadcast("$playerId:$message", socket)
        }
    } catch (e: Exception) {
        println("$playerId desconectou.")
    } finally {
        clients.remove(socket)
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }
}

private fun broadcast(message: String, sender: Socket) {
    clients.keys.forEach { client ->
        if (client != sender && !client.isClosed) {
            try {
                val output = client.getOutputStream().bufferedWriter()
                output.write("$message\n")
                output.flush()
            } catch (_: Exception) {
                // Cliente desconectado; será removido pelo próprio handler.
            }
        }
    }
}
