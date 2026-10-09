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

    /** Valor geral: vale para quem não tem valor individual. */
    suspend fun setValorMensal(valor: String) {
        if (valor.isBlank()) return
        val normalized = PaymentValues.normalize(valor)
        context.paymentDataStore.edit { prefs ->
            prefs[valorKey] = normalized
        }
    }

    /** Valor único: grava como geral e apaga os valores individuais de todos. */
    suspend fun applyValorToAll(valor: String) {
        if (valor.isBlank()) return
        val normalized = PaymentValues.normalize(valor)
        context.paymentDataStore.edit { prefs ->
            prefs[valorKey] = normalized
            val list = parseList(prefs[membersKey]).map { it.copy(valor = null) }
            prefs[membersKey] = gson.toJson(list)
        }
    }

    /** Valor individual; em branco volta a usar o valor geral. */
    suspend fun setMemberValor(member: PaymentMember, valor: String?) {
        val normalized = valor?.takeIf { it.isNotBlank() }?.let { PaymentValues.normalize(it) }
        context.paymentDataStore.edit { prefs ->
            val list = parseList(prefs[membersKey])
            val idx = list.indexOfFirst { it.id == member.id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(valor = normalized)
                prefs[membersKey] = gson.toJson(list)
            }
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
        if (members.all { it.valor == null }) {
            appendLine("💰 Valor: $valor")
        }
        appendLine("🔑 PIX: $pix")
        appendLine()
        members.forEachIndexed { i, m ->
            val icon = if (m.isPaid) "✅" else "⛔"
            val situacao = if (m.isPaid) "PAGO" else PaymentValues.effective(m, valor)
            appendLine("$icon ${i + 1}. ${m.name} - $situacao")
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
