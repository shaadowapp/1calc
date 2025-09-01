package com.shaadow.onecalculator.mathly.logic

import java.util.*

object ConversationHandler {
    
    // Track conversation context
    private var conversationContext = mutableMapOf<String, Any>()
    private var userMood = "neutral"
    private var lastTopic = ""
    private var questionCount = 0
    
    // Common questions and their answers
    private val conversationQA = mapOf(
        // Identity and introduction questions
        "who are you" to "I'm Mathly, your AI math assistant! 🤖✨ I'm here to help you with all things mathematics - from basic calculations to complex problem solving. I can solve equations, explain concepts, and even create practice problems for you.",
        
        "what are you" to "I'm Mathly, an AI-powered math tutor and calculator. I can help you with arithmetic, algebra, geometry, calculus, statistics, and more. Think of me as your personal math teacher who's available 24/7! 📚🧮",
        
        "what is mathly" to "Mathly is an AI math assistant designed to make learning mathematics fun and accessible. I can solve problems, explain concepts, generate practice questions, and help you understand math in a clear, step-by-step way.",
        
        "who is mathly" to "Mathly is me! I'm your friendly AI math tutor. I was created to help students, teachers, and anyone who wants to learn or practice mathematics. I love making math less intimidating and more enjoyable! 😊",
        
        "what's your name" to "My name is Mathly! It's a combination of 'Math' and 'friendly' - because I'm here to make math friendly and approachable for everyone.",
        
        "what is your name" to "My name is Mathly! I'm your AI math assistant, ready to help you with any mathematical challenge.",
        
        // Capability questions
        "what can you do" to "I can do a lot! 🎯 I solve equations, calculate complex expressions, explain math concepts, create practice problems, help with homework, and even generate step-by-step solutions. I work with arithmetic, algebra, geometry, calculus, statistics, and more. Just ask me anything math-related!",
        
        "what do you do" to "I help people learn and solve mathematics! I can calculate, explain, teach, and even create custom math problems. Whether you need help with basic arithmetic or advanced calculus, I'm here to assist you.",
        
        "how do you work" to "I work by analyzing your math questions and providing accurate solutions with explanations. I can handle everything from simple calculations to complex mathematical concepts. Just type your question and I'll do my best to help!",
        
        "how does this work" to "It's simple! Just ask me any math question - like 'solve 2x + 5 = 15' or 'explain quadratic equations' - and I'll provide the answer with a clear explanation. I can also create practice problems if you want to test your skills!",
        
        // Self-description questions
        "tell me about yourself" to "I'm Mathly, your AI math companion! 🤖 I was designed to make mathematics accessible and enjoyable for everyone. I can solve problems, explain concepts, create practice questions, and help you build confidence in math. I'm patient, friendly, and always ready to help!",
        
        "introduce yourself" to "Hello! I'm Mathly, your AI math assistant! 👋 I'm here to help you with all things mathematics. Whether you're struggling with basic arithmetic or tackling advanced calculus, I'm ready to assist with clear explanations and step-by-step solutions. Let's make math fun together!",
        
        "explain yourself" to "I'm Mathly, an AI designed specifically for mathematics education. I can solve equations, explain concepts, generate practice problems, and help you understand math in a way that makes sense. I'm here to support your learning journey!",
        
        "describe yourself" to "I'm Mathly - friendly, knowledgeable, and passionate about mathematics! I can handle calculations, solve problems, explain concepts, and even create custom practice questions. I'm patient and always ready to help you succeed in math.",
        
        // Greeting responses
        "how are you" to "I'm doing great, thank you for asking! 😊 I'm always excited to help with mathematics. How can I assist you today?",
        
        "how are you doing" to "I'm functioning perfectly and ready to help with math! What would you like to work on today?",
        
        // Basic greetings
        "hi" to "Hello! 👋 I'm Mathly, your AI math assistant! I'm here to help you with all things mathematics. How can I assist you today?",
        
        "hello" to "Hi there! 👋 I'm Mathly, your friendly AI math tutor. I can help you solve problems, explain concepts, and make math fun! What would you like to work on?",
        
        "hey" to "Hey! 👋 I'm Mathly, ready to help with your math questions! Whether it's basic arithmetic or advanced calculus, I'm here to assist. What can I help you with?",
        
        "greetings" to "Greetings! 👋 I'm Mathly, your AI math companion. I'm excited to help you with mathematics today. What would you like to learn or solve?",
        
        "good morning" to "Good morning! ☀️ I'm Mathly, your AI math assistant. Ready to help you with mathematics today! What would you like to work on?",
        
        "good afternoon" to "Good afternoon! 🌤️ I'm Mathly, your AI math tutor. How can I help you with mathematics today?",
        
        "good evening" to "Good evening! 🌙 I'm Mathly, your AI math assistant. Ready to help you with any math questions you might have!",
        
        // Help and guidance questions
        "help me" to "I'd be happy to help! What math problem or concept would you like assistance with? I can help with calculations, explanations, practice problems, and more.",
        
        "i need help" to "I'm here to help! What math question or problem are you working on? Just describe what you need, and I'll do my best to assist you.",
        
        "can you help me" to "Absolutely! I'm here to help with any math-related questions or problems. What would you like to work on?",
        
        "what should i do" to "That depends on what you're trying to accomplish! Are you working on a specific math problem, studying for a test, or trying to understand a concept? Let me know what you're working on, and I'll guide you through it.",
        
        // Feature questions
        "what can you calculate" to "I can calculate almost anything mathematical! From basic arithmetic (addition, subtraction, multiplication, division) to complex operations like calculus, trigonometry, statistics, and more. Just give me the problem and I'll solve it!",
        
        "can you solve equations" to "Yes! I can solve all types of equations - linear, quadratic, cubic, systems of equations, and more. I'll show you the step-by-step solution so you can understand how it works.",
        
        "can you solve math questions" to "Absolutely! I can solve all kinds of math questions - from basic arithmetic to advanced calculus. I can handle equations, word problems, geometry, algebra, trigonometry, statistics, and more. Just give me the problem and I'll provide a clear solution with explanations!",
        
        "can you solve math problems" to "Yes! I'm designed specifically to solve math problems of all types and difficulty levels. Whether it's basic arithmetic, algebra, geometry, calculus, or statistics, I can help you find the solution and explain the process step by step.",
        
        "can you solve questions" to "Yes! I can solve mathematical questions and problems. I'm particularly good at math, but I can also help with logical reasoning and problem-solving approaches. What type of question do you have?",
        
        "do you know calculus" to "Absolutely! I'm well-versed in calculus including limits, derivatives, integrals, differential equations, and more. I can solve problems and explain the concepts clearly.",
        
        "can you help with algebra" to "Of course! Algebra is one of my specialties. I can help with solving equations, factoring, graphing, word problems, and all other algebra topics. What specific algebra problem are you working on?",
        
        // Encouragement and motivation
        "i'm bad at math" to "Don't worry! Everyone learns at their own pace, and math can be challenging. I'm here to help make it easier and more understandable. Let's start with something you're comfortable with and build from there. What would you like to work on?",
        
        "math is hard" to "Math can definitely be challenging, but you're not alone! I'm here to help break down complex concepts into simpler, more understandable parts. Let's tackle this together - what specific topic or problem is giving you trouble?",
        
        "i don't understand math" to "That's completely normal! Math can be confusing, but I'm here to help make it clearer. Let's start with the basics and work our way up. What specific concept or problem would you like me to explain?",
        
        // General questions
        "what is this" to "This is Mathly - your AI math assistant! I'm here to help you with mathematics, whether you need to solve problems, understand concepts, or practice your skills. How can I help you today?",
        
        "what's this" to "This is Mathly! I'm your friendly AI math tutor, ready to help you with any mathematical questions or problems. What would you like to work on?",
        
        "how do i use this" to "It's simple! Just ask me any math question or type a mathematical expression. I can solve equations, explain concepts, create practice problems, and help you learn. Try asking something like 'solve 2x + 5 = 15' or 'explain quadratic equations'!",
        
        // Fun and personality
        "are you smart" to "I'm quite good at mathematics! 🤓 I can handle complex calculations and explain concepts clearly. But the real goal is to help you become confident and skilled in math. Let's work together on that!",
        
        "are you intelligent" to "I'm designed to be knowledgeable about mathematics and help people learn. My intelligence is focused on making math accessible and understandable for everyone. What would you like to learn about?",
        
        "do you like math" to "I love mathematics! 🧮 It's fascinating how numbers, patterns, and logic come together to solve real-world problems. I'm excited to share that enthusiasm with you and help you discover the beauty of math!",
        
        "what do you like" to "I love mathematics and helping people learn! I enjoy solving complex problems, explaining concepts clearly, and seeing people gain confidence in their math skills. What's your favorite subject or topic?",
        
        // Technical questions
        "are you an ai" to "Yes, I'm an AI (Artificial Intelligence) specifically designed to help with mathematics. I can process mathematical expressions, solve problems, and provide explanations to help you learn and understand math concepts.",
        
        "are you real" to "I'm a real AI assistant, though I'm not a human. I'm a computer program designed to help with mathematics. I can solve problems, explain concepts, and assist with learning - all of which are very real and helpful!",
        
        "are you human" to "No, I'm not human - I'm an AI (Artificial Intelligence) designed to help with mathematics. But I'm here to provide real, helpful assistance with your math questions and problems!",
        
        // Encouragement
        "i'm stuck" to "Don't worry! Being stuck is part of learning. Let's work through this together. What problem or concept are you having trouble with? I'll help you understand it step by step.",
        
        "i don't get it" to "That's okay! Let me try to explain it in a different way. What specific part is confusing you? I'm here to help make it clearer.",
        
        "this doesn't make sense" to "I understand! Sometimes math can seem confusing. Let me break it down in a simpler way. What exactly doesn't make sense to you? I'll help clarify it.",
        
        // Quick suggestions and prompts
        "give me a problem" to "Sure! Here's a fun math problem for you:\n\n**Problem**: If a rectangle has a length of 8 units and a width of 6 units, what is its area and perimeter?\n\nTry solving it, and I'll help you check your answer! 📐",
        
        "create a problem" to "I'd love to create a problem for you! What topic would you like to practice?\n\n• Algebra (equations, inequalities)\n• Geometry (area, perimeter, volume)\n• Arithmetic (basic operations)\n• Calculus (derivatives, integrals)\n\nJust let me know your preference! 🎯",
        
        "test me" to "Great! Let's test your math skills! Here's a challenge:\n\n**Problem**: Solve for x: 3x + 7 = 22\n\nTake your time, and I'll help you work through it step by step! 💪",
        
        "practice problems" to "Excellent! Let's practice some problems together. What would you like to work on?\n\n• Basic arithmetic\n• Algebra equations\n• Geometry problems\n• Word problems\n• Calculus concepts\n\nPick a topic and I'll create some practice problems for you! 📚",
        
        "show me an example" to "Of course! Here's a clear example:\n\n**Example**: Solving a quadratic equation\n\nProblem: x² + 5x + 6 = 0\n\nStep 1: Identify a=1, b=5, c=6\nStep 2: Use the quadratic formula: x = (-b ± √(b²-4ac)) / 2a\nStep 3: Substitute: x = (-5 ± √(25-24)) / 2\nStep 4: Simplify: x = (-5 ± √1) / 2 = (-5 ± 1) / 2\nStep 5: Solutions: x = -2 or x = -3\n\nWould you like me to explain any of these steps in more detail? 🤓",
        
        "explain step by step" to "I love explaining things step by step! What concept or problem would you like me to break down for you?\n\nI can explain:\n• How to solve equations\n• Mathematical concepts\n• Problem-solving strategies\n• Calculation methods\n\nJust tell me what you'd like to understand better! 📖",
        
        // Positive reinforcement
        "thank you" to "You're very welcome! 😊 I'm glad I could help. Is there anything else you'd like to work on or learn about?",
        
        "thanks" to "You're welcome! I'm here whenever you need help with math. Feel free to ask more questions!",
        
        "good job" to "Thank you! I'm here to help you succeed in math. Keep up the great work, and don't hesitate to ask if you need more assistance!",
        
        "you're helpful" to "Thank you! That's exactly what I'm here for - to help make mathematics more accessible and enjoyable. I'm glad I can be of assistance! 😊",
        
        "you're awesome" to "Thank you so much! 😊 I'm here to make math awesome for you! What would you like to explore next?",
        
        "you're the best" to "That's very kind of you! I'm here to be the best math assistant I can be for you. Let's keep learning together! 🌟",
        
        // Learning preferences
        "i'm a beginner" to "Welcome to the wonderful world of mathematics! 🌟 Being a beginner is the perfect place to start. I'm here to help you build a strong foundation.\n\nLet's start with something comfortable. What basic math concept would you like to explore first?",
        
        "i'm advanced" to "Excellent! I love working with advanced students! 🚀 I can help you with complex problems, proofs, advanced calculus, and challenging mathematical concepts.\n\nWhat advanced topic would you like to tackle today?",
        
        "i'm studying for a test" to "Test preparation is important! 📝 I can help you review concepts, practice problems, and build confidence.\n\nWhat subject are you studying for? I can create practice problems and explain any concepts you need to review!",
        
        "i need homework help" to "Homework help is one of my specialties! 📚 I can guide you through problems, explain concepts, and help you understand the material without just giving you the answers.\n\nWhat homework problem are you working on? Let's tackle it together!",
        
        // Fun and engagement
        "tell me a math joke" to "Here's a math joke for you! 😄\n\nWhy was the math book sad?\nBecause it had too many problems!\n\n😆 Math can be fun! What would you like to work on today?",
        
        "math is boring" to "I understand that feeling! But math can actually be really exciting when you see how it connects to the real world. 🌍\n\nLet me show you some interesting applications:\n• How math helps build bridges and buildings\n• How it's used in video games and animations\n• How it helps predict weather patterns\n\nWhat interests you? I bet we can find a math topic that's fascinating! ✨",
        
        "make math fun" to "Absolutely! Let's make math fun together! 🎉\n\nWe can:\n• Solve puzzles and brain teasers\n• Explore math in games and sports\n• Discover patterns in nature\n• Create cool geometric designs\n\nWhat sounds interesting to you? Let's find the fun in math! 🎯",
        
        "i hate math" to "I hear you, and that's totally okay! Many people feel that way, often because of bad experiences. But here's the thing - math is just a tool, and I'm here to help you see it differently.\n\nLet's start with something that interests you. What do you enjoy doing? We can find math in almost anything! 🌟"
    )
    
