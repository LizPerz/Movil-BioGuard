package com.bioguard.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bioguard.movil.ui.screens.EditPerfilDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class EditPerfilDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun camposEnBlanco_muestraErroresYNoGuarda() {
        var saved = false
        composeRule.setContent {
            EditPerfilDialog(
                currentNombre = "",
                currentApellidoPaterno = "",
                currentApellidoMaterno = "",
                onDismiss = {},
                onSave = { _, _, _ -> saved = true }
            )
        }

        composeRule.onNodeWithText("Guardar").performClick()

        composeRule.onNodeWithText("Ingresa tu nombre").assertIsDisplayed()
        composeRule.onNodeWithText("Ingresa tu apellido paterno").assertIsDisplayed()
        composeRule.onNodeWithText("Ingresa tu apellido materno").assertIsDisplayed()
        assertFalse("No debe llamar onSave con campos vacíos", saved)
    }

    @Test
    fun camposValidos_guardaConTrim() {
        var savedNombre: String? = null
        var savedApellidoPaterno: String? = null
        var savedApellidoMaterno: String? = null
        composeRule.setContent {
            EditPerfilDialog(
                currentNombre = "",
                currentApellidoPaterno = "",
                currentApellidoMaterno = "",
                onDismiss = {},
                onSave = { nombre, apellidoPaterno, apellidoMaterno ->
                    savedNombre = nombre
                    savedApellidoPaterno = apellidoPaterno
                    savedApellidoMaterno = apellidoMaterno
                }
            )
        }

        composeRule.onNodeWithText("Nombre").performTextInput("  Carlos  ")
        composeRule.onNodeWithText("Apellido paterno").performTextInput("  García ")
        composeRule.onNodeWithText("Apellido materno").performTextInput("  López ")
        composeRule.onNodeWithText("Guardar").performClick()

        assertEquals("Carlos", savedNombre)
        assertEquals("García", savedApellidoPaterno)
        assertEquals("López", savedApellidoMaterno)
    }
}
