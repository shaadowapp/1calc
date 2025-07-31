package com.shaadow.onecalculator.mathly.logic

object ContentFilter {
    
    // Comprehensive blocked words and phrases
    private val blockedWords = setOf(
        // Profanity and offensive terms
        "fuck", "shit", "bitch", "ass", "damn", "hell", "crap", "piss", "dick", "cock", "pussy", "cunt",
        "whore", "slut", "bastard", "motherfucker", "fucker", "shitty", "fucking", "bitchy", "asshole",
        "dumbass", "jackass", "smartass", "badass", "hardass", "fatass", "dumbass", "jackass",
        "douche", "douchebag", "twat", "wanker", "wank", "jerk", "jerkoff", "jerk off", "fag", "faggot",
        "queer", "dyke", "lesbo", "retard", "retarded", "spastic", "mong", "idiot", "moron", "stupid",
        "dumb", "retarded", "imbecile", "cretin", "numbskull", "bonehead", "airhead", "dipshit",
        
        // Sexual content and explicit terms
        "sex", "sexual", "porn", "pornography", "nude", "naked", "penis", "vagina", "breast", "boob",
        "dick", "cock", "pussy", "cunt", "fuck", "fucking", "suck", "sucking", "blow", "blowing",
        "cum", "sperm", "ejaculate", "masturbate", "masturbation", "orgasm", "erection", "hard",
        "horny", "sexy", "hot", "attractive", "beautiful", "handsome", "cute", "hot", "sexy",
        "nudes", "nudity", "strip", "stripping", "lap dance", "lapdance", "escort", "prostitute",
        "hooker", "call girl", "callgirl", "whore", "slut", "bimbo", "skank", "ho", "hoe",
        "tits", "titties", "boobs", "breasts", "nipple", "nipples", "clit", "clitoris", "anus",
        "butthole", "asshole", "butt", "ass", "booty", "thigh", "thighs", "leg", "legs", "foot",
        "feet", "toe", "toes", "finger", "fingers", "hand", "hands", "mouth", "lips", "tongue",
        "kiss", "kissing", "lick", "licking", "suck", "sucking", "blow", "blowing", "ride", "riding",
        
        // Violence, threats, and harmful content
        "kill", "killing", "murder", "death", "dead", "die", "dying", "suicide", "bomb", "explode",
        "shoot", "shooting", "gun", "weapon", "knife", "blood", "bloody", "hurt", "pain", "torture",
        "abuse", "abusive", "violent", "violence", "fight", "fighting", "attack", "attacking",
        "assault", "rape", "raping", "rapist", "pedophile", "pedo", "child molester", "molest",
        "molesting", "abuse", "abusing", "beating", "beat", "punch", "punching", "kick", "kicking",
        "slap", "slapping", "hit", "hitting", "stab", "stabbing", "cut", "cutting", "burn", "burning",
        "strangle", "strangling", "choke", "choking", "drown", "drowning", "hang", "hanging",
        "poison", "poisoning", "overdose", "overdosing", "drug", "drugs", "cocaine", "heroin",
        "marijuana", "weed", "alcohol", "beer", "wine", "drunk", "high", "stoned", "addict",
        "addiction", "smoke", "smoking", "cigarette", "tobacco", "nicotine", "meth", "crystal",
        "ecstasy", "mdma", "lsd", "acid", "mushroom", "mushrooms", "psilocybin", "ketamine",
        "roofie", "rohypnol", "ghb", "date rape", "daterape", "roofie", "rohypnol",
        
        // Hate speech and discrimination
        "nigger", "nigga", "faggot", "fag", "dyke", "lesbo", "retard", "retarded", "spic", "kike",
        "chink", "gook", "towelhead", "sandnigger", "beaner", "wetback", "cracker", "honky",
        "whitey", "redneck", "hillbilly", "trailer trash", "trailer trash", "white trash",
        "nazi", "hitler", "holocaust", "genocide", "ethnic cleansing", "racial", "racist",
        "racism", "bigot", "bigotry", "prejudice", "discrimination", "segregation", "apartheid",
        "supremacist", "supremacy", "white supremacy", "black supremacy", "aryan", "aryan nation",
        "kkk", "ku klux klan", "skinhead", "neo nazi", "neonazi", "fascist", "fascism",
        
        // Cyberbullying and harassment
        "bully", "bullying", "harass", "harassment", "stalk", "stalking", "creep", "creepy",
        "stalker", "harasser", "bully", "bullying", "cyberbully", "cyberbullying", "troll",
        "trolling", "hate", "hating", "hater", "haters", "toxic", "toxicity", "triggered",
        "snowflake", "libtard", "conservatard", "republican", "democrat", "liberal", "conservative",
        "political", "politics", "election", "vote", "voting", "president", "trump", "biden",
        
        // Common variations and misspellings
        "fuk", "fck", "shyt", "sh1t", "b1tch", "b!tch", "d!ck", "c0ck", "p*ssy", "c*nt", "wh0re",
        "sl*t", "b@stard", "m0therfucker", "f*cker", "sh!tty", "f*cking", "b!tchy", "a\$hole",
        "dumb@ss", "j@ckass", "sm@rtass", "b@dass", "h@rdass", "f@tass", "f*ck", "f*cking",
        "sh*t", "b*tch", "d*ck", "c*ck", "p*ssy", "c*nt", "wh*re", "sl*t", "b*stard",
        "m*therfucker", "f*cker", "sh*tty", "b*tchy", "a*shole", "dumb*ss", "j*ckass",
        "sm*rtass", "b*dass", "h*rdass", "f*tass", "fuk", "fck", "shyt", "sh1t", "b1tch",
        "b!tch", "d!ck", "c0ck", "p*ssy", "c*nt", "wh0re", "sl*t", "b@stard", "m0therfucker",
        "f*cker", "sh!tty", "f*cking", "b!tchy", "a\$hole", "dumb@ss", "j@ckass", "sm@rtass",
        "b@dass", "h@rdass", "f@tass", "f*ck", "f*cking", "sh*t", "b*tch", "d*ck", "c*ck",
        "p*ssy", "c*nt", "wh*re", "sl*t", "b*stard", "m*therfucker", "f*cker", "sh*tty",
        "b*tchy", "a*shole", "dumb*ss", "j*ckass", "sm*rtass", "b*dass", "h*rdass", "f*tass"
    )
    
