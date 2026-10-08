# Prompt do Projeto Android - Controle de Pagamentos

Crie um app Android em Kotlin com Jetpack Compose chamado **appLista**, package `com.example.applista`, Minimum SDK API 24, usando Kotlin DSL (`build.gradle.kts`).

## Objetivo
O app será usado para controle de pagamentos de um grupo.

## Informações fixas na tela principal
- **Valor:** R$ 25,00
- **Vencimento:** dia 10
- **Chave PIX:** (sua chave PIX)

## Lista inicial de membros
- Membro 1 ✅
- Membro 2 ⛔
- Membro 3 ⛔

## Funcionalidades obrigatórias
1. **Marcar/desmarcar pagamento** ao clicar no nome do membro, alternando entre:
   - ✅ Pago
   - ⛔ Pendente

2. **Adicionar membro** com:
   - campo de texto
   - botão para adicionar

3. **Remover membro** com uma das opções:
   - botão de deletar
   - swipe para excluir

4. **Botão "Copiar para WhatsApp"**
   - Deve gerar o texto formatado da lista
   - Deve copiar o texto para a área de transferência
   - O texto deve ficar pronto para colar no WhatsApp

5. **Persistência local**
   - Usar Room ou SharedPreferences/DataStore
   - Salvar a lista e o status de pagamento mesmo após fechar o app

## Formato do texto para WhatsApp
```text
⚽ Lista de Pagamento - Vencimento: 10
💰 Valor: R$ 25,00
🔑 PIX: (sua chave PIX)

✅ Membro 1
⛔ Membro 2
⛔ Membro 3
```

## Estrutura sugerida do projeto
- `MainActivity.kt`
- `PaymentMember.kt` (model)
- `PaymentViewModel.kt`
- `PaymentRepository.kt`
- `PaymentScreen.kt`
- `MemberItem.kt`
- arquivos necessários de persistência local

## Requisitos técnicos
- Linguagem: **Kotlin**
- UI: **Jetpack Compose**
- Arquitetura simples e organizada
- Código limpo e comentado quando necessário
- Compatível com Android Studio

## Extra desejável
- Botão para compartilhar diretamente no WhatsApp usando Intent
- Confirmação antes de remover membro
- Interface simples, bonita e fácil de usar
