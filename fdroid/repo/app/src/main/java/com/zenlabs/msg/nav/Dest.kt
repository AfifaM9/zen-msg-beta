package com.zenlabs.msg.nav

sealed class Dest(val route: String) {
    data object Conversations : Dest("conversations")
    data object New : Dest("new")
    data object Chat : Dest("chat/{conversationId}") {
        fun build(id: Long) = "chat/$id"
        const val ARG = "conversationId"
    }
}
