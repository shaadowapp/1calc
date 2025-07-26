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
import com.shaadow.onecalculator.mathly.logic.ConversationHandler
import com.shaadow.onecalculator.mathly.ai.MathAiClient
import com.shaadow.onecalculator.parser.Expression
import kotlinx.coroutines.launch
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AlertDialog

class MathlyChatFragment : Fragment(), ChatAdapter.OnAiActionListener {
    private lateinit var chatRecycler: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter

    private fun showWelcomeUI(promptGroup: View, askMathlyText: TextView) {
        promptGroup.visibility = View.VISIBLE
        askMathlyText.visibility = View.VISIBLE
        promptGroup.bringToFront()
    }
    private fun hideWelcomeUI(promptGroup: View, askMathlyText: TextView) {
        promptGroup.visibility = View.GONE
        askMathlyText.visibility = View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        chatRecycler = view.findViewById(R.id.chat_recycler)
        messageInput = view.findViewById(R.id.message_input)
        sendButton = view.findViewById(R.id.send_button)
        chatAdapter = ChatAdapter()
        chatAdapter.setOnAiActionListener(this)
        chatRecycler.adapter = chatAdapter
        chatRecycler.layoutManager = LinearLayoutManager(requireContext())
        val promptGroup = view.findViewById<View>(R.id.prompt_group)
        val askMathlyText = view.findViewById<TextView>(R.id.ask_mathly_gradient)
        if (chatAdapter.itemCount == 0) {
            showWelcomeUI(promptGroup, askMathlyText)
        } else {
            hideWelcomeUI(promptGroup, askMathlyText)
        }
        askMathlyText.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val width = v.width.toFloat()
            if (width > 0) {
                val textShader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(
                        Color.parseColor("#4285F4"), // Start (blue)
                        Color.parseColor("#5D2DC8")  // End (purple/blue-violet)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                (v as TextView).paint.shader = textShader
            }
        }
        // Example chips logic (Material Chips)
        val chipIds = listOf(
            R.id.example_chip_1, R.id.example_chip_2, R.id.example_chip_3,
            R.id.example_chip_4, R.id.example_chip_5, R.id.example_chip_6
        )
        chipIds.forEach { id ->
            view.findViewById<com.google.android.material.chip.Chip>(id).setOnClickListener { chip ->
                val chipText = (chip as com.google.android.material.chip.Chip).text.toString()
                android.util.Log.d("MathlyChatFragment", "Chip clicked: $chipText")
                sendUserMessage(chipText, askMathlyText, promptGroup)
                // Only hide after sendUserMessage
                hideWelcomeUI(promptGroup, askMathlyText)
            }
        }
        setupSendButton(askMathlyText, promptGroup)
        setupSettingsPopup(view, promptGroup, askMathlyText)
        return view
    }

    private fun sendUserMessage(userInput: String, askMathlyText: TextView, promptGroup: View) {
        if (userInput.isEmpty()) return
        
        chatAdapter.addUserMessage(userInput)
        hideWelcomeUI(promptGroup, askMathlyText)
        scrollToBottom()
        messageInput.setText("")
        
        // Check for conversational questions FIRST (before content filter)
        if (ConversationHandler.isConversationalQuestion(userInput)) {
            val response = ConversationHandler.getConversationalResponse(userInput)
            if (response != null) {
                chatAdapter.addAIMessage(response)
                scrollToBottom()
                return
            }
        }
        
        // Check for greetings (fallback)
        if (Validation.isGreeting(userInput)) {
            val greetingResponse = ConversationHandler.getConversationalResponse("hi")
            chatAdapter.addAIMessage(greetingResponse ?: "Hello! I'm Mathly. How can I help you with math today?")
            scrollToBottom()
            return
        }
        
        // Check for inappropriate content AFTER conversation checks
        if (Validation.containsInappropriateContent(userInput)) {
            chatAdapter.addAIMessage(Validation.getFilteredMessage(userInput))
            scrollToBottom()
            return
        }
        
        // Check for simple math expressions
        if (isSimpleMathExpression(userInput)) {
            try {
                val result = Expression.calculate(userInput)
                val response = "Result: $result\n\n" + ConversationHandler.getRandomEncouragement()
                chatAdapter.addAIMessage(response)
            } catch (e: Exception) {
                chatAdapter.addAIMessage("Sorry, Mathly couldn't evaluate that expression. Could you please check the format and try again?")
            }
            scrollToBottom()
            return
        }
        
        // Check if it's a math query
        if (!Validation.isMathQuery(userInput)) {
            val stats = ConversationHandler.getConversationStats()
            val questionCount = stats["questionCount"] as? Int ?: 0
            
            val response = if (questionCount <= 2) {
                "I'm Mathly, your AI math assistant! I can help you with calculations, equations, problem solving, and explaining math concepts. Try asking me something like:\n\n" +
                "• 'Solve 2x + 5 = 15'\n" +
                "• 'Explain quadratic equations'\n" +
                "• 'What is calculus?'\n" +
                "• 'Help me with algebra'"
            } else {
                "I'm designed to help with mathematics! I can solve equations, explain concepts, and help with calculations. What math problem or topic would you like to work on?"
            }
            
            chatAdapter.addAIMessage(response)
            scrollToBottom()
            return
        }
        
        // Show loading bubble
        chatAdapter.addAIMessage("Thinking...")
        scrollToBottom()
        
        // Call AI client
        lifecycleScope.launch {
            try {
                val aiReply = MathAiClient.sendMathQuery(userInput)
                val enhancedReply = enhanceAIResponse(aiReply.trim())
                chatAdapter.updateLastAIMessage(enhancedReply)
            } catch (e: Exception) {
                val errorResponse = "I'm having trouble connecting right now. Please try again in a moment. If the problem persists, you can try:\n\n" +
                                  "• Checking your internet connection\n" +
                                  "• Asking a simpler question\n" +
                                  "• Restarting the app"
                chatAdapter.updateLastAIMessage(errorResponse)
            }
            scrollToBottom()
        }
    }
    
