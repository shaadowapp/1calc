package com.shaadow.onecalculator.mathly.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shaadow.onecalculator.R
import com.shaadow.onecalculator.mathly.logic.Validation
import com.shaadow.onecalculator.mathly.ai.MathAiClient
import kotlinx.coroutines.launch
import com.shaadow.onecalculator.parser.Expression
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

class MathlyChatFragment : Fragment() {
    private lateinit var chatContent: LinearLayout
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var welcomeSection: View
    private lateinit var chatScroll: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        chatContent = view.findViewById(R.id.chat_content)
        messageInput = view.findViewById(R.id.message_input)
        sendButton = view.findViewById(R.id.send_button)
        chatScroll = view.findViewById(R.id.chat_scroll)
        welcomeSection = chatContent.getChildAt(0) // Welcome section is the first child
        setupSendButton(inflater)
        return view
    }

    private fun setupSendButton(inflater: LayoutInflater) {
        sendButton.setOnClickListener {
            val userInput = messageInput.text.toString().trim()
            if (userInput.isEmpty()) return@setOnClickListener
            // Hide welcome UI on first message
            if (welcomeSection.visibility == View.VISIBLE) {
                welcomeSection.visibility = View.GONE
            }
            addUserMessage(userInput, inflater)
            messageInput.setText("")
            scrollToBottom()
            if (Validation.isGreeting(userInput)) {
                addAiMessage("Hello! I'm Mathly. How can I help you with math today?", inflater)
                scrollToBottom()
                return@setOnClickListener
            }
            if (isSimpleMathExpression(userInput)) {
                try {
                    val result = Expression.calculate(userInput)
                    addAiMessage("Result: $result", inflater)
                } catch (e: Exception) {
                    addAiMessage("Sorry, Mathly couldn't evaluate that expression.", inflater)
                }
                scrollToBottom()
                return@setOnClickListener
            }
            if (!Validation.isMathQuery(userInput)) {
                addAiMessage("Sorry, Mathly can only help with math expressions and questions.", inflater)
                scrollToBottom()
                return@setOnClickListener
            }
            // Show loading bubble
            val loadingBubble = addAiMessage("Thinking...", inflater, isLoading = true)
            scrollToBottom()
            // Call AI client
            lifecycleScope.launch {
                try {
                    val aiReply = MathAiClient.sendMathQuery(userInput)
                    // Replace loading bubble text with AI reply
                    loadingBubble.findViewById<TextView>(R.id.message_text).text = aiReply.trim()
                    scrollToBottom()
                } catch (e: Exception) {
                    loadingBubble.findViewById<TextView>(R.id.message_text).text = "Error connecting to AI: ${e.message}"
                    scrollToBottom()
                }
            }
        }
    }

    private fun addUserMessage(message: String, inflater: LayoutInflater) {
        val userBubble = inflater.inflate(R.layout.item_chat_message_user, chatContent, false)
        val messageText = userBubble.findViewById<TextView>(R.id.message_text)
        val actionGroup = userBubble.findViewById<LinearLayout>(R.id.user_action_group)
        val btnCopy = userBubble.findViewById<ImageButton>(R.id.btn_copy)
        messageText.text = message
        // Long press to show action group
        userBubble.setOnLongClickListener {
            actionGroup.visibility = View.VISIBLE
            true
        }
        // Hide action group on tap elsewhere in the bubble
        userBubble.setOnClickListener {
            actionGroup.visibility = View.GONE
        }
        btnCopy.setOnClickListener {
            copyToClipboard(message)
        }
        chatContent.addView(userBubble)
    }

    private fun addAiMessage(message: String, inflater: LayoutInflater, isLoading: Boolean = false): View {
        val aiBubble = inflater.inflate(R.layout.item_chat_message_ai, chatContent, false)
        val messageText = aiBubble.findViewById<TextView>(R.id.message_text)
        val btnCopy = aiBubble.findViewById<ImageButton>(R.id.btn_copy)
        val btnLike = aiBubble.findViewById<ImageButton>(R.id.btn_like)
        val btnDislike = aiBubble.findViewById<ImageButton>(R.id.btn_dislike)
        val btnShare = aiBubble.findViewById<ImageButton>(R.id.btn_share)
        messageText.text = message
        btnCopy.setOnClickListener {
            copyToClipboard(message)
        }
        btnLike.setOnClickListener {
            toggleLikeDislike(btnLike, btnDislike)
        }
        btnDislike.setOnClickListener {
            toggleLikeDislike(btnDislike, btnLike)
        }
        btnShare.setOnClickListener {
            shareText(message)
        }
        chatContent.addView(aiBubble)
        return aiBubble
    }

    private fun scrollToBottom() {
        chatScroll.post {
            chatScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun isSimpleMathExpression(input: String): Boolean {
        // Accepts only numbers, operators, parentheses, decimal points, spaces
        val simpleMathRegex = Regex("""^[\d+\-*/%^().!√\s]+$""")
        return simpleMathRegex.matches(input)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun toggleLikeDislike(selected: ImageButton, other: ImageButton) {
        val isSelected = selected.tag == true
        if (isSelected) {
            selected.tag = false
            selected.setColorFilter(resources.getColor(android.R.color.white, null))
        } else {
            selected.tag = true
            selected.setColorFilter(resources.getColor(R.color.brand_purple, null))
            other.tag = false
            other.setColorFilter(resources.getColor(android.R.color.white, null))
            val action = if (selected.id == R.id.btn_like) "Liked" else "Disliked"
            Toast.makeText(requireContext(), action, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, "Share Mathly's reply"))
    }
}
