package com.shaadow.onecalculator.mathly.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shaadow.onecalculator.ChatAdapter
import com.shaadow.onecalculator.R
import com.shaadow.onecalculator.mathly.logic.Validation
import com.shaadow.onecalculator.mathly.ai.MathAiClient
import com.shaadow.onecalculator.parser.Expression
import kotlinx.coroutines.launch

class MathlyChatFragment : Fragment() {
    private lateinit var chatRecycler: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        chatRecycler = view.findViewById(R.id.chat_recycler)
        messageInput = view.findViewById(R.id.message_input)
        sendButton = view.findViewById(R.id.send_button)
        chatAdapter = ChatAdapter()
        chatRecycler.adapter = chatAdapter
        chatRecycler.layoutManager = LinearLayoutManager(requireContext())
        setupSendButton()
        return view
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val userInput = messageInput.text.toString().trim()
            if (userInput.isEmpty()) return@setOnClickListener
            chatAdapter.addUserMessage(userInput)
            scrollToBottom()
            messageInput.setText("")
            if (Validation.isGreeting(userInput)) {
                chatAdapter.addAIMessage("Hello! I'm Mathly. How can I help you with math today?")
                scrollToBottom()
                return@setOnClickListener
            }
            if (isSimpleMathExpression(userInput)) {
                try {
                    val result = Expression.calculate(userInput)
                    chatAdapter.addAIMessage("Result: $result")
                } catch (e: Exception) {
                    chatAdapter.addAIMessage("Sorry, Mathly couldn't evaluate that expression.")
                }
                scrollToBottom()
                return@setOnClickListener
            }
            if (!Validation.isMathQuery(userInput)) {
                chatAdapter.addAIMessage("Sorry, Mathly can only help with math expressions and questions.")
                scrollToBottom()
                return@setOnClickListener
            }
            // Show loading bubble
            chatAdapter.addAIMessage("Thinking...")
            scrollToBottom()
            val loadingIndex = chatAdapter.itemCount - 1
            // Call AI client
            lifecycleScope.launch {
                try {
                    val aiReply = MathAiClient.sendMathQuery(userInput)
                    chatAdapter.addAIMessage(aiReply.trim())
                    // Optionally, remove the loading bubble if you want
                    // Or update the message at loadingIndex
                } catch (e: Exception) {
                    chatAdapter.addAIMessage("Error connecting to AI: ${e.message}")
                }
                scrollToBottom()
            }
        }
    }

    private fun scrollToBottom() {
        chatRecycler.post {
            chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun isSimpleMathExpression(input: String): Boolean {
        // Accepts only numbers, operators, parentheses, decimal points, spaces
        val simpleMathRegex = Regex("^[\\d+\\-*/%^().!√\\s]+")
        return simpleMathRegex.matches(input)
    }
}
