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
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color
import android.widget.TextView
import android.widget.Button

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
                Toast.makeText(requireContext(), "Chip clicked: $chipText", Toast.LENGTH_SHORT).show()
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
        if (Validation.isGreeting(userInput)) {
            chatAdapter.addAIMessage("Hello! I'm Mathly. How can I help you with math today?")
            scrollToBottom()
            return
        }
        if (isSimpleMathExpression(userInput)) {
            try {
                val result = Expression.calculate(userInput)
                chatAdapter.addAIMessage("Result: $result")
            } catch (e: Exception) {
                chatAdapter.addAIMessage("Sorry, Mathly couldn't evaluate that expression.")
            }
            scrollToBottom()
            return
        }
        if (!Validation.isMathQuery(userInput)) {
            chatAdapter.addAIMessage("Sorry, Mathly can only help with math expressions and questions.")
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
                chatAdapter.updateLastAIMessage(aiReply.trim())
            } catch (e: Exception) {
                chatAdapter.updateLastAIMessage("Error connecting to AI: "+e.message)
            }
            scrollToBottom()
        }
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
        message?.let {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AI Message", it.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
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