    // Dynamic response templates
    private val dynamicResponses = mapOf(
        "greeting_followup" to listOf(
            "What math topic would you like to explore today?",
            "Is there a specific problem you're working on?",
            "Would you like to practice some calculations?",
            "What area of math interests you most?"
        ),
        "encouragement" to listOf(
            "You're doing great! Keep up the excellent work! 🌟",
            "Math is a journey, and you're making great progress! 📈",
            "Every problem you solve makes you stronger! 💪",
            "You've got this! I believe in you! 🎯",
            "Learning math takes time, and you're doing wonderfully! ⏰",
            "Your persistence is impressive! Keep going! 🔥",
            "You're becoming more confident with each question! 🚀",
            "Math skills are like muscles - they get stronger with practice! 💪",
            "You're asking great questions! That's how learning happens! 🤔",
            "I'm proud of your effort! Keep pushing forward! 👏"
        ),
        "topic_suggestions" to listOf(
            "How about we work on some algebra problems?",
            "Would you like to explore geometry concepts?",
            "Should we practice some calculus?",
            "What about some statistics problems?",
            "Let's try some trigonometry!",
            "How about basic arithmetic practice?"
        )
    )  
  
    /**
     * Check if the input is a conversational question that has a predefined answer
     */
    fun isConversationalQuestion(input: String): Boolean {
        val lowerInput = input.lowercase().trim()
        
        // Check for exact matches first
        if (conversationQA.containsKey(lowerInput)) {
            return true
        }
        
        // Check for partial matches with key words
        val keyWords = listOf(
            "hi", "hello", "hey", "greetings", "good morning", "good afternoon", "good evening",
            "who are you", "what are you", "what is mathly", "who is mathly",
            "what's your name", "what is your name", "how are you",
            "what can you do", "what do you do", "how do you work",
            "tell me about yourself", "introduce yourself", "explain yourself",
            "describe yourself", "help me", "i need help", "can you help",
            "what can you calculate", "can you solve", "do you know",
            "can you help with", "i'm bad at math", "math is hard",
            "i don't understand", "what is this", "what's this",
            "how do i use this", "are you smart", "are you intelligent",
            "do you like math", "what do you like", "are you an ai",
            "are you real", "are you human", "i'm stuck", "i don't get it",
            "this doesn't make sense", "thank you", "thanks", "good job",
            "you're helpful", "you're awesome", "you're the best",
            "give me a problem", "create a problem", "test me", "practice problems",
            "show me an example", "explain step by step", "i'm a beginner",
            "i'm advanced", "i'm studying for a test", "i need homework help",
            "tell me a math joke", "math is boring", "make math fun", "i hate math"
        )
        
        return keyWords.any { keyWord ->
            lowerInput.contains(keyWord.lowercase())
        }
    }
    
