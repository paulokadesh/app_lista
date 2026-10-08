package com.example.applista.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.paymentDataStore: DataStore<Preferences> by preferencesDataStore(name = "payment_members")

data class PaymentSettings(
    val valor: String,
    val pix: String,
)

class PaymentRepository(private val context: Context) {
    private val gson = Gson()
    private val membersKey = stringPreferencesKey("members_json")
    private val valorKey = stringPreferencesKey("valor_mensal")
    private val pixKey = stringPreferencesKey("pix_key")
    private val listType = object : TypeToken<MutableList<PaymentMember>>() {}.type

    val members: Flow<List<PaymentMember>> = context.paymentDataStore.data.map { prefs ->
        parseList(prefs[membersKey])
    }

    val settings: Flow<PaymentSettings> = context.paymentDataStore.data.map { prefs ->
        PaymentSettings(
            valor = prefs[valorKey] ?: PaymentConstants.VALOR,
            pix = prefs[pixKey] ?: PaymentConstants.PIX,
        )
    }

    suspend fun ensureInitialData() {
        context.paymentDataStore.edit { prefs ->
            if (prefs[membersKey].isNullOrBlank()) {
                prefs[membersKey] = gson.toJson(InitialMembers.list)
            }
            if (prefs[valorKey].isNullOrBlank()) {
                prefs[valorKey] = PaymentConstants.VALOR
            }
            if (prefs[pixKey].isNullOrBlank()) {
                prefs[pixKey] = PaymentConstants.PIX
            }
        }
    }

    suspend fun togglePaid(member: PaymentMember) {
        context.paymentDataStore.edit { prefs ->
            val list = parseList(prefs[membersKey])
            val idx = list.indexOfFirst { it.id == member.id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(isPaid = !list[idx].isPaid)
                prefs[membersKey] = gson.toJson(list)
            }
        }
    }

    suspend fun addMember(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        context.paymentDataStore.edit { prefs ->
            val list = parseList(prefs[membersKey])
            val nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1L
            list.add(PaymentMember(id = nextId, name = trimmed, isPaid = false))
            prefs[membersKey] = gson.toJson(list)
        }
    }

    suspend fun removeMember(member: PaymentMember) {
        context.paymentDataStore.edit { prefs ->
            val list = parseList(prefs[membersKey])
            list.removeAll { it.id == member.id }
            prefs[membersKey] = gson.toJson(list)
        }
    }

    suspend fun setValorMensal(valor: String) {
        val trimmed = valor.trim()
        if (trimmed.isEmpty()) return
        context.paymentDataStore.edit { prefs ->
            prefs[valorKey] = trimmed
        }
    }

    suspend fun setPix(pix: String) {
        val trimmed = pix.trim()
        if (trimmed.isEmpty()) return
        context.paymentDataStore.edit { prefs ->
            prefs[pixKey] = trimmed
        }
    }

    fun buildWhatsAppText(
        members: List<PaymentMember>,
        valor: String,
        pix: String,
    ): String = buildString {
        appendLine("⚽ Lista de Pagamento - Vencimento: ${PaymentConstants.VENCIMENTO_DIA}")
        appendLine("💰 Valor: $valor")
        appendLine("🔑 PIX: $pix")
        appendLine()
        members.forEach { m ->
            val icon = if (m.isPaid) "✅" else "⛔"
            appendLine("$icon ${m.name}")
        }
    }

    private fun parseList(raw: String?): MutableList<PaymentMember> {
        if (raw.isNullOrBlank()) return InitialMembers.list.toMutableList()
        return gson.fromJson<MutableList<PaymentMember>>(raw, listType) ?: InitialMembers.list.toMutableList()
    }
}

private object InitialMembers {
    val list = listOf(
        PaymentMember(1L, "Membro 1", true),
        PaymentMember(2L, "Membro 2", false),
        PaymentMember(3L, "Membro 3", false),
    )
}
