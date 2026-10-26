package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perimenopauseintelligence.app.content.Articles

@Composable
fun LearnScreen(onOpen: (String) -> Unit) {
    ScreenList("Learn") {
        item { InfoCard { Text(Articles.STATUS, style = MaterialTheme.typography.bodyMedium) } }
        items(Articles.all.size, key = { Articles.all[it].id }) { i ->
            val a = Articles.all[i]
            Card(Modifier.fillMaxWidth().clickable { onOpen(a.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(a.title, style = MaterialTheme.typography.titleMedium)
                    Text(a.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ArticleScreen(id: String, onBack: () -> Unit) {
    val a = Articles.byId(id)
    ScreenList(a?.title ?: "Article", onBack = onBack) {
        if (a == null) {
            item { Text("This article could not be found.") }
            return@ScreenList
        }
        item { Text(Articles.STATUS, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(a.paragraphs.size) { Text(a.paragraphs[it], style = MaterialTheme.typography.bodyLarge) }
        item {
            InfoCard {
                Text("Sources to verify before release", style = MaterialTheme.typography.titleSmall)
                Articles.sourcesToVerify.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { Text("This is general information, not medical advice.", style = MaterialTheme.typography.bodySmall) }
    }
}