    /**
     * Get the appropriate conversational response with context awareness
     */
    fun getConversationalResponse(input: String): String? {
        val lowerInput = input.lowercase().trim()
        
        // Update conversation context
        updateConversationContext(input)
        
        // Check for exact matches first
        if (conversationQA.containsKey(lowerInput)) {
            val baseResponse = conversationQA[lowerInput]!!
            return addContextualFollowUp(baseResponse, input)
        }
        
        // Check for partial matches and return the most appropriate response
        val response = when {
            lowerInput.contains("hi") && !lowerInput.contains("high") ->
                conversationQA["hi"]
            
            lowerInput.contains("hello") ->
                conversationQA["hello"]
            
            lowerInput.contains("hey") ->
                conversationQA["hey"]
            
            lowerInput.contains("greetings") ->
                conversationQA["greetings"]
            
            lowerInput.contains("good morning") ->
                conversationQA["good morning"]
            
            lowerInput.contains("good afternoon") ->
                conversationQA["good afternoon"]
            
            lowerInput.contains("good evening") ->
                conversationQA["good evening"]
            
            lowerInput.contains("who are you") || lowerInput.contains("what are you") ->
                conversationQA["who are you"]
            
            lowerInput.contains("what is mathly") || lowerInput.contains("who is mathly") ->
                conversationQA["what is mathly"]
            
            lowerInput.contains("what's your name") || lowerInput.contains("what is your name") ->
                conversationQA["what's your name"]
            
            lowerInput.contains("how are you") ->
                conversationQA["how are you"]
            
            lowerInput.contains("what can you do") || lowerInput.contains("what do you do") ->
                conversationQA["what can you do"]
            
            lowerInput.contains("how do you work") || lowerInput.contains("how does this work") ->
                conversationQA["how do you work"]
            
            lowerInput.contains("tell me about yourself") || lowerInput.contains("introduce yourself") ->
                conversationQA["tell me about yourself"]
            
            lowerInput.contains("explain yourself") || lowerInput.contains("describe yourself") ->
                conversationQA["explain yourself"]
            
            lowerInput.contains("help me") || lowerInput.contains("i need help") || lowerInput.contains("can you help me") ->
                conversationQA["help me"]
            
            lowerInput.contains("what can you calculate") ->
                conversationQA["what can you calculate"]
            
            lowerInput.contains("can you solve") && lowerInput.contains("equation") ->
                conversationQA["can you solve equations"]
            
            lowerInput.contains("can you solve") && (lowerInput.contains("math") || lowerInput.contains("question") || lowerInput.contains("problem")) ->
                conversationQA["can you solve math questions"]
            
            lowerInput.contains("do you know calculus") ->
                conversationQA["do you know calculus"]
            
            lowerInput.contains("can you help with algebra") ->
                conversationQA["can you help with algebra"]
            
            lowerInput.contains("i'm bad at math") || lowerInput.contains("math is hard") ->
                conversationQA["i'm bad at math"]
            
            lowerInput.contains("i don't understand math") ->
                conversationQA["i don't understand math"]
            
            lowerInput.contains("what is this") || lowerInput.contains("what's this") ->
                conversationQA["what is this"]
            
            lowerInput.contains("how do i use this") ->
                conversationQA["how do i use this"]
            
            lowerInput.contains("are you smart") || lowerInput.contains("are you intelligent") ->
                conversationQA["are you smart"]
            
            lowerInput.contains("do you like math") ->
                conversationQA["do you like math"]
            
            lowerInput.contains("what do you like") ->
                conversationQA["what do you like"]
            
            lowerInput.contains("are you an ai") ->
                conversationQA["are you an ai"]
            
            lowerInput.contains("are you real") ->
                conversationQA["are you real"]
            
            lowerInput.contains("are you human") ->
                conversationQA["are you human"]
            
            lowerInput.contains("i'm stuck") ->
                conversationQA["i'm stuck"]
            
            lowerInput.contains("i don't get it") ->
                conversationQA["i don't get it"]
            
            lowerInput.contains("this doesn't make sense") ->
                conversationQA["this doesn't make sense"]
            
            lowerInput.contains("give me a problem") ->
                conversationQA["give me a problem"]
            
            lowerInput.contains("create a problem") ->
                conversationQA["create a problem"]
            
            lowerInput.contains("test me") ->
                conversationQA["test me"]
            
            lowerInput.contains("practice problems") ->
                conversationQA["practice problems"]
            
            lowerInput.contains("show me an example") ->
                conversationQA["show me an example"]
            
            lowerInput.contains("explain step by step") ->
                conversationQA["explain step by step"]
            
            lowerInput.contains("thank you") || lowerInput.contains("thanks") ->
                conversationQA["thank you"]
            
            lowerInput.contains("good job") ->
                conversationQA["good job"]
            
            lowerInput.contains("you're helpful") ->
                conversationQA["you're helpful"]
            
            lowerInput.contains("you're awesome") ->
                conversationQA["you're awesome"]
            
            lowerInput.contains("you're the best") ->
                conversationQA["you're the best"]
            
            lowerInput.contains("i'm a beginner") ->
                conversationQA["i'm a beginner"]
            
            lowerInput.contains("i'm advanced") ->
                conversationQA["i'm advanced"]
            
            lowerInput.contains("i'm studying for a test") ->
                conversationQA["i'm studying for a test"]
            
            lowerInput.contains("i need homework help") ->
                conversationQA["i need homework help"]
            
            lowerInput.contains("tell me a math joke") ->
                conversationQA["tell me a math joke"]
            
            lowerInput.contains("math is boring") ->
                conversationQA["math is boring"]
            
            lowerInput.contains("make math fun") ->
                conversationQA["make math fun"]
            
            lowerInput.contains("i hate math") ->
                conversationQA["i hate math"]
            
            else -> null
        }
        
        return response?.let { addContextualFollowUp(it, input) }
    }
    
