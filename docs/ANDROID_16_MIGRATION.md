# Integracao do Android 16 / API 36

Esta branch integra o `main` (atualizacao para Android 15) com a implementacao
Android 16 testada no Android Studio. O merge preserva os dois historicos.

## Configuracao

- `compileSdk` e `targetSdk`: 36; `minSdk`: 21.
- Versao do app: 1.0.37, `versionCode` 37 (o `main` usava 30; a Play recebeu 35 e 36).
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

Na integracao inicial em 28/08/2026: APK debug, um teste unitario, cinco testes instrumentados
no emulador API 36, otimizacao R8 de release e lint de debug/release passaram.
O lint completo terminou com zero erros e 106 avisos; os avisos legados nao
foram silenciados. O emulador foi executado em modo somente leitura e encerrado
apos os testes.

Essa verificacao inicial compilou/otimizou release, mas executou os testes em
debug. A falha de reflexao do Gson encontrada depois pela Play exige testar
tambem a execucao do APK minificado; compilar release sozinho nao detecta isso.

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
Se a Console ja tiver um `versionCode` maior ou igual a 37, incrementar o valor
antes de gerar o bundle. A assinatura de release nao e validada por estes testes.

## Correcao do crash reportado no teste de 16 KB (versao 35)

O stack trace da Play aponta `ExceptionInInitializerError` em
`SpreadSheetConverter`, causado por `TypeToken must be created with a type argument`.
O mesmo crash foi reproduzido na inicializacao do APK release em Android 16
com paginas de 4096 bytes. O bundle 35 inspecionado nao contem bibliotecas `.so`.
Portanto, esse stack trace e uma falha de reflexao/R8, nao de alinhamento de ELF.

As regras de producao agora preservam `Signature`, `TypeToken` e suas subclasses,
conforme a [orientacao do Gson](https://google.github.io/gson/Troubleshooting.html#illegalstateexception-typetoken-must-be-created-with-a-type-argument).
Minificacao e otimizacao de recursos continuam ativas. Nao houve mudanca no
esquema Room, nos conversores, nos JSONs de exemplo nem nas funcionalidades.

`ReleasePersistenceTest` cobre JSON existente de pranchas (incluindo objetos
aninhados), o mapa do cache e a gravacao/leitura das duas entidades pelo Room
em um banco exclusivamente em memoria.

Depois da correcao, oito testes instrumentados passaram no release de QA em
Android 16 / x86_64 / 4 KB, sem falhas. O teste unitario existente e o lint vital
de release tambem passaram. As credenciais de assinatura de producao nao foram
usadas e o AAB assinado 35 anterior foi mantido intacto.

Tambem foi gerado um APK 36 com as regras normais de producao (sem as regras
extras do executor de testes), mudando apenas a assinatura para a chave de QA
e o diretorio de saida. No emulador descartavel, a primeira inicializacao criou
e exibiu a prancha de exemplo; a prancha abriu com seus itens; apos encerrar o
processo e reabrir o app, os dados persistidos continuaram disponiveis.
Nao houve novo crash do app nessas verificacoes. Esse APK nao contem `.so`.

Para executar os testes no APK otimizado, use um emulador descartavel, sem
contas pessoais, e configure `ANDROID_SERIAL` para o dispositivo de teste:

```text
gradlew.bat -I docs/release-tests.init.gradle :app:connectedReleaseAndroidTest
```

O init script mantem a otimizacao de release, usa a chave de debug apenas no QA
e separa as saidas em `build/release-qa`. Regras adicionais preservam as bibliotecas
compartilhadas AndroidX/Kotlin usadas pelo APK de testes; nao preservam Gson nem
os conversores. Esse APK de QA nao e identico ao de producao: tambem e necessario
verificar a inicializacao de um APK com as regras normais de release.
O script bloqueia a geracao de bundles para evitar upload acidental
de uma assinatura de teste. Para publicar, nao use esse init script: gere um
novo AAB da versao atual assinado pelo fluxo habitual do Android Studio.

Um teste em 4 KB nao substitui a revalidacao em um dispositivo de 16 KB.
O novo bundle ainda precisa passar pelos testes da Play Console.

## Correcao de retorno e restauracao das pranchas (versao 37)

O segundo relatorio da Play, referente a 1.0.36, apresenta outra excecao:
`Fragment no longer exists for key f0` em `FragmentStatePagerAdapter.restoreState`.
O teste de seguir uma pagina vinculada e voltar reproduziu a mesma excecao no
APK minificado 36 preservado, em Android 16 / x86_64 / paginas de 4 KB.

Ao colocar a pagina no back stack, o ViewPager salvava referencias aos seus
fragmentos filhos. A limpeza `adapter = null` em `onDestroyView` removia esses
filhos, invalidando as referencias usadas na volta. A tela tambem observava a
pagina global da Activity e podia reconstruir a pagina anterior com os dados
da pagina seguinte.

A correcao mantem o ViewPager e a restauracao de estado habilitada:

- Cada `PageFragment` recebe sua propria `Page` nos argumentos persistidos.
- O adapter e instalado uma vez por view, sem observar a pagina de outra tela.
- Na destruicao da view, removem-se listeners e referencias locais; o
  `FragmentManager` continua responsavel pelo ciclo de vida dos filhos.
- Ao recriar a Activity, respeitam-se os fragmentos e o historico restaurados.
  Uma navegacao ja consumida nao e repetida pelo ultimo valor do LiveData.
- `PageNavigationTest` cobre tres ciclos de ida/volta e tres recriacoes na
  pagina secundaria, verificando conteudo, historico e posicao dos dois pagers.
- Dois testes unitarios cobrem o consumo unico da navegacao e uma nova
  solicitacao para a mesma pagina.

O esquema Room, os JSONs de exemplo, a chave de assinatura, as regras do Gson,
o SDK minimo e os recursos de fala nao foram alterados por essa correcao.
Referencia: [estado dos fragments](https://developer.android.com/guide/fragments/saving-state).

Na versao 37, passaram os dez testes instrumentados de release, os tres testes
unitarios e o lint vital, em Android 16 / x86_64 / paginas de 4096 bytes.
Um segundo APK foi compilado com as regras normais de producao, sem as regras
extras do executor de testes, alterando apenas a assinatura para QA e a saida.
Nesse APK, a inicializacao com dados limpos e a abertura da prancha de exemplo
passaram, assim como tres ciclos de abrir `Eat` e voltar pelo Android, o link
`Begin` para a pagina inicial e a retomada da pagina secundaria pelos recentes
seguida de retorno. O buffer de crashes permaneceu vazio. O APK nao contem
bibliotecas `.so`; a assinatura de producao e a qualidade do audio nao foram
validadas por esse teste.

Para publicar, gerar novamente o AAB assinado 1.0.37 no Android Studio. O AAB
assinado 36 anterior nao e substituido pelos testes locais. A validacao local
em 4 KB continua sem substituir o novo teste de 16 KB da Play.
