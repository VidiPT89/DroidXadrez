package com.vidi.droidxadrez.multiplayer

import android.os.Handler
import android.os.Looper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.vidi.droidxadrez.engine.PieceColor
import com.vidi.droidxadrez.engine.PieceType
import com.vidi.droidxadrez.engine.Square
import kotlinx.coroutines.tasks.await

sealed class MultiplayerError(message: String) : Exception(message) {
    object NotConfigured : MultiplayerError("not-configured")
    object RoomNotFound : MultiplayerError("room-not-found")
    object RoomFull : MultiplayerError("room-full")
    object RoomFinished : MultiplayerError("room-finished")
    object CreateFailed : MultiplayerError("create-failed")
}

data class ChatMessage(val uid: String, val text: String, val mine: Boolean)

/**
 * Networking layer for Multiplayer mode: rooms, moves, chat and presence over Firestore.
 * Mirrors chess-multiplayer.js's role on web — GameViewModel never talks to Firestore directly.
 */
object MultiplayerService {
    val configured: Boolean
        get() {
            val app = try { FirebaseApp.getInstance() } catch (e: IllegalStateException) { return false }
            return app.options.apiKey != "REPLACE_ME"
        }

    var roomCode: String? = null
        private set
    var myColor: PieceColor? = null
        private set
    var opponentOnline: Boolean = false
        private set

    var onRemoteMove: ((Square, Square, PieceType?) -> Unit)? = null
    var onChat: ((ChatMessage) -> Unit)? = null
    var onOpponentJoined: (() -> Unit)? = null
    var onOpponentPresence: ((Boolean) -> Unit)? = null
    var onGameFinished: ((String) -> Unit)? = null

    private var myUid: String? = null
    private var role: String? = null // "host" | "guest"
    private var appliedPly: Int = -1
    private var roomListener: ListenerRegistration? = null
    private var movesListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var lastOppPresence: Map<String, Any>? = null
    private var sawGuest = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null
    private var staleRunnable: Runnable? = null

    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I/L
    private const val PRESENCE_HEARTBEAT_MS = 20_000L
    private const val PRESENCE_STALE_MS = 45_000L

    private fun db() = FirebaseFirestore.getInstance()
    private fun randomCode(): String = (1..6).map { CODE_ALPHABET.random() }.joinToString("")

    private suspend fun ensureSignedIn(): String {
        FirebaseAuth.getInstance().currentUser?.let { return it.uid }
        val result = FirebaseAuth.getInstance().signInAnonymously().await()
        return result.user!!.uid
    }

    suspend fun createRoom(): String {
        if (!configured) throw MultiplayerError.NotConfigured
        val uid = ensureSignedIn()
        repeat(6) {
            val code = randomCode()
            val ref = db().collection("rooms").document(code)
            val existing = runCatching { ref.get().await() }.getOrNull()
            if (existing == null || existing.exists()) return@repeat
            val created = runCatching {
                ref.set(
                    mapOf(
                        "hostUid" to uid, "hostColor" to "w", "guestUid" to null,
                        "status" to "waiting", "result" to null,
                        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(),
                        "hostPresence" to mapOf("online" to true, "lastSeen" to FieldValue.serverTimestamp()),
                        "guestPresence" to mapOf("online" to false, "lastSeen" to FieldValue.serverTimestamp()),
                    )
                ).await()
            }.isSuccess
            if (created) return joinRoom(code)
        }
        throw MultiplayerError.CreateFailed
    }

    suspend fun joinRoom(code: String): String {
        if (!configured) throw MultiplayerError.NotConfigured
        val uid = ensureSignedIn()
        myUid = uid
        val ref = db().collection("rooms").document(code)
        val snap = runCatching { ref.get().await() }.getOrNull()
        if (snap == null || !snap.exists()) throw MultiplayerError.RoomNotFound
        val data = snap.data ?: throw MultiplayerError.RoomNotFound

        val hostUid = data["hostUid"] as? String
        val guestUid = data["guestUid"] as? String
        val hostColor = (data["hostColor"] as? String) ?: "w"
        val status = data["status"] as? String

        when {
            hostUid == uid -> {
                role = "host"
                myColor = if (hostColor == "w") PieceColor.WHITE else PieceColor.BLACK
            }
            guestUid == uid -> {
                role = "guest"
                myColor = if (hostColor == "w") PieceColor.BLACK else PieceColor.WHITE
            }
            guestUid == null -> {
                if (status == "finished") throw MultiplayerError.RoomFinished
                val joined = runCatching {
                    ref.update(
                        mapOf(
                            "guestUid" to uid, "status" to "active",
                            "guestPresence" to mapOf("online" to true, "lastSeen" to FieldValue.serverTimestamp()),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                }.isSuccess
                if (!joined) throw MultiplayerError.RoomFull
                role = "guest"
                myColor = if (hostColor == "w") PieceColor.BLACK else PieceColor.WHITE
            }
            else -> throw MultiplayerError.RoomFull
        }

        roomCode = code
        appliedPly = -1
        sawGuest = guestUid != null || role == "guest"
        opponentOnline = false
        lastOppPresence = null
        attachRoomListener()
        attachMovesListener()
        attachChatListener()
        startHeartbeat()
        return code
    }

    private fun attachRoomListener() {
        roomListener?.remove()
        val code = roomCode ?: return
        roomListener = db().collection("rooms").document(code).addSnapshotListener { snap, _ ->
            val data = snap?.data ?: return@addSnapshotListener
            handleRoomUpdate(data)
        }
        staleRunnable?.let { mainHandler.removeCallbacks(it) }
        staleRunnable = object : Runnable {
            override fun run() {
                recomputePresence()
                mainHandler.postDelayed(this, 8_000L)
            }
        }
        mainHandler.postDelayed(staleRunnable!!, 8_000L)
    }

    private fun handleRoomUpdate(data: Map<String, Any>) {
        val guestUid = data["guestUid"] as? String
        if (guestUid != null && !sawGuest) {
            sawGuest = true
            onOpponentJoined?.invoke()
        }
        val status = data["status"] as? String
        val result = data["result"] as? String
        if (status == "finished" && result != null) {
            onGameFinished?.invoke(result)
        }
        @Suppress("UNCHECKED_CAST")
        lastOppPresence = (if (role == "host") data["guestPresence"] else data["hostPresence"]) as? Map<String, Any>
        recomputePresence()
    }

    private fun recomputePresence() {
        val p = lastOppPresence
        val online = p != null && (p["online"] as? Boolean) == true &&
            (p["lastSeen"] as? com.google.firebase.Timestamp)?.let {
                System.currentTimeMillis() - it.toDate().time < PRESENCE_STALE_MS
            } == true
        if (online != opponentOnline) {
            opponentOnline = online
            onOpponentPresence?.invoke(online)
        }
    }

    private fun attachMovesListener() {
        movesListener?.remove()
        val code = roomCode ?: return
        movesListener = db().collection("rooms").document(code).collection("moves")
            .orderBy("ply", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) handleMoveDoc(change.document.data)
                }
            }
    }

