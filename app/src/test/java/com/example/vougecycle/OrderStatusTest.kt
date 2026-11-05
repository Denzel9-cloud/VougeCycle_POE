package com.example.vougecycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderStatusTest {
    @Test fun orderMovesInTheCorrectSequence() {
        assertEquals("paid", OrderStatus.next("pending"))
        assertEquals("shipped", OrderStatus.next("paid"))
        assertEquals("delivered", OrderStatus.next("shipped"))
    }

    @Test fun deliveredOrderCannotAdvance() {
        assertNull(OrderStatus.next("delivered"))
    }
}