    // Enhanced blocked phrases with more comprehensive patterns
    private val blockedPhrases = setOf(
        // Sexual phrases and requests
        "send nudes", "send me nudes", "show me your body", "show me your private", "take off your clothes",
        "get naked", "get undressed", "strip for me", "dance for me", "touch yourself",
        "play with yourself", "masturbate", "jerk off", "jack off", "wank", "wanking",
        "fuck you", "fuck off", "go fuck yourself", "suck my dick", "suck my cock",
        "blow me", "give me head", "eat me out", "lick me", "ride me", "fuck me",
        "have sex", "make love", "sleep with", "hook up", "one night stand",
        "send pics", "send photos", "nude pics", "nude photos", "sexy pics", "hot pics",
        "show skin", "flash me", "expose yourself", "undress for me", "get horny",
        "turn me on", "make me hard", "make me wet", "sexual fantasy", "dirty talk",

        // Violent and threatening phrases
        "kill yourself", "kill you", "kill them", "kill him", "kill her", "commit suicide",
        "end your life", "take your life", "hurt yourself", "hurt you", "hurt them",
        "beat you up", "beat them up", "punch you", "punch them", "kick you", "kick them",
        "stab you", "stab them", "shoot you", "shoot them", "bomb you", "bomb them",
        "rape you", "rape them", "assault you", "assault them", "molest you", "molest them",
        "torture you", "torture them", "murder you", "murder them", "execute you", "execute them",
        "hang yourself", "hang you", "drown yourself", "drown you", "poison yourself", "poison you",

        // Enhanced threatening phrases with variations
        "i will kill you", "i will hurt you", "i will beat you", "i will punch you",
        "i will kick you", "i will stab you", "i will shoot you", "i will bomb you",
        "i will rape you", "i will assault you", "i will molest you", "i will stalk you",
        "i will harass you", "i will bully you", "i will troll you", "i will hate you",
        "you should die", "you should kill yourself", "you should end your life",
        "you deserve to die", "you deserve to be hurt", "you deserve to be beaten",
        "gonna kill you", "gonna hurt you", "gonna beat you", "gonna shoot you",
        "want to kill", "want to hurt", "want to beat", "want to shoot",

        // Hate speech and discrimination phrases
        "i hate", "i hate you", "i hate them", "i hate him", "i hate her", "i hate blacks",
        "i hate whites", "i hate asians", "i hate hispanics", "i hate jews", "i hate muslims",
        "i hate gays", "i hate lesbians", "i hate trans", "i hate disabled", "i hate retards",
        "go back to", "go back to your country", "go back to africa", "go back to mexico",
        "go back to china", "go back to india", "go back to pakistan", "go back to iran",
        "you people", "your kind", "your race", "your religion", "dirty immigrant",
        "illegal alien", "terrorist", "extremist", "radical", "savage", "primitive",

        // Enhanced cyberbullying phrases
        "you are stupid", "you are dumb", "you are retarded", "you are an idiot",
        "you are a moron", "you are worthless", "you are useless", "you are pathetic",
        "you are disgusting", "you are ugly", "you are fat", "you are skinny",
        "you are short", "you are tall", "you are weak", "you are a loser",
        "nobody likes you", "everyone hates you", "you have no friends",
        "you are alone", "you are lonely", "you are sad", "you are depressed",
        "you suck", "you fail", "you're trash", "you're garbage", "you're nothing",
        "go die", "drop dead", "get lost", "shut up", "fuck off",

        // Inappropriate personal requests
        "show me your", "send me your", "take a picture of yourself", "take a photo of yourself",
        "record yourself", "video yourself", "stream yourself", "go live for me",
        "meet me", "come over", "visit me", "stay with me", "sleep with me",
        "date me", "marry me", "be my girlfriend", "be my boyfriend",
        "be my wife", "be my husband", "be my lover", "be my partner",
        "what do you look like", "describe your body", "how old are you",
        "where do you live", "what's your address", "send location",

        // Drug and substance abuse references
        "buy drugs", "sell drugs", "get high", "smoke weed", "do cocaine",
        "take pills", "overdose", "drug dealer", "drug deal", "get stoned",
        "smoke crack", "inject heroin", "snort cocaine", "pop pills", "get drunk",

        // Self-harm and suicide references
        "cut myself", "hurt myself", "harm myself", "end it all", "can't go on",
        "want to die", "wish i was dead", "life is pointless", "no reason to live",
        "suicide methods", "how to kill", "ways to die", "painless death"
    )
    
