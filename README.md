# 🖥️ Lunar Workstation 🌜

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Gradle](https://img.shields.io/badge/Build-Gradle-blue.svg)](https://gradle.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📋 Descrição

**Workstation** é um aplicativo Android desenvolvido em Java utilizando Gradle como sistema de build. Esta aplicação foi projetada como uma ferramenta de produtividade e gerenciamento de tarefas, oferecendo uma interface intuitiva para organizar e monitorar atividades do dia a dia.

## ✨ Funcionalidades

Com base na análise da estrutura do projeto e do `AndroidManifest.xml`, o aplicativo oferece:

### 🌐 **Conectividade de Rede**
- Permissões de `INTERNET` e `ACCESS_NETWORK_STATE`
- Sincronização de dados online
- Comunicação com serviços externos

### ⚙️ **Serviço em Primeiro Plano**
- Permissão `FOREGROUND_SERVICE`
- Operações em segundo plano visíveis ao usuário
- Sincronização contínua de dados

### 📝 **Gerenciamento de Tarefas**
- Interface `TaskMenu` para listagem de tarefas
- Organização e controle de atividades
- Interface amigável e intuitiva

### 🎨 **Interface de Usuário**
- Design nativo Android
- Ícones e temas personalizados
- Experiência de usuário otimizada

## 🛠️ Instalação

### 📋 Pré-requisitos

Antes de começar, certifique-se de ter os seguintes itens instalados:

- **Android Studio** 🎯  
  Baixe em [developer.android.com/studio](https://developer.android.com/studio)

- **Java Development Kit (JDK)** ☕  
  JDK 11 ou superior (geralmente incluído no Android Studio)

### 📥 Clonar o Repositório

```bash
git clone https://github.com/Bigeus/workstation.git
cd workstation
```

### 🔧 Configurar no Android Studio

1. **Abrir o Android Studio** 🚀
2. **Importar o projeto:**
   - Na tela inicial: `"Open an existing Android Studio project"`
   - Ou via menu: `File > Open`
3. **Selecionar o diretório** `workstation`
4. **Aguardar a sincronização** do Gradle ⏳
   - O processo pode demorar alguns minutos
   - Dependências serão baixadas automaticamente

### ▶️ Executar o Aplicativo

1. **Preparar o dispositivo:**
   - Conectar um dispositivo Android via USB 📱
   - Ou iniciar um emulador no Android Studio 🖥️

2. **Executar:**
   - Selecionar o dispositivo na barra de ferramentas
   - Clicar em `Run 'app'` (▶️ botão verde)
   - Ou usar `Run > Run 'app'`

3. **Aguardar a instalação** 📲

## 🚀 Uso

Após a instalação bem-sucedida:

1. **Localizar o app** `Workstation` no dispositivo 📱
2. **Tela inicial:** `MainActivity` será exibida primeiro
3. **Menu de tarefas:** Acesse via `TaskMenu` para gerenciar suas atividades
4. **Explorar funcionalidades** disponíveis na interface

## 🤝 Contribuição

Contribuições são sempre bem-vindas! Para colaborar:

### 📝 Processo de Contribuição

1. **Fork o repositório** 🍴
   ```bash
   git fork https://github.com/Bigeus/workstation.git
   ```

2. **Criar nova branch** 🌿
   ```bash
   git checkout -b feature/sua-feature-incrivel
   ```

3. **Fazer alterações** ✏️
   - Implemente suas melhorias
   - Teste todas as funcionalidades

4. **Commit suas mudanças** 💾
   ```bash
   git commit -m "✨ Adiciona nova feature incrível"
   ```

5. **Push para seu fork** 🚀
   ```bash
   git push origin feature/sua-feature-incrivel
   ```

6. **Abrir Pull Request** 📤
   - Descreva suas alterações detalhadamente
   - Referencie issues relacionadas

### 🏷️ Convenções

- Use commits descritivos com emojis
- Siga o padrão de código existente
- Adicione testes quando necessário
- Documente novas funcionalidades

## 📄 Licença

Este projeto está licenciado sob a **Licença MIT** - veja o arquivo [LICENSE](LICENSE) para detalhes.

```
MIT License

Copyright (c) 2024 Workstation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

## 📞 Contato

- **GitHub:** [@Bigeus](https://github.com/Bigeus)
- **Issues:** [Reportar problema](https://github.com/Bigeus/workstation/issues)

---

⭐ **Gostou do projeto?** Deixe uma estrela no repositório!

📱 **Encontrou um bug?** Abra uma [issue](https://github.com/Bigeus/workstation/issues/new)

