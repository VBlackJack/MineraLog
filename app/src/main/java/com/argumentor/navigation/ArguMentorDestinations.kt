package com.argumentor.navigation

const val TOPIC_ID_ARG = "topicId"

sealed class ArguMentorDestination(val route: String) {
    object Dashboard : ArguMentorDestination("dashboard")
    object TopicEditor : ArguMentorDestination("editor/{$TOPIC_ID_ARG}") {
        fun createRoute(topicId: Long) = "editor/$topicId"
    }
    object Debate : ArguMentorDestination("debate/{$TOPIC_ID_ARG}") {
        fun createRoute(topicId: Long) = "debate/$topicId"
    }
    object Stats : ArguMentorDestination("stats/{$TOPIC_ID_ARG}") {
        fun createRoute(topicId: Long) = "stats/$topicId"
    }
    object Backup : ArguMentorDestination("backup")
    object Fallacies : ArguMentorDestination("fallacies")
}
