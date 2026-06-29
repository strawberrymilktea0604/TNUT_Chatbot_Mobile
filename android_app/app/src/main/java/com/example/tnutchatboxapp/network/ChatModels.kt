package com.example.tnutchatboxapp.network

data class Message(val question: String)
data class ResponseMessage(val answer: String, val query_time: Float? = null)