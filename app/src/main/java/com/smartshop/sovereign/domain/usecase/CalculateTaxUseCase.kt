package com.smartshop.sovereign.domain.usecase

import com.smartshop.sovereign.domain.model.CartItem
import javax.inject.Inject

/**
 * Use Case: Calculate tax and totals for checkout
 * All money calculations in Long (cents) - no Double!
 */
class CalculateTaxUseCase @Inject constructor() {
    /**
     * Calculate subtotal, tax, and total
     * @param items Cart items
     * @param taxRate Tax rate in basis points (e.g., 1800 = 18%)
     * @return Triple of (subtotal, tax, total) all in UGX cents
     */
    operator fun invoke(items: List<CartItem>, taxRate: Long): Triple<Long, Long, Long> {
        val subtotal = items.sumOf { it.totalPrice }
        // Tax calculation: included in price (tax = price * rate / (10000 + rate))
        val tax = (subtotal * taxRate) / (10000L + taxRate)
        val total = subtotal // Total includes tax
        return Triple(subtotal, tax, total)
    }
}