    private fun handleMoveDoc(d: Map<String, Any>) {
        val ply = (d["ply"] as? Long)?.toInt() ?: return
        if (ply <= appliedPly) return
        appliedPly = ply
        if (d["by"] as? String == myUid) return // my own move, applied locally already
        @Suppress("UNCHECKED_CAST")
        val fromMap = d["from"] as? Map<String, Any> ?: return
        @Suppress("UNCHECKED_CAST")
        val toMap = d["to"] as? Map<String, Any> ?: return
        val fr = (fromMap["r"] as? Long)?.toInt() ?: return
        val fc = (fromMap["c"] as? Long)?.toInt() ?: return
        val tr = (toMap["r"] as? Long)?.toInt() ?: return
        val tc = (toMap["c"] as? Long)?.toInt() ?: return
        val promotion = (d["promotion"] as? String)?.let { code -> PieceType.entries.firstOrNull { it.code == code } }
        onRemoteMove?.invoke(Square(fr, fc), Square(tr, tc), promotion)
    }

    private fun attachChatListener() {
        chatListener?.remove()
        val code = roomCode ?: return
        chatListener = db().collection("rooms").document(code).collection("chat")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val d = change.document.data
                        val uid = d["uid"] as? String ?: return@forEach
                        val text = d["text"] as? String ?: return@forEach
                        onChat?.invoke(ChatMessage(uid, text, uid == myUid))
                    }
                }
            }
    }

    private fun presenceField() = if (role == "host") "hostPresence" else "guestPresence"

    private fun sendHeartbeat(online: Boolean) {
        val code = roomCode ?: return
        db().collection("rooms").document(code).update(
            mapOf(
                presenceField() to mapOf("online" to online, "lastSeen" to FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
    }

    private fun startHeartbeat() {
        sendHeartbeat(true)
        heartbeatRunnable?.let { mainHandler.removeCallbacks(it) }
        heartbeatRunnable = object : Runnable {
            override fun run() {
                sendHeartbeat(true)
                mainHandler.postDelayed(this, PRESENCE_HEARTBEAT_MS)
            }
        }
        mainHandler.postDelayed(heartbeatRunnable!!, PRESENCE_HEARTBEAT_MS)
    }

    fun sendMove(from: Square, to: Square, promotion: PieceType?) {
        val code = roomCode ?: return
        val uid = myUid ?: return
        val ply = appliedPly + 1
        val plyId = ply.toString().padStart(4, '0')
        db().collection("rooms").document(code).collection("moves").document(plyId).set(
            mapOf(
                "ply" to ply,
                "from" to mapOf("r" to from.r, "c" to from.c),
                "to" to mapOf("r" to to.r, "c" to to.c),
                "promotion" to promotion?.code,
                "by" to uid,
                "playedAt" to FieldValue.serverTimestamp(),
            )
        )
    }

    fun sendChat(text: String) {
        val code = roomCode ?: return
        val uid = myUid ?: return
        val trimmed = text.trim().take(300)
        if (trimmed.isEmpty()) return
        db().collection("rooms").document(code).collection("chat").add(
            mapOf("uid" to uid, "text" to trimmed, "sentAt" to FieldValue.serverTimestamp())
        )
    }

    fun resign() {
        val code = roomCode ?: return
        val color = myColor ?: return
        db().collection("rooms").document(code).update(
            mapOf("status" to "finished", "result" to "resign-" + color.code, "updatedAt" to FieldValue.serverTimestamp())
        )
    }

    fun leaveRoom() {
        sendHeartbeat(false)
        roomListener?.remove(); roomListener = null
        movesListener?.remove(); movesListener = null
        chatListener?.remove(); chatListener = null
        heartbeatRunnable?.let { mainHandler.removeCallbacks(it) }; heartbeatRunnable = null
        staleRunnable?.let { mainHandler.removeCallbacks(it) }; staleRunnable = null
        roomCode = null
        role = null
        myColor = null
        appliedPly = -1
        opponentOnline = false
        lastOppPresence = null
        sawGuest = false
    }
}
