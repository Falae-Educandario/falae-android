# Integracao do Android 16 / API 36

Esta branch integra o `main` (atualizacao para Android 15) com a implementacao
Android 16 testada no Android Studio. O merge preserva os dois historicos.

## Configuracao

- `compileSdk` e `targetSdk`: 36; `minSdk`: 21.
- Versao do app: 1.0.31, `versionCode` 31 (o `main` usava 30).
- AGP 8.9.2, Gradle 8.11.1, bytecode Java/Kotlin 17 e Kotlin 1.9.25.
- Mantida a combinacao de dependencias usada na versao local testada.
- Repositorios de dependencias centralizados em `settings.gradle`, como no `main`.

API 36 atende ao requisito de atualizacoes da Google Play a partir de 31/08/2026:
https://support.google.com/googleplay/android-developer/answer/11926878

## Decisoes da revisao

- Preservados os fluxos testados de pranchas/ViewPager, navegacao, login e
  sincronizacao. A migracao do `main` para ViewPager2 e as reescritas de login
  nao eram necessarias para atingir API 36 e nao foram carregadas neste merge.
- Preservado o servico de fala e o idioma configurado no dispositivo, sem
  substituir a fala por uma instancia local que forca portugues brasileiro.
- Mantidos os ajustes de insets da versao Android 16 para barras do sistema,
  recortes de tela e teclado. O comportamento antigo de voltar e a orientacao
  horizontal foram mantidos com as opcoes de compatibilidade da API 36.
  A opcao de orientacao precisa ser reavaliada antes de mirar API 37.
- Aproveitados os ajustes pontuais do `main` para contexts/fragments,
  acesso a cores, conversao da resposta de rede e idiomas por aplicativo.
  A lista de idiomas declara somente portugues e ingles, que possuem recursos.
- Nao carregados os logs de corpo de login/resposta, o `trim()` da senha,
  nem a configuracao global permissiva de HTTP/certificados de usuario.
- A atualizacao do numero de versao agora preserva preferencias de
  acessibilidade e o ultimo usuario selecionado. O comportamento anterior
  limpava todas as preferencias em cada upgrade.
- Os JSONs das pranchas de exemplo e o esquema do banco Room nao mudaram.
- O callback de layout da prancha e removido quando a view e destruida;
  a observacao da pagina e limitada ao ciclo de vida da view e instala o adapter
  quando os dados chegam. Isso evita acesso a fragments destruidos na recriacao.
  O callback de imagem/texto tambem ignora itens que ja sairam da tela.
- Removidos do versionamento o bundle de debug e a configuracao local do
  VS Code que estavam no `main`; ambos continuam recuperaveis no historico.
- Mantidas as verificacoes de lint/release e as regras de ProGuard pontuais,
  sem carregar as supressoes abrangentes da migracao anterior.

## Verificacao

Validado em 28/08/2026: APK debug, um teste unitario, cinco testes instrumentados
no emulador API 36, otimizacao R8 de release e lint de debug/release passaram.
O lint completo terminou com zero erros e 106 avisos; os avisos legados nao
foram silenciados. O emulador foi executado em modo somente leitura e encerrado
apos os testes.

Executar com JDK 17 ou compativel com Gradle 8.11.1, SDK 36 instalado e
um dispositivo/emulador de teste conectado:

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:minifyReleaseWithR8 :app:lintDebug :app:lintVitalRelease
```

Os testes instrumentados cobrem parcelizacao de pranchas, abertura e
paginacao apos recriar a Activity, navegacao/validacao local do login e
preservacao de preferencias na atualizacao da versao 30 para 31.

O build debug continua usando `https://10.0.2.2:3000` e o release usa
`https://www.falaeapp.org`. Os testes nao autenticam no servidor de producao.
Antes da publicacao, verificar manualmente login/sincronizacao com o backend,
audio/varredura e comportamento em um tablet real e em um Android antigo.

## Publicacao

Gerar um Android App Bundle de release assinado com a chave de upload ja
cadastrada na Play Console. As credenciais e a chave nao fazem parte desta PR.
Se a Console ja tiver um `versionCode` maior ou igual a 31, incrementar o valor
antes de gerar o bundle. A assinatura de release nao e validada por estes testes.