    /**
     * Enhance AI response with conversation context
     */
    private fun enhanceAIResponse(aiReply: String): String {
        val stats = ConversationHandler.getConversationStats()
        val questionCount = stats["questionCount"] as? Int ?: 0
        val userMood = stats["userMood"] as? String ?: "neutral"
        
        // Add encouragement for frustrated users
        if (userMood == "frustrated") {
            return "$aiReply\n\n${ConversationHandler.getRandomEncouragement()}"
        }
        
        // Add follow-up for longer conversations
        if (questionCount > 3) {
            val followUp = when {
                aiReply.contains("solution") || aiReply.contains("answer") -> 
                    "\n\nWould you like me to explain any part of this solution in more detail?"
                aiReply.contains("equation") || aiReply.contains("formula") -> 
                    "\n\nWould you like to practice more problems like this?"
                else -> 
                    "\n\nIs there anything else you'd like to know about this topic?"
            }
            return aiReply + followUp
        }
        
        return aiReply
    }

    private fun setupSendButton(askMathlyText: TextView, promptGroup: View) {
        sendButton.setOnClickListener {
            val userInput = messageInput.text.toString().trim()
            sendUserMessage(userInput, askMathlyText, promptGroup)
        }
    }

    private fun setupSettingsPopup(view: View, promptGroup: View, askMathlyText: TextView) {
        val settingsIcon = view.findViewById<View>(R.id.chat_settings)
        settingsIcon.setOnClickListener { v ->
            val items = listOf(
                com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                    id = 1,
                    title = "New chat",
                    iconRes = R.drawable.ic_add,
                    onClick = {
                        chatAdapter.clearMessages()
                        messageInput.setText("")
                        chatAdapter.notifyDataSetChanged()
                        showWelcomeUI(promptGroup, askMathlyText)
                        // Reset conversation context for new chat
                        ConversationHandler.resetConversationContext()
                        true
                    }
                ),
                com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                    id = 2,
                    title = "Settings",
                    iconRes = R.drawable.ic_settings,
                    onClick = {
                        val intent = android.content.Intent(requireContext(), com.shaadow.onecalculator.SettingsActivity::class.java)
                        startActivity(intent)
                        true
                    }
                ),
                com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                    id = 3,
                    title = "About Us",
                    iconRes = R.drawable.ic_info,
                    onClick = {
                        val intent = android.content.Intent(requireContext(), com.shaadow.onecalculator.AboutUsActivity::class.java)
                        startActivity(intent)
                        true
                    }
                )
            )
            com.shaadow.onecalculator.utils.PopupMenuBuilder.show(requireContext(), v, items)
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

    override fun onCopy(messageId: String) {
        val message = chatAdapter.getMessageById(messageId)
        if (message != null) {
            try {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", message.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.os.Handler(requireContext().mainLooper).post {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Copy not allowed")
                        .setMessage("Copying to clipboard is restricted by your device or profile. This may happen on work profiles, secondary users, or some enterprise devices. Try on a personal device or profile if you need this feature.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
    override fun onLike(messageId: String) {
        val message = chatAdapter.getMessageById(messageId)
        message?.let {
            Toast.makeText(requireContext(), "Liked: ${it.text}", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDislike(messageId: String) {
        val message = chatAdapter.getMessageById(messageId)
        message?.let {
            Toast.makeText(requireContext(), "Disliked: ${it.text}", Toast.LENGTH_SHORT).show()
        }
    }
}
