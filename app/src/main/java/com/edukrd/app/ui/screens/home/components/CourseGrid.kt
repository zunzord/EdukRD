package com.edukrd.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edukrd.app.models.Course
import com.edukrd.app.ui.screens.home.components.ExperienceCard

@Composable
fun CourseGrid(
    courses: List<Course>,
    passedCourseIds: Set<String>,
    coinRewards: Map<String, Int>,
    onCourseClick: (Course) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(courses) { course ->
            val isCompleted = passedCourseIds.contains(course.id)
            ExperienceCard(
                course = course,
                isCompleted = isCompleted,
                onClick = { onCourseClick(course) }
            )
        }
    }
}
