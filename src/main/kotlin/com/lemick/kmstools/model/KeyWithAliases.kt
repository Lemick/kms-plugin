package com.lemick.kmstools.model

data class KeyWithAliases(val keyId: String, val aliases: List<String>) {

    override fun toString(): String {
        val formattedAliases = aliases.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "(", postfix = ")", separator = ", ")
            ?.let { if (it.length > 50) it.take(50) + "..." else it }
            ?: ""
        return "$keyId $formattedAliases"
    }
}