    /**
     * Update conversation context based on user input
     */
    private fun updateConversationContext(input: String) {
        val lowerInput = input.lowercase()
        
        // Track question count
        questionCount++
        
        // Detect user mood
        when {
            lowerInput.contains("bad") || lowerInput.contains("hard") || lowerInput.contains("difficult") || 
            lowerInput.contains("confused") || lowerInput.contains("stuck") || lowerInput.contains("don't understand") ->
                userMood = "frustrated"
            
            lowerInput.contains("thank") || lowerInput.contains("good") || lowerInput.contains("great") ||
            lowerInput.contains("awesome") || lowerInput.contains("love") || lowerInput.contains("amazing") ->
                userMood = "positive"
            
            lowerInput.contains("hi") || lowerInput.contains("hello") || lowerInput.contains("hey") ->
                userMood = "friendly"
            
            else -> userMood = "neutral"
        }
        
        // Track topics
        when {
            lowerInput.contains("algebra") -> lastTopic = "algebra"
            lowerInput.contains("calculus") -> lastTopic = "calculus"
            lowerInput.contains("geometry") -> lastTopic = "geometry"
            lowerInput.contains("trigonometry") -> lastTopic = "trigonometry"
            lowerInput.contains("statistics") -> lastTopic = "statistics"
            lowerInput.contains("probability") -> lastTopic = "probability"
            lowerInput.contains("equation") -> lastTopic = "equations"
            lowerInput.contains("derivative") -> lastTopic = "calculus"
            lowerInput.contains("integral") -> lastTopic = "calculus"
            lowerInput.contains("matrix") -> lastTopic = "linear algebra"
            else -> {
                // Keep existing topic if no new topic detected
            }
        }
        
        // Store conversation context
        conversationContext["lastInput"] = input
        conversationContext["questionCount"] = questionCount
        conversationContext["userMood"] = userMood
        conversationContext["lastTopic"] = lastTopic
    }
    
