package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Goal
import com.example.ui.GoalCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun goal_card_screenshot() {
    val sampleGoal = Goal(
      id = 1,
      title = "Drink Water Daily",
      description = "Stay healthy and hydrated throughout the day",
      currentPoints = 3,
      maxPoints = 8,
      allowExceed = false
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        GoalCard(
          goal = sampleGoal,
          isDark = false,
          onIncrement = {},
          onDecrement = {},
          onHoldIncrement = {},
          onHoldDecrement = {},
          onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
