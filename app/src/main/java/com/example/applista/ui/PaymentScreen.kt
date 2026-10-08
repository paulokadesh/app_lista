package com.example.applista.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.applista.PaymentViewModel
import com.example.applista.R
import com.example.applista.data.PaymentConstants
import com.example.applista.data.PaymentMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(viewModel: PaymentViewModel) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf("") }
    var valorMensal by remember { mutableStateOf("") }
    var pixKey by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<PaymentMember?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(MemberFilter.ALL) }
    var pendingExport by remember { mutableStateOf<ExportAction?>(null) }
    var exportMode by remember { mutableStateOf(ExportMode.CURRENT_FILTER) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    fun searchedMembers(): List<PaymentMember> {
        if (searchQuery.isBlank()) return members
        val q = searchQuery.trim()
        return members.filter { it.name.contains(q, ignoreCase = true) }
    }

    fun filteredMembers(): List<PaymentMember> {
        val searched = searchedMembers()
        return when (filter) {
            MemberFilter.ALL -> searched
            MemberFilter.PAID -> searched.filter { it.isPaid }
            MemberFilter.PENDING -> searched.filter { !it.isPaid }
        }
    }

    fun allMembersSortedPaidFirstByName(): List<PaymentMember> {
        val byName = compareBy<PaymentMember> { it.name.lowercase() }.thenBy { it.id }
        val paid = members.filter { it.isPaid }.sortedWith(byName)
        val pending = members.filter { !it.isPaid }.sortedWith(byName)
        return paid + pending
    }

    LaunchedEffect(settings.valor) {
        if (valorMensal.isBlank()) {
            valorMensal = settings.valor
        }
    }

    LaunchedEffect(settings.pix) {
        if (pixKey.isBlank()) {
            pixKey = settings.pix
        }
    }

    fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.share_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.share_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun tryWhatsApp(text: String) {
        val whatsapp = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            whatsapp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(whatsapp)
        } catch (_: Exception) {
            shareText(text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_valor, settings.valor),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.label_vencimento, PaymentConstants.VENCIMENTO_DIA),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.label_pix, settings.pix),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        IconButton(onClick = { menuExpanded = !menuExpanded }) {
                            Icon(
                                imageVector = if (menuExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                            )
                        }
                    }

                    AnimatedVisibility(visible = menuExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = pixKey,
                                onValueChange = { pixKey = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.hint_pix)) },
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.setPix(pixKey) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.save_pix))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = valorMensal,
                                onValueChange = { valorMensal = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.hint_valor_mensal)) },
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.setValorMensal(valorMensal) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.save_valor_mensal))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            RowButtons(
                                onCopy = { pendingExport = ExportAction.COPY },
                                onWhatsApp = { pendingExport = ExportAction.WHATSAPP },
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.hint_new_member)) },
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.addMember(newName)
                                    newName = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.add_member))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_member)) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val searched = searchedMembers()
            val countAll = searched.size
            val countPaid = searched.count { it.isPaid }
            val countPending = countAll - countPaid

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterOption(
                    selected = filter == MemberFilter.ALL,
                    text = stringResource(
                        R.string.filter_with_count,
                        stringResource(R.string.filter_all),
                        countAll,
                    ),
                    onClick = { filter = MemberFilter.ALL },
                    modifier = Modifier.weight(1f),
                )
                FilterOption(
                    selected = filter == MemberFilter.PAID,
                    text = stringResource(
                        R.string.filter_with_count,
                        stringResource(R.string.filter_paid),
                        countPaid,
                    ),
                    onClick = { filter = MemberFilter.PAID },
                    modifier = Modifier.weight(1f),
                )
                FilterOption(
                    selected = filter == MemberFilter.PENDING,
                    text = stringResource(
                        R.string.filter_with_count,
                        stringResource(R.string.filter_pending),
                        countPending,
                    ),
                    onClick = { filter = MemberFilter.PENDING },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                val filtered = filteredMembers()
                items(filtered, key = { it.id }) { member ->
                    MemberItem(
                        member = member,
                        onToggle = { viewModel.togglePaid(member) },
                        onDeleteClick = { pendingDelete = member },
                        onSwipeDelete = { pendingDelete = member },
                    )
                }
            }
        }
    }

    pendingDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.confirm_remove_title)) },
            text = { Text(stringResource(R.string.confirm_remove_message, member.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(member)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingExport?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingExport = null },
            title = { Text(stringResource(R.string.export_options_title)) },
            text = {
                Column {
                    ExportOption(
                        selected = exportMode == ExportMode.CURRENT_FILTER,
                        text = stringResource(R.string.export_current_filter),
                        onClick = { exportMode = ExportMode.CURRENT_FILTER },
                    )
                    ExportOption(
                        selected = exportMode == ExportMode.ALL_SORTED,
                        text = stringResource(R.string.export_all_sorted),
                        onClick = { exportMode = ExportMode.ALL_SORTED },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val exportList = when (exportMode) {
                            ExportMode.CURRENT_FILTER -> filteredMembers()
                            ExportMode.ALL_SORTED -> allMembersSortedPaidFirstByName()
                        }
                        val text = viewModel.whatsAppTextFor(exportList)
                        when (action) {
                            ExportAction.COPY -> {
                                clipboard.setText(AnnotatedString(text))
                                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                            }
                            ExportAction.WHATSAPP -> {
                                tryWhatsApp(text)
                            }
                        }
                        pendingExport = null
                    },
                ) {
                    Text(stringResource(R.string.export))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExport = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RowButtons(
    onCopy: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.copy_whatsapp))
        }
        OutlinedButton(onClick = onWhatsApp, modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.share_whatsapp))
            }
        }
    }
}

private enum class MemberFilter {
    ALL,
    PAID,
    PENDING,
}

@Composable
private fun FilterOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private enum class ExportAction {
    COPY,
    WHATSAPP,
}

private enum class ExportMode {
    CURRENT_FILTER,
    ALL_SORTED,
}

@Composable
private fun ExportOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = text, modifier = Modifier.padding(start = 6.dp))
    }
}