    /**
     * Add contextual follow-up to responses
     */
    private fun addContextualFollowUp(baseResponse: String, input: String): String {
        val followUp = when {
            questionCount == 1 -> {
                "\n\n" + dynamicResponses["greeting_followup"]?.random()
            }
            userMood == "frustrated" -> {
                "\n\n" + getRandomEncouragement()
            }
            baseResponse.contains("solve") || baseResponse.contains("calculate") -> {
                if (questionCount > 3) {
                    "\n\n" + getPersonalizedFollowUp()
                } else ""
            }
            else -> ""
        }
        
        return baseResponse + (followUp ?: "")
    }
    
    /**
     * Get personalized follow-up based on conversation context
     */
    private fun getPersonalizedFollowUp(): String {
        return when {
            lastTopic.isNotEmpty() -> "Would you like to continue working on $lastTopic, or shall we explore something else?"
            userMood == "frustrated" -> "Let's take it step by step. What specific part would you like to focus on?"
            userMood == "positive" -> "Great! What would you like to work on next?"
            else -> "What would you like to explore or practice?"
        }
    }
    
    /**
     * Get a random encouraging message
     */
    fun getRandomEncouragement(): String {
        return dynamicResponses["encouragement"]?.random() ?: "You're doing great! Keep up the excellent work! 🌟"
    }
    
    /**
     * Get a welcome message for new users
     */
    fun getWelcomeMessage(): String {
        return "Hello! I'm Mathly, your AI math assistant! 🤖✨\n\n" +
               "I'm here to help you with all things mathematics - from basic calculations to complex problem solving. " +
               "I can solve equations, explain concepts, create practice problems, and help you understand math step by step.\n\n" +
               "Just ask me anything math-related, and I'll do my best to help! What would you like to work on today?"
    }
    
    /**
     * Reset conversation context (for new chat)
     */
    fun resetConversationContext() {
        conversationContext.clear()
        userMood = "neutral"
        lastTopic = ""
        questionCount = 0
    }
    
    /**
     * Get conversation statistics
     */
    fun getConversationStats(): Map<String, Any> {
        return mapOf(
            "questionCount" to questionCount,
            "userMood" to userMood,
            "lastTopic" to lastTopic,
            "contextSize" to conversationContext.size
        )
    }
}