package com.example.applista.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object PaymentValues {
    private val brl: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /** Valor que vale para o membro: o individual, se houver, senão o geral. */
    fun effective(member: PaymentMember, valorGeral: String): String =
        member.valor ?: valorGeral

    /** Aceita "30", "30,50", "R$ 30,00", "1.234,56". Retorna null se não for número. */
    fun parse(raw: String?): BigDecimal? {
        if (raw.isNullOrBlank()) return null
        var s = raw.filter { it.isDigit() || it == ',' || it == '.' }
        if (s.isEmpty()) return null
        if (s.contains(',')) {
            s = s.replace(".", "").replace(',', '.')
        }
        return s.toBigDecimalOrNull()
    }

    fun format(value: BigDecimal): String =
        brl.format(value).replace(' ', ' ')

    /** Padroniza para "R$ 30,00" quando dá para entender o número; senão mantém o texto. */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        return parse(trimmed)?.let { format(it) } ?: trimmed
    }

    /** Soma o que falta receber (membros não pagos). Ignora valores que não são número. */
    fun totalPendente(members: List<PaymentMember>, valorGeral: String): BigDecimal =
        members.filter { !it.isPaid }
            .mapNotNull { parse(effective(it, valorGeral)) }
            .fold(BigDecimal.ZERO, BigDecimal::add)
}
