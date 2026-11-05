package com.example.vougecycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {
    @Test fun validEmailIsAccepted() = assertTrue(InputValidator.isValidEmail("user@example.com"))

    @Test fun invalidEmailIsRejected() = assertFalse(InputValidator.isValidEmail("not-an-email"))

    @Test fun strongPasswordHasNoError() = assertNull(InputValidator.passwordError("Secure1"))

    @Test fun weakPasswordReturnsUsefulError() {
        assertEquals("Password must contain a capital letter", InputValidator.passwordError("secure1"))
    }

    @Test fun priceMustBePositive() {
        assertEquals(150.50, InputValidator.validPrice("150.50")!!, 0.0)
        assertNull(InputValidator.validPrice("-1"))
    }
}
