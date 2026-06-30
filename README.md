<div align="center">

# 🛡️ NoSCRoll

### *Retome o controle do seu tempo de tela*

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![API](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-informational?style=for-the-badge)](https://developer.android.com/studio/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

**NoSCRoll** é um aplicativo Android de bem-estar digital que ajuda você a monitorar, limitar e reduzir o tempo gasto em aplicativos no seu smartphone. Com um sistema de metas e gamificação baseado em XP, manter hábitos saudáveis de uso do celular nunca foi tão motivador.

</div>

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Arquitetura](#-arquitetura)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação e Configuração](#-instalação-e-configuração)
- [Permissões Necessárias](#-permissões-necessárias)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Fluxo de Telas](#-fluxo-de-telas)
- [Como Contribuir](#-como-contribuir)

---

## 💡 Sobre o Projeto

O **NoSCRoll** nasceu da necessidade de combater o uso excessivo de aplicativos de redes sociais e entretenimento. O app roda um **serviço de monitoramento em segundo plano**, verificando a cada **2 segundos** qual aplicativo está em uso e, ao atingir o limite diário configurado, **bloqueia o acesso** automaticamente com uma tela de aviso.

Ao contrário de apps de controle parental comuns, o NoSCRoll é **self-service**: o próprio usuário define seus limites, acompanha seu progresso em um gráfico semanal e se motiva através de um sistema de **metas com recompensas de XP e níveis**.

---

## ✨ Funcionalidades

### 🔒 Bloqueio Inteligente de Apps
- Monitora o app em primeiro plano em tempo real (a cada **2 segundos**)
- Ao atingir o limite diário, exibe uma **tela de bloqueio** sobre o aplicativo
- O app bloqueado é forçado a ir para segundo plano automaticamente
- O bloqueio é **resetado à meia-noite** (limite por dia)
- Cache local de limites para performance otimizada, sem delay entre verificações

### ⏱️ Configuração de Limites por App
- Selecione qualquer app instalado no dispositivo
- Defina um limite diário com precisão de **horas e minutos** (pickers interativos)
- Visualize o uso atual do dia no momento da configuração
- Remova apps da lista de monitoramento com um clique — o desbloqueio é imediato

### 📊 Dashboard com Gráfico Semanal
- Gráfico de linha interativo (via **MPAndroidChart**) mostrando o uso total dos últimos **7 dias**
- Exibição do nível atual e barra de progresso de XP do usuário
- Preview das três primeiras metas pendentes (fácil / médio / difícil)

### 🎯 Sistema de Metas com Gamificação
- Crie metas pessoais de redução de uso do celular
- Três níveis de dificuldade com recompensas diferentes:
  | Nível | XP ganho ao concluir |
  |-------|---------------------|
  | 🟢 Fácil | +15 XP |
  | 🟡 Médio | +30 XP |
  | 🔴 Difícil | +50 XP |
- Sistema de **Level Up** com progressão exponencial (XP necessário aumenta 15% por nível)
- Metas organizadas em listas separadas por dificuldade com RecyclerView

### 👤 Autenticação e Perfil
- Cadastro e login com **Firebase Authentication** (e-mail e senha)
- Dados do perfil (nome completo, data de nascimento) sincronizados no **Firestore**
- Edição de perfil com atualização de senha
- Logout com limpeza de sessão

---

## 🏗️ Arquitetura

O projeto segue uma arquitetura **MVVM simplificada** com separação de responsabilidades em camadas:

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer (Activities)             │
│  LoginActivity · ActivityInicio · MetasActivity     │
│  AplicativosActivity · CadastroAplicativo · ...     │
└──────────────────────────┬──────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────┐
│               Data / Repository Layer               │
│         AppUsageRepository (Room + UsageStats)      │
└──────────┬───────────────────────────────┬──────────┘
           │                               │
┌──────────▼──────────┐       ┌────────────▼──────────┐
│   Room Database     │       │  Firebase Firestore    │
│  · app_limits       │       │  · users (perfil/XP)  │
│  · daily_usage      │       │  · metas (goals)      │
└─────────────────────┘       └───────────────────────┘
           │
┌──────────▼──────────────────────────────────────────┐
│           Background Service Layer                  │
│   AppUsageMonitor (ForegroundService)               │
│   └─ UsageStatsManagerHelper                        │
└─────────────────────────────────────────────────────┘
```

### Camadas Principais

| Camada | Componentes | Responsabilidade |
|--------|-------------|-----------------|
| **UI** | Activities, Adapters | Exibição e interação do usuário |
| **Repository** | `AppUsageRepository` | Abstração do banco de dados local |
| **Database** | Room (`AppDatabase`) | Persistência local de limites e uso diário |
| **Cloud** | Firebase Auth + Firestore | Autenticação e dados do perfil/metas |
| **Service** | `AppUsageMonitor` | Monitoramento em background (ForegroundService) |
| **Helper** | `UsageStatsManagerHelper` | Leitura das estatísticas do Android |

---

## 🛠️ Tecnologias Utilizadas

| Categoria | Tecnologia | Versão |
|-----------|-----------|--------|
| **Linguagem** | Kotlin | 1.9.22 |
| **UI** | Android Views + Material Design 3 | 1.11.0 |
| **Gráficos** | MPAndroidChart | 3.1.0 |
| **Banco Local** | Room (SQLite) | 2.6.1 |
| **Autenticação** | Firebase Authentication | 22.3.1 |
| **Banco em Nuvem** | Firebase Firestore | 24.10.1 |
| **Analytics** | Firebase Analytics | BOM 32.8.0 |
| **Async** | Kotlin Coroutines | 1.7.3 |
| **Build** | Gradle Kotlin DSL | 8.13.1 |
| **Min SDK** | Android 7.0 (Nougat) | API 24 |
| **Target SDK** | Android 14 | API 34 |
| **Compile SDK** | Android 15 | API 36 |

---

## 📋 Pré-requisitos

- **Android Studio** Hedgehog (2023.1.1) ou superior
- **JDK 8** ou superior
- **Conta Firebase** com projeto configurado
- Dispositivo ou emulador com **Android 7.0+** (API 24)
- Conexão com a internet para sincronização com Firebase

---

## 🚀 Instalação e Configuração

### 1. Clone o repositório
```bash
git clone https://github.com/Devisneke/NoSCRoll.git
cd NoSCRoll
```

### 2. Configure o Firebase

1. Acesse o [Firebase Console](https://console.firebase.google.com/)
2. Crie um novo projeto (ou utilize um existente)
3. Adicione um app Android com o package name: `com.example.noscroll`
4. Faça o download do arquivo `google-services.json`
5. Coloque o arquivo em `app/google-services.json`

#### Estrutura do Firestore necessária:

**Coleção `users`** (documento por `userId`):
```json
{
  "userId": "string",
  "fullName": "string",
  "email": "string",
  "birthDate": "string",
  "xp": 0,
  "level": 1
}
```

**Coleção `metas`** (documentos individuais):
```json
{
  "userId": "string",
  "descricao": "string",
  "nivel": "facil | medio | dificil",
  "concluida": false,
  "dataCriacao": "timestamp"
}
```

### 3. Abra no Android Studio
```
File → Open → Selecione a pasta do projeto
```

### 4. Sincronize o Gradle
O Android Studio solicitará sincronização automaticamente. Aguarde a conclusão.

### 5. Execute o app
- Conecte um dispositivo físico **ou** inicie um emulador
- Pressione **Run ▶** ou use `Shift + F10`

> ⚠️ **Importante:** O monitoramento de apps em primeiro plano **não funciona em emuladores** sem configuração especial. Recomenda-se testar em um dispositivo físico.

---

## 🔐 Permissões Necessárias

O app solicita as seguintes permissões — **algumas precisam ser concedidas manualmente** nas configurações do Android:

| Permissão | Tipo | Para que serve |
|-----------|------|----------------|
| `PACKAGE_USAGE_STATS` | ⚠️ Manual (Configurações) | Ler estatísticas de uso dos apps |
| `SYSTEM_ALERT_WINDOW` | ⚠️ Manual (Configurações) | Exibir tela de bloqueio sobre outros apps |
| `FOREGROUND_SERVICE` | Automática | Manter o serviço de monitoramento ativo |
| `POST_NOTIFICATIONS` | Automática (Android 13+) | Notificação persistente do serviço |
| `INTERNET` | Automática | Comunicação com Firebase |
| `WAKE_LOCK` | Automática | Manter o serviço ativo quando a tela apaga |

> 💡 **Na primeira execução**, o app guiará o usuário para conceder as permissões de `PACKAGE_USAGE_STATS` e `SYSTEM_ALERT_WINDOW` automaticamente.

---

## 📁 Estrutura do Projeto

```
NoSCRoll/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/noscroll/
│           │   │
│           │   ├── 📱 Activities (UI)
│           │   ├── MainActivity.kt          # Splash + verificação de auth
│           │   ├── LoginActivity.kt         # Tela de login (Firebase Auth)
│           │   ├── CadastroUsuario.kt       # Cadastro de novo usuário
│           │   ├── ActivityInicio.kt        # Dashboard principal (gráfico + metas + XP)
│           │   ├── AplicativosActivity.kt   # Lista de apps monitorados
│           │   ├── AppListActivity.kt       # Seleção de apps instalados
│           │   ├── CadastroAplicativo.kt    # Configuração de limite por app
│           │   ├── MetasActivity.kt         # Lista de metas por dificuldade
│           │   ├── CriarMetaActivity.kt     # Formulário de criação de meta
│           │   ├── ConfiguracaoActivity.kt  # Configurações gerais
│           │   ├── EditarPerfilActivity.kt  # Edição de perfil do usuário
│           │   ├── BlockActivity.kt         # Tela de bloqueio (popup)
│           │   ├── BaseActivity.kt          # Activity base (toolbar + back nav)
│           │   │
│           │   ├── 🔄 Adapters
│           │   ├── AppListAdapter.kt        # Adapter para lista de todos os apps
│           │   ├── ConfiguredAppAdapter.kt  # Adapter para apps monitorados
│           │   ├── MetaAdapter.kt           # Adapter de metas + sistema de XP
│           │   │
│           │   ├── 🗄️ Data Layer (Room)
│           │   ├── data/
│           │   │   ├── AppDatabase.kt           # Instância singleton do Room
│           │   │   ├── AppLimitEntity.kt         # Entidade: limites por app
│           │   │   ├── AppLimitDao.kt            # DAO: operações de limite
│           │   │   ├── DailyUsageEntity.kt       # Entidade: uso diário
│           │   │   ├── DailyUsageDao.kt          # DAO: operações de uso diário
│           │   │   └── AppUsageRepository.kt     # Repositório unificado
│           │   │
│           │   ├── ⚙️ Service (Background)
│           │   ├── service/
│           │   │   └── AppUsageMonitor.kt    # ForegroundService de monitoramento
│           │   │
│           │   ├── 🛠️ Helpers
│           │   ├── helper/
│           │   │   └── UsageStatsManagerHelper.kt # Wrapper da UsageStats API
│           │   │
│           │   └── 📦 Models
│           │       ├── Meta.kt              # Data class de Meta
│           │       ├── AppInfo.kt           # Data class de App instalado
│           │       └── AppUsageInfo.kt      # Data class de uso de app
│           │
│           ├── res/
│           │   ├── layout/                  # 16 layouts XML (Material Design)
│           │   ├── drawable/                # Ícones e backgrounds
│           │   └── values/                  # Strings, cores, estilos
│           │
│           └── AndroidManifest.xml
│
├── build.gradle.kts                         # Config root Gradle
└── app/build.gradle.kts                     # Config do módulo app
```

---

## 🗺️ Fluxo de Telas

```
┌────────────────┐
│  MainActivity  │ ──(usuário logado)──→ ActivityInicio
│  (Splash/Login)│ ──(não logado)─────→ LoginActivity
└────────────────┘

┌────────────────┐     ┌──────────────────┐
│  LoginActivity │────→│  ActivityInicio  │ (Dashboard)
│                │     │  ┌─ Gráfico 7d   │
│  CadastroUsuario│    │  ├─ Nível/XP     │
└────────────────┘     │  ├─ 3 Metas      │
                       │  ├─ [Metas] ──→ MetasActivity
                       │  ├─ [Apps]  ──→ AplicativosActivity
                       │  └─ [Config]──→ ConfiguracaoActivity
                       └──────────────────┘

MetasActivity ──────→ CriarMetaActivity
AplicativosActivity ─→ AppListActivity ──→ CadastroAplicativo
ConfiguracaoActivity ─→ EditarPerfilActivity

                    ┌─────────────────────────────────┐
                    │  AppUsageMonitor (Background)   │
                    │  Polling a cada 2s              │
                    │  → Verifica app em foreground   │
                    │  → Compara com limite do Room   │
                    │  → Dispara BlockActivity        │
                    └─────────────────────────────────┘
```

---

## 🤝 Como Contribuir

Contribuições são bem-vindas! Siga os passos abaixo:

1. **Fork** este repositório
2. Crie uma branch para sua feature:
   ```bash
   git checkout -b feature/minha-nova-funcionalidade
   ```
3. Faça suas alterações e **commit**:
   ```bash
   git commit -m "feat: adiciona nova funcionalidade X"
   ```
4. Envie para o repositório remoto:
   ```bash
   git push origin feature/minha-nova-funcionalidade
   ```
5. Abra um **Pull Request**

### Sugestões de melhorias futuras

- [ ] Widget na tela inicial com resumo de uso diário
- [ ] Modo de uso por horário (ex: bloquear após 22h)
- [ ] Relatórios semanais via notificação
- [ ] Integração com Google Digital Wellbeing API
- [ ] Suporte a múltiplos perfis de usuário
- [ ] Temas claros e escuros (Material You)
- [ ] Exportar relatório de uso em PDF

---

## 📄 Licença

Este projeto está licenciado sob a licença **MIT**. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

<div align="center">

Feito com ❤️ para ajudar pessoas a usarem menos o celular e viverem mais o presente.

**⭐ Se este projeto te ajudou, deixa uma estrela!**

</div>
