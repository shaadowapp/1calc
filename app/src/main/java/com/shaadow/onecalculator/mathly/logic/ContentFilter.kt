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
    
    // Blocked phrases and patterns
    private val blockedPhrases = setOf(
        // Sexual phrases
        "send nudes", "send me nudes", "show me your", "take off your", "take your clothes off",
        "get naked", "get undressed", "strip for me", "dance for me", "touch yourself",
        "play with yourself", "masturbate", "jerk off", "jack off", "wank", "wanking",
        "fuck you", "fuck off", "go fuck yourself", "suck my dick", "suck my cock",
        "blow me", "give me head", "eat me out", "lick me", "ride me", "fuck me",
        "have sex", "make love", "sleep with", "hook up", "one night stand",
        
        // Violent phrases
        "kill yourself", "kill you", "kill them", "kill him", "kill her", "commit suicide",
        "end your life", "take your life", "hurt yourself", "hurt you", "hurt them",
        "beat you up", "beat them up", "punch you", "punch them", "kick you", "kick them",
        "stab you", "stab them", "shoot you", "shoot them", "bomb you", "bomb them",
        "rape you", "rape them", "assault you", "assault them", "molest you", "molest them",
        
        // Threatening phrases
        "i will kill you", "i will hurt you", "i will beat you", "i will punch you",
        "i will kick you", "i will stab you", "i will shoot you", "i will bomb you",
        "i will rape you", "i will assault you", "i will molest you", "i will stalk you",
        "i will harass you", "i will bully you", "i will troll you", "i will hate you",
        "you should die", "you should kill yourself", "you should end your life",
        "you deserve to die", "you deserve to be hurt", "you deserve to be beaten",
        
        // Hate speech phrases
        "i hate", "i hate you", "i hate them", "i hate him", "i hate her", "i hate blacks",
        "i hate whites", "i hate asians", "i hate hispanics", "i hate jews", "i hate muslims",
        "i hate gays", "i hate lesbians", "i hate trans", "i hate disabled", "i hate retards",
        "go back to", "go back to your country", "go back to africa", "go back to mexico",
        "go back to china", "go back to india", "go back to pakistan", "go back to iran",
        
        // Cyberbullying phrases
        "you are stupid", "you are dumb", "you are retarded", "you are an idiot",
        "you are a moron", "you are worthless", "you are useless", "you are pathetic",
        "you are disgusting", "you are ugly", "you are fat", "you are skinny",
        "you are short", "you are tall", "you are weak", "you are strong",
        "nobody likes you", "everyone hates you", "you have no friends",
        "you are alone", "you are lonely", "you are sad", "you are depressed",
        
        // Inappropriate requests
        "show me your", "send me your", "take a picture of", "take a photo of",
        "record yourself", "video yourself", "stream yourself", "go live",
        "meet me", "come over", "visit me", "stay with me", "sleep with me",
        "date me", "marry me", "be my", "be my girlfriend", "be my boyfriend",
        "be my wife", "be my husband", "be my lover", "be my partner"
    )
    
    // Blocked patterns (regex)
    private val blockedPatterns = listOf(
        Regex("\\b\\w*[f]+[u]+[c]+[k]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[s]+[h]+[i]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[b]+[i]+[t]+[c]+[h]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[d]+[i]+[c]+[k]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[c]+[o]+[c]+[k]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[p]+[u]+[s]+[s]+[y]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[c]+[u]+[n]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[w]+[h]+[o]+[r]+[e]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[s]+[l]+[u]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[n]+[i]+[g]+[g]+[e]+[r]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[f]+[a]+[g]+[g]+[o]+[t]+\\w*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\w*[r]+[e]+[t]+[a]+[r]+[d]+\\w*\\b", RegexOption.IGNORE_CASE)
    )
    
    // Context-specific blocked content - REMOVED overly restrictive blocking
    private val mathContextBlocked = setOf<String>(
        // Only block clearly inappropriate math-related requests
        // Removed overly broad terms that were blocking legitimate questions
    )
    
    /**
     * Check if the input contains inappropriate content
     */
    fun containsInappropriateContent(input: String): Boolean {
        val lowerInput = input.lowercase()
        
        // Check for blocked words
        val words = lowerInput.split(Regex("\\s+|[^a-zA-Z0-9]"))
        val hasBlockedWords = words.any { word -> 
            blockedWords.any { blocked -> 
                word.contains(blocked, ignoreCase = true) || 
                blocked.contains(word, ignoreCase = true)
            }
        }
        
        // Check for blocked phrases with word boundaries
        val hasBlockedPhrases = blockedPhrases.any { phrase ->
            val phraseLower = phrase.lowercase()
            // Use word boundary matching to avoid false positives
            val pattern = Regex("\\b${Regex.escape(phraseLower)}\\b", RegexOption.IGNORE_CASE)
            pattern.containsMatchIn(lowerInput)
        }
        
        // Check for blocked patterns
        val hasBlockedPatterns = blockedPatterns.any { pattern ->
            pattern.containsMatchIn(input)
        }
        
        // Check for context-specific blocked content with word boundaries
        val hasContextBlocked = mathContextBlocked.any { phrase ->
            val phraseLower = phrase.lowercase()
            val pattern = Regex("\\b${Regex.escape(phraseLower)}\\b", RegexOption.IGNORE_CASE)
            pattern.containsMatchIn(lowerInput)
        }
        
        return hasBlockedWords || hasBlockedPhrases || hasBlockedPatterns || hasContextBlocked
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
     * Get a more detailed response based on the type of content detected
     */
    fun getDetailedFilteredMessage(input: String): String {
        if (!containsInappropriateContent(input)) {
            return input
        }
        
        val lowerInput = input.lowercase()
        
        return when {
            lowerInput.contains("sex") || lowerInput.contains("porn") || lowerInput.contains("nude") ->
                "I cannot process messages with sexual content. Please keep our conversation focused on mathematics and educational topics."
            
            lowerInput.contains("kill") || lowerInput.contains("death") || lowerInput.contains("suicide") ->
                "I cannot process messages with violent content. If you're having thoughts of self-harm, please contact a mental health professional or call a crisis hotline. Let's focus on mathematics instead."
            
            lowerInput.contains("nigger") || lowerInput.contains("faggot") || lowerInput.contains("retard") ->
                "I cannot process messages with hate speech or discriminatory language. Please be respectful and keep our conversation focused on mathematics."
            
            lowerInput.contains("fuck") || lowerInput.contains("shit") || lowerInput.contains("bitch") ->
                "I cannot process messages with profanity. Please use respectful language and keep our conversation focused on mathematics."
            
            else ->
                "I'm sorry, but I cannot process messages containing inappropriate content. Please keep our conversation focused on mathematics and educational topics."
        }
    }
    
    /**
     * Check if input contains only math-related content
     */
    fun isMathOnly(input: String): Boolean {
        val mathPattern = Regex("^[\\d+\\-*/%^().!√\\s\\w]+$")
        return mathPattern.matches(input.trim())
    }
    
    /**
     * Get severity level of inappropriate content (1-5, 5 being most severe)
     */
    fun getSeverityLevel(input: String): Int {
        if (!containsInappropriateContent(input)) return 0
        
        val lowerInput = input.lowercase()
        
        return when {
            lowerInput.contains("kill") || lowerInput.contains("suicide") || lowerInput.contains("rape") -> 5
            lowerInput.contains("nigger") || lowerInput.contains("faggot") || lowerInput.contains("retard") -> 4
            lowerInput.contains("fuck") || lowerInput.contains("shit") || lowerInput.contains("bitch") -> 3
            lowerInput.contains("sex") || lowerInput.contains("porn") || lowerInput.contains("nude") -> 3
            lowerInput.contains("damn") || lowerInput.contains("hell") || lowerInput.contains("crap") -> 2
            else -> 1
        }
    }
} 