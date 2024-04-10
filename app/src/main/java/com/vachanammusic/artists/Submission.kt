// Submission.kt
package com.vachanammusic.artists

data class Submission(
    val name: String,
    val description: String,
    val artists: String,
    val organization: String,
    val website: String,
    val others: String,
    val approved: String
// assuming approved is a string, change to boolean if it's appropriate
)
