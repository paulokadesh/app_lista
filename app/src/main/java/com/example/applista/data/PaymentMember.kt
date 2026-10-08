package com.example.applista.data

data class PaymentMember(
    val id: Long,
    val name: String,
    val isPaid: Boolean,
    /** Valor individual do membro; null = usa o valor geral do mês. */
    val valor: String? = null,
)