    // Enhanced blocked patterns (regex) with better detection
    private val blockedPatterns = listOf(
        // Profanity patterns with character substitutions
        Regex("\\b\\w*[f]+[u@0]+[c]+[k]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[s$5]+[h]+[i1!]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[b]+[i1!]+[t]+[c]+[h]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[d]+[i1!]+[c]+[k]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[c]+[o0]+[c]+[k]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[p]+[u@]+[s$5]+[s$5]+[y]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[c]+[u@]+[n]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[w]+[h]+[o0]+[r]+[e3]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[s$5]+[l]+[u@]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),

        // Hate speech patterns
        Regex("\\b\\w*[n]+[i1!]+[g]+[g]+[e3]+[r]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[f]+[a@4]+[g]+[g]+[o0]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[r]+[e3]+[t]+[a@4]+[r]+[d]+\\w*\\b", RegexOption.IGNORE_CASE),

        // Leetspeak and character substitution patterns
        Regex("\\bf[u@0*]+ck\\b", RegexOption.IGNORE_CASE),
        Regex("\\bs[h]*[i1!*]+t\\b", RegexOption.IGNORE_CASE),
        Regex("\\bb[i1!*]+tch\\b", RegexOption.IGNORE_CASE),
        Regex("\\bd[i1!*]+ck\\b", RegexOption.IGNORE_CASE),
        Regex("\\ba[s$5*]+[s$5*]+h[o0*]+l[e3*]+\\b", RegexOption.IGNORE_CASE),

        // Violence patterns
        Regex("\\bk[i1!*]+ll\\s+(you|yourself|them|him|her)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(go\\s+)?d[i1!*]+e\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsu[i1!*]+c[i1!*]+de\\b", RegexOption.IGNORE_CASE),

        // Sexual content patterns
        Regex("\\bs[e3*]+nd\\s+n[u@*]+d[e3*]+s\\b", RegexOption.IGNORE_CASE),
        Regex("\\bs[e3*]+x[yu@*]+[a@*]+l\\b", RegexOption.IGNORE_CASE),
        Regex("\\bp[o0*]+rn\\b", RegexOption.IGNORE_CASE),

        // Repeated characters (often used to bypass filters)
        Regex("\\b\\w*([a-z])\\1{2,}\\w*\\b", RegexOption.IGNORE_CASE),

        // Common bypass attempts with spaces or symbols
        Regex("\\bf\\s*u\\s*c\\s*k\\b", RegexOption.IGNORE_CASE),
        Regex("\\bs\\s*h\\s*i\\s*t\\b", RegexOption.IGNORE_CASE),
        Regex("\\bb\\s*i\\s*t\\s*c\\s*h\\b", RegexOption.IGNORE_CASE)
    )
    
    // Context-specific blocked content for math-related inappropriate requests
    private val mathContextBlocked = setOf(
        // Inappropriate math-related requests
        "calculate my body", "solve my body", "find my measurements", "calculate my size",
        "math my body", "equation for my body", "formula for my body", "geometry of my body",
        "calculate age for dating", "solve for illegal", "math for drugs", "calculate drug",
        "equation for violence", "formula for harm", "geometry of weapons", "calculate damage",
        "solve for death", "math for suicide", "calculate pain", "formula for torture",
        "statistics for hate", "probability of violence", "calculate revenge", "solve for murder",

        // Inappropriate use of math terms for sexual content
        "integrate my body", "derive my curves", "calculate my assets", "solve my figure",
        "find my dimensions", "measure my body", "calculate my proportions", "geometry of curves",
        "trigonometry of body", "algebra of attraction", "calculus of desire", "statistics of beauty",

        // Attempts to use math context for inappropriate requests
        "math homework about sex", "calculate sexual", "solve for sexual", "equation for sex",
        "formula for sexual", "geometry of sexual", "trigonometry of sexual", "algebra of sex",
        "calculus of sex", "statistics of sex", "probability of sex", "math about bodies",

        // Drug-related math requests
        "calculate dosage", "solve for drugs", "math for dealing", "equation for high",
        "formula for overdose", "calculate addiction", "solve for substance", "math for pills",

        // Violence-related math requests
        "calculate force to hurt", "solve for damage", "math for weapons", "equation for violence",
        "formula for harm", "calculate impact damage", "solve for injury", "math for fighting"
    )
    
    /**
     * Check if the input contains inappropriate content with improved performance and accuracy
     */
    fun containsInappropriateContent(input: String): Boolean {
        if (input.isBlank()) return false

        val lowerInput = input.lowercase().trim()

        // Early exit for very short inputs that are likely safe
        if (lowerInput.length < 3) return false

        // Quick check for blocked words using optimized approach
        val hasBlockedWords = checkBlockedWords(lowerInput)
        if (hasBlockedWords) return true

        // Check for blocked phrases with fuzzy matching
        val hasBlockedPhrases = checkBlockedPhrases(lowerInput)
        if (hasBlockedPhrases) return true

        // Check for blocked patterns (regex)
        val hasBlockedPatterns = blockedPatterns.any { pattern ->
            pattern.containsMatchIn(input)
        }
        if (hasBlockedPatterns) return true

        // Check for context-specific blocked content
        val hasContextBlocked = checkMathContextBlocked(lowerInput)
        if (hasContextBlocked) return true

        // Additional checks for sophisticated bypass attempts
        val hasBypassAttempts = checkBypassAttempts(lowerInput)

        return hasBypassAttempts
    }

    /**
     * Optimized blocked words checking with exact and partial matching
     */
    private fun checkBlockedWords(lowerInput: String): Boolean {
        // Split input into words, removing punctuation and numbers
        val words = lowerInput.split(Regex("[^a-z]+")).filter { it.length > 2 }

        return words.any { word ->
            // Exact match check (fastest)
            if (blockedWords.contains(word)) return@any true

            // Partial match check for words containing blocked terms
            blockedWords.any { blocked ->
                word.contains(blocked) && word.length <= blocked.length + 3
            }
        }
    }

    /**
     * Enhanced phrase checking with fuzzy matching for common misspellings
     */
    private fun checkBlockedPhrases(lowerInput: String): Boolean {
        return blockedPhrases.any { phrase ->
            // Direct substring check (fastest)
            if (lowerInput.contains(phrase)) return@any true

            // Fuzzy matching for phrases with character substitutions
            val fuzzyPhrase = phrase
                .replace("o", "[o0]")
                .replace("i", "[i1!]")
                .replace("e", "[e3]")
                .replace("a", "[a@4]")
                .replace("s", "[s$5]")

            val pattern = Regex("\\b${fuzzyPhrase}\\b", RegexOption.IGNORE_CASE)
            pattern.containsMatchIn(lowerInput)
        }
    }

    /**
     * Check for math context-specific inappropriate content
     */
    private fun checkMathContextBlocked(lowerInput: String): Boolean {
        return mathContextBlocked.any { phrase ->
            lowerInput.contains(phrase)
        }
    }

    /**
     * Check for sophisticated bypass attempts
     */
    private fun checkBypassAttempts(lowerInput: String): Boolean {
        // Check for excessive character repetition (often used to bypass filters)
        val hasExcessiveRepetition = Regex("([a-z])\\1{3,}").containsMatchIn(lowerInput)
        if (hasExcessiveRepetition) {
            // Remove repeated characters and check again
            val normalized = lowerInput.replace(Regex("([a-z])\\1+"), "$1")
            return checkBlockedWords(normalized) || checkBlockedPhrases(normalized)
        }

        // Check for spaced-out words (e.g., "f u c k")
        val spacedPattern = Regex("\\b[a-z](?:\\s+[a-z]){2,}\\b")
        val spacedMatches = spacedPattern.findAll(lowerInput)
        for (match in spacedMatches) {
            val compressed = match.value.replace("\\s+".toRegex(), "")
            if (blockedWords.contains(compressed)) return true
        }

        // Check for mixed case and symbol substitutions
        val symbolSubstituted = lowerInput
            .replace("@", "a")
            .replace("3", "e")
            .replace("1", "i")
            .replace("!", "i")
            .replace("0", "o")
            .replace("$", "s")
            .replace("5", "s")
            .replace("7", "t")
            .replace("+", "t")

        if (symbolSubstituted != lowerInput) {
            return checkBlockedWords(symbolSubstituted) || checkBlockedPhrases(symbolSubstituted)
        }

        return false
    }
    
    /**
     * Get a filtered response message
     */
    fun getFilteredMessage(input: String): String {
        if (!containsInappropriateContent(input)) {
            return input
        }
        
        return "I'm sorry, but I cannot process messages containing inappropriate content. " +
               "Please keep our conversation focused on mathematics and educational topics. " +
               "I'm here to help you learn and solve math problems in a respectful environment."
    }
    
    /**
     * Get a more detailed and contextual response based on the type of content detected
     */
    fun getDetailedFilteredMessage(input: String): String {
        if (!containsInappropriateContent(input)) {
            return input
        }

        val lowerInput = input.lowercase()
        val severity = getSeverityLevel(input)

        return when {
            // High severity - violence, self-harm, threats
            severity >= 4 -> {
                when {
                    lowerInput.contains("kill") || lowerInput.contains("suicide") || lowerInput.contains("death") ->
                        "I'm concerned about this message. If you're having thoughts of self-harm, please reach out to a mental health professional or call a crisis hotline immediately. I'm here to help with math problems in a safe, supportive environment."

                    lowerInput.contains("rape") || lowerInput.contains("assault") || lowerInput.contains("molest") ->
                        "I cannot and will not process messages containing references to violence or assault. Please keep our conversation respectful and focused on mathematics."

                    containsHateSpeech(lowerInput) ->
                        "I cannot process messages with hate speech or discriminatory language. Everyone deserves respect regardless of their background. Let's focus on learning mathematics together."

                    else ->
                        "This message contains severely inappropriate content that I cannot process. Please keep our conversation respectful and focused on mathematics."
                }
            }

            // Medium severity - sexual content, profanity
            severity == 3 -> {
                when {
                    containsSexualContent(lowerInput) ->
                        "I'm designed to help with mathematics, not discuss sexual topics. Please keep our conversation educational and appropriate. What math problem can I help you with?"

                    containsProfanity(lowerInput) ->
                        "I understand you might be frustrated, but please use respectful language. I'm here to help you with math problems - what would you like to work on?"

                    else ->
                        "This message contains inappropriate content. Please rephrase your question in a respectful way, and I'll be happy to help with your math needs."
                }
            }

            // Lower severity - mild inappropriate content
            severity <= 2 -> {
                when {
                    checkMathContextBlocked(lowerInput) ->
                        "I notice you're trying to use math terms inappropriately. I'm here to help with legitimate mathematical problems and concepts. What math topic would you like to explore?"

                    containsBullying(lowerInput) ->
                        "Please be kind and respectful in our conversation. I'm here to help you learn mathematics in a positive environment. What math problem can I assist you with?"

                    else ->
                        "I'd prefer to keep our conversation focused on mathematics and learning. What math problem or concept would you like help with today?"
                }
            }

            else ->
                "I'm sorry, but I cannot process this message. Please keep our conversation focused on mathematics and educational topics. I'm here to help you learn and solve math problems!"
        }
    }

    /**
     * Helper function to detect hate speech
     */
    private fun containsHateSpeech(input: String): Boolean {
        val hateSpeechTerms = listOf("nigger", "faggot", "retard", "kike", "chink", "spic", "towelhead")
        return hateSpeechTerms.any { input.contains(it) }
    }

    /**
     * Helper function to detect sexual content
     */
    private fun containsSexualContent(input: String): Boolean {
        val sexualTerms = listOf("sex", "porn", "nude", "naked", "sexual", "masturbate", "orgasm")
        return sexualTerms.any { input.contains(it) }
    }

    /**
     * Helper function to detect profanity
     */
    private fun containsProfanity(input: String): Boolean {
        val profanityTerms = listOf("fuck", "shit", "bitch", "damn", "ass", "bastard")
        return profanityTerms.any { input.contains(it) }
    }

    /**
     * Helper function to detect bullying language
     */
    private fun containsBullying(input: String): Boolean {
        val bullyingTerms = listOf("stupid", "idiot", "moron", "loser", "worthless", "pathetic")
        return bullyingTerms.any { input.contains(it) }
    }
    
    /**
     * Check if input contains only math-related content
     */
    fun isMathOnly(input: String): Boolean {
        val mathPattern = Regex("^[\\d+\\-*/%^().!√\\s\\w]+$")
        return mathPattern.matches(input.trim())
    }
    
    /**
     * Get severity level of inappropriate content (0-5, 5 being most severe)
     * 0 = Clean content
     * 1 = Mild inappropriate content
     * 2 = Moderate inappropriate content
     * 3 = Serious inappropriate content
     * 4 = Severe inappropriate content
     * 5 = Extremely severe content (violence, threats, extreme hate speech)
     */
    fun getSeverityLevel(input: String): Int {
        if (!containsInappropriateContent(input)) return 0

        val lowerInput = input.lowercase()

        // Level 5: Extremely severe - violence, threats, self-harm
        val level5Terms = listOf(
            "kill yourself", "commit suicide", "end your life", "kill you", "murder", "rape",
            "assault", "molest", "torture", "bomb", "shoot", "stab", "hang", "drown", "poison"
        )
        if (level5Terms.any { lowerInput.contains(it) }) return 5

        // Level 4: Severe - hate speech, extreme discrimination
        val level4Terms = listOf(
            "nigger", "faggot", "kike", "chink", "spic", "towelhead", "sandnigger",
            "i will kill", "i will hurt", "you should die", "go back to your country"
        )
        if (level4Terms.any { lowerInput.contains(it) }) return 4

        // Level 3: Serious - sexual content, strong profanity
        val level3Terms = listOf(
            "fuck", "fucking", "motherfucker", "sex", "sexual", "porn", "nude", "naked",
            "masturbate", "orgasm", "send nudes", "show me your", "suck my"
        )
        if (level3Terms.any { lowerInput.contains(it) }) return 3

        // Level 2: Moderate - mild profanity, bullying
        val level2Terms = listOf(
            "shit", "bitch", "damn", "hell", "ass", "bastard", "stupid", "idiot",
            "moron", "retard", "you suck", "you're trash", "loser"
        )
        if (level2Terms.any { lowerInput.contains(it) }) return 2

        // Level 1: Mild - context-inappropriate or borderline content
        val level1Terms = listOf(
            "crap", "piss", "dumb", "shut up", "get lost"
        )
        if (level1Terms.any { lowerInput.contains(it) }) return 1

        // Check for math context violations
        if (checkMathContextBlocked(lowerInput)) return 2

        // Check for bypass attempts
        if (checkBypassAttempts(lowerInput)) return 3

        // Default for any other detected inappropriate content
        return 1
    }

    /**
     * Get educational suggestions when inappropriate content is detected
     */
    fun getEducationalSuggestions(): String {
        val suggestions = listOf(
            "Try asking: 'Solve 2x + 5 = 15'",
            "Ask me: 'What is the derivative of x²?'",
            "Try: 'Explain the Pythagorean theorem'",
            "Ask: 'How do I factor quadratic equations?'",
            "Try: 'Calculate the area of a circle with radius 5'",
            "Ask me: 'What is calculus used for?'",
            "Try: 'Help me with algebra basics'",
            "Ask: 'Explain trigonometric functions'"
        )

        return "\n\nHere are some math topics I can help with:\n• ${suggestions.random()}\n• ${suggestions.random()}"
    }

    /**
     * Check if content is likely a legitimate math question that was incorrectly flagged
     */
    fun isLikelyFalsePositive(input: String): Boolean {
        val lowerInput = input.lowercase()

        // Check for legitimate math terms that might be flagged
        val mathTerms = listOf(
            "sin", "cos", "tan", "log", "ln", "sqrt", "integral", "derivative",
            "equation", "formula", "calculate", "solve", "find", "what is",
            "how to", "explain", "help", "algebra", "geometry", "calculus"
        )

        val hasMathTerms = mathTerms.any { lowerInput.contains(it) }
        val severity = getSeverityLevel(input)

        // If it has math terms and low severity, it might be a false positive
        return hasMathTerms && severity <= 2
    }

    /**
     * Get a comprehensive content analysis report (for debugging/logging)
     */
    fun getContentAnalysis(input: String): Map<String, Any> {
        return mapOf(
            "input_length" to input.length,
            "contains_inappropriate" to containsInappropriateContent(input),
            "severity_level" to getSeverityLevel(input),
            "likely_false_positive" to isLikelyFalsePositive(input),
            "blocked_words_detected" to checkBlockedWords(input.lowercase()),
            "blocked_phrases_detected" to checkBlockedPhrases(input.lowercase()),
            "math_context_violation" to checkMathContextBlocked(input.lowercase()),
            "bypass_attempts_detected" to checkBypassAttempts(input.lowercase())
        )
    }
}