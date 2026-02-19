package com.app.viam.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * App-wide shape constants — change these to adjust button/card/input corner radius globally.
 */
object AppShapes {
    /** Radius used on buttons (Button, OutlinedButton, etc.) */
    val ButtonRadius = 8.dp

    /** Radius used on cards */
    val CardRadius = 10.dp

    /** Radius used on dialogs */
    val DialogRadius = 12.dp

    /** Radius used on text fields */
    val TextFieldRadius = 8.dp

    /** Radius used on small chips / badges */
    val ChipRadius = 6.dp
}

val AppMaterialShapes = Shapes(
    // extraSmall covers TextFields and small components
    extraSmall = RoundedCornerShape(AppShapes.TextFieldRadius),
    // small covers Chips, Snackbars
    small = RoundedCornerShape(AppShapes.ChipRadius),
    // medium covers Cards, Dialogs, Buttons
    medium = RoundedCornerShape(AppShapes.ButtonRadius),
    // large covers Bottom sheets
    large = RoundedCornerShape(AppShapes.CardRadius),
    // extraLarge covers FAB
    extraLarge = RoundedCornerShape(AppShapes.DialogRadius)
)
