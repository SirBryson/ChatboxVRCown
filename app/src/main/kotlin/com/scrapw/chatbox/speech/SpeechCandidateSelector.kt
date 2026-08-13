package com.scrapw.chatbox.speech

/**
 * Conservatively re-ranks Samsung's own recognition hypotheses. It never creates
 * or edits words; it can only select one candidate that the recognizer returned.
 */
class SpeechCandidateSelector {

    fun select(candidates: List<String>, confidences: FloatArray?): String {
        val usable = candidates
            .map(String::trim)
            .filter(String::isNotEmpty)
            .take(MAX_CANDIDATES)

        if (usable.isEmpty()) return ""
        if (usable.size == 1) return usable.first()

        return usable.withIndex().maxByOrNull { (index, text) ->
            score(text, index, confidenceAt(confidences, index))
        }?.value ?: usable.first()
    }

    private fun score(text: String, rank: Int, confidence: Float?): Double {
        val normalized = text
            .lowercase()
            .replace('’', '\'')
            .replace(Regex("[^a-z0-9' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        var score = -(rank * RANK_PENALTY)
        if (confidence != null) score += confidence * CONFIDENCE_WEIGHT

        for ((pattern, penalty) in grammarPenalties) {
            if (pattern.containsMatchIn(normalized)) score -= penalty
        }

        val words = normalized.split(' ').filter(String::isNotBlank)
        score -= words.zipWithNext().count { (left, right) -> left == right } * REPEATED_WORD_PENALTY
        return score
    }

    private fun confidenceAt(confidences: FloatArray?, index: Int): Double? {
        val value = confidences?.getOrNull(index) ?: return null
        return value.takeIf { it in 0f..1f }?.toDouble()
    }

    private companion object {
        const val MAX_CANDIDATES = 5
        const val CONFIDENCE_WEIGHT = 2.0
        const val RANK_PENALTY = 0.18
        const val REPEATED_WORD_PENALTY = 1.25

        val grammarPenalties = listOf(
            Regex("\\b(that's|what's|who's|where's|there's|here's)\\s+(was|were|am|are)\\b") to 2.5,
            Regex("\\bi\\s+(is|are|were)\\b") to 2.0,
            Regex("\\byou\\s+(is|am|was)\\b") to 2.0,
            Regex("\\b(he|she|it)\\s+(am|are|were)\\b") to 2.0,
            Regex("\\b(we|they)\\s+(is|am|was)\\b") to 2.0,
            Regex("\\bdid\\s+(went|saw|heard|said|made|took|came|knew|thought|gave|found)\\b") to 1.8
        )
    }
}
