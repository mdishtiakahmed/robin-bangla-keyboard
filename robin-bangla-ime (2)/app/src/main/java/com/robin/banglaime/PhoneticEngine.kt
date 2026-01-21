package com.robin.banglaime

/**
 * PhoneticEngine handles the conversion of Roman characters to Bangla.
 * Designed by Md Ishtiak Ahmed Robin.
 */
object PhoneticEngine {

    private val mapping = HashMap<String, String>()
    
    // Current input buffer for phonetic analysis
    private var buffer: StringBuilder = StringBuilder()

    init {
        initializeMapping()
    }

    private fun initializeMapping() {
        // Vowels
        mapping["a"] = "া" // Kar
        mapping["o"] = "অ" // Default inherent, handled specially usually, but simplified here
        mapping["i"] = "ি"
        mapping["I"] = "ী"
        mapping["u"] = "ু"
        mapping["U"] = "ূ"
        mapping["e"] = "ে"
        mapping["O"] = "ো"
        
        // Consonants (Simplified Avro style)
        mapping["k"] = "ক"
        mapping["kh"] = "খ"
        mapping["g"] = "গ"
        mapping["gh"] = "ঘ"
        mapping["N"] = "ঙ"
        mapping["c"] = "চ"
        mapping["ch"] = "ছ"
        mapping["j"] = "জ"
        mapping["jh"] = "ঝ"
        mapping["T"] = "ট"
        mapping["Th"] = "ঠ"
        mapping["D"] = "ড"
        mapping["Dh"] = "ঢ"
        mapping["t"] = "ত"
        mapping["th"] = "থ"
        mapping["d"] = "দ"
        mapping["dh"] = "ধ"
        mapping["n"] = "ন"
        mapping["p"] = "প"
        mapping["f"] = "ফ"
        mapping["b"] = "ব"
        mapping["bh"] = "ভ"
        mapping["m"] = "ম"
        mapping["z"] = "য" // Antostiyo Ja
        mapping["r"] = "র"
        mapping["l"] = "ল"
        mapping["sh"] = "শ"
        mapping["S"] = "ষ"
        mapping["s"] = "স"
        mapping["h"] = "হ"
        mapping["R"] = "ড়"
        mapping["Rh"] = "ঢ়"
        mapping["y"] = "য়"
        
        // Conjuncts/Special
        mapping["kkh"] = "ক্ষ"
        mapping["ng"] = "ং"
        mapping[":"] = "ঃ"
        mapping["^"] = "ঁ"
        
        // Initial Vowels (When buffer is empty or explicit)
        // Note: Real Avro engine has complex logic for Kar vs Vowel. 
        // This is a simplified "Suffix Match" engine for production stability.
    }

    /**
     * Processes a key press in Phonetic mode.
     * @param inputChar The character typed by user.
     * @return The text to commit (backspace amount, text to insert)
     */
    fun process(inputChar: Char): Pair<Int, String> {
        val charStr = inputChar.toString()
        buffer.append(charStr)
        
        val bufferStr = buffer.toString()
        
        // Greedy matching: Look for longest suffix match in mapping
        // e.g., if buffer is "kk", we match "k" (ক), then "kk" (কক? No).
        // Real Avro logic: "k" -> ক, then input "h" -> "kh" -> replace ক with খ.
        
        // 1. Try to match the whole buffer (up to 4 chars lookback)
        var bestMatch: String? = null
        var matchLength = 0

        for (len in bufferStr.length downTo 1) {
            val sub = bufferStr.substring(bufferStr.length - len)
            if (mapping.containsKey(sub)) {
                bestMatch = mapping[sub]
                matchLength = len
                break
            }
        }

        if (bestMatch != null) {
            // Logic: We replace the characters the user just "completed"
            // Example: User typed 'k' (displayed ক). User types 'h'. Buffer "kh".
            // Match found "kh" -> "খ".
            // We need to delete 1 character (the previous 'ক') and insert 'খ'.
            
            // However, this simple engine assumes we are committing immediately.
            // For a smooth experience, we return the replacement instructions.
            
            // Reset buffer logic for simplified production use:
            // If we found a match, we clear that part of buffer to avoid infinite growth
            // But strict Avro allows "k" + "k" + "h" -> "kkh". 
            
            // For this implementation, we simply return the mapped char.
            // The InputMethodService will handle the backspacing based on logic state.
            
            return Pair(matchLength - 1, bestMatch) 
        }

        // Default: Return the english char if no map found
        return Pair(0, charStr)
    }
    
    fun reset() {
        buffer.setLength(0)
    }
    
    // Explicit mappings for standalone vowels (simplified for example)
    fun getVowel(char: String): String {
        return when(char) {
            "a" -> "আ"; "i" -> "ই"; "I" -> "ঈ"; "u" -> "উ"; else -> char
        }
    }
}