package com.example.applista

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applista.data.PaymentMember
import com.example.applista.data.PaymentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PaymentRepository(application.applicationContext)

    val members = repository.members.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val settings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        com.example.applista.data.PaymentSettings(
            valor = com.example.applista.data.PaymentConstants.VALOR,
            pix = com.example.applista.data.PaymentConstants.PIX,
        ),
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialData()
        }
    }

    fun togglePaid(member: PaymentMember) {
        viewModelScope.launch { repository.togglePaid(member) }
    }

    fun addMember(name: String) {
        viewModelScope.launch { repository.addMember(name) }
    }

    fun removeMember(member: PaymentMember) {
        viewModelScope.launch { repository.removeMember(member) }
    }

    fun setValorMensal(valor: String) {
        viewModelScope.launch { repository.setValorMensal(valor) }
    }

    fun setPix(pix: String) {
        viewModelScope.launch { repository.setPix(pix) }
    }

    fun whatsAppText(): String = whatsAppTextFor(members.value)

    fun whatsAppTextFor(members: List<PaymentMember>): String =
        repository.buildWhatsAppText(
            members = members,
            valor = settings.value.valor,
            pix = settings.value.pix,
        )
}